/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmh;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.smf.SelectiveLoadRequest;
import software.amazon.smithy.model.loader.smf.SmfReader;
import software.amazon.smithy.model.loader.smf.SmfWriter;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;

/**
 * Benchmarks comparing SMF binary loading vs JSON AST loading.
 *
 * <p>Both paths go through {@code Model.assembler().addImport(file)} to ensure
 * an apples-to-apples comparison of the full loading pipeline.
 *
 * <p>Run with: {@code ./gradlew :smithy-model:jmh -Pjmh.includes="SmfLoadingBenchmark"}
 */
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
public class SmfLoadingBenchmark {

    // Cold traits split out into the secondary table for dynamic-client selective
    // loads. Mirrors the set used by SmfAwsModelSplitDiffTest: documentation,
    // examples, and endpoint test cases are never needed to execute a call.
    private static final Set<String> COLD_TRAITS = new HashSet<>(Arrays.asList(
            "smithy.api#documentation",
            "smithy.api#examples",
            "smithy.rules#endpointTests"));

    // Execution-hot allowlist: the curated set of traits a smithy-java dynamic
    // client actually needs to build/parse a request (derived from the
    // TraitKey.get(...) usage in smithy-java, minus introspection-only traits
    // like documentation/deprecated). Everything NOT in this set is treated as
    // cold. This is the "explicit hot allowlist" alternative to the cold
    // denylist above and is robust against newly-added cold traits.
    private static final Set<String> HOT_EXECUTION_TRAITS = new HashSet<>(Arrays.asList(
            "smithy.api#default",
            "smithy.api#enum",
            "smithy.api#eventHeader",
            "smithy.api#eventPayload",
            "smithy.api#hostLabel",
            "smithy.api#httpChecksumRequired",
            "smithy.api#httpError",
            "smithy.api#httpHeader",
            "smithy.api#httpLabel",
            "smithy.api#httpPayload",
            "smithy.api#httpQuery",
            "smithy.api#http",
            "smithy.api#jsonName",
            "smithy.api#length",
            "smithy.api#mediaType",
            "smithy.api#pattern",
            "smithy.api#range",
            "smithy.api#required",
            "smithy.api#sensitive",
            "smithy.api#sparse",
            "smithy.api#streaming",
            "smithy.api#uniqueItems",
            "smithy.api#unitType",
            "smithy.api#cors",
            "smithy.api#endpoint",
            "smithy.api#idempotencyToken",
            "smithy.api#paginated",
            "smithy.api#xmlAttribute",
            "smithy.api#xmlFlattened",
            "smithy.api#xmlNamespace",
            "smithy.api#xmlName",
            "smithy.api#enumValue",
            "smithy.api#clientOptional",
            "aws.protocols#awsQueryError",
            "aws.protocols#ec2QueryName",
            "aws.protocols#awsJson1_0",
            "aws.protocols#httpChecksum",
            "aws.protocols#requestCompression",
            "aws.api#service",
            "smithy.rules#contextParam",
            "smithy.rules#endpointBdd"));

    @State(Scope.Thread)
    public static class ModelState {

        @Param({"ec2", "s3", "dynamodb", "sts"})
        public String service;

        public Path jsonFile;
        public Path smfFile;
        public byte[] smfBytes;
        public byte[] jsonBytes;
        // Same model written with hot/cold trait splitting so selective loads
        // can skip cold-trait decoding entirely.
        public byte[] splitSmfBytes;
        // Split using an explicit execution-hot ALLOWLIST: every trait not
        // needed to execute a call is cold. The intended dynamic-client config.
        public byte[] splitAllowlistSmfBytes;
        public SelectiveLoadRequest selectiveRequest;
        public SelectiveLoadRequest selectiveRequestCrc;

        @Setup
        public void prepare() throws Exception {
            // Load the JSON model from the classpath resource
            String resourcePath = getResourcePath(service);
            String json;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IllegalStateException("Model not found on classpath: " + resourcePath);
                }
                json = IoUtils.toUtf8String(is);
            }

            // Parse the model
            Model model = Model.assembler()
                    .addUnparsedModel(resourcePath, json)
                    .disableValidation()
                    .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                    .assemble()
                    .unwrap();

            // Write JSON file (re-serialized for canonical form)
            String canonicalJson = Node.printJson(
                    ModelSerializer.builder().build().serialize(model));
            jsonFile = Files.createTempFile("bench-" + service, ".json");
            jsonBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);
            Files.write(jsonFile, jsonBytes);

            // Write SMF file
            smfBytes = SmfWriter.builder().build().serialize(model);
            smfFile = Files.createTempFile("bench-" + service, ".smf");
            Files.write(smfFile, smfBytes);

            // Write a hot/cold split SMF: cold traits (docs/examples/endpointTests)
            // go into the secondary table so selective loads skip them.
            splitSmfBytes = SmfWriter.builder()
                    .splitColdTraits(true)
                    .coldTraitFilter(trait -> COLD_TRAITS.contains(trait.toShapeId().toString()))
                    .build()
                    .serialize(model);

            splitAllowlistSmfBytes = SmfWriter.builder()
                    .splitColdTraits(true)
                    // Allowlist: everything NOT in the execution-hot set is cold.
                    .coldTraitFilter(trait -> !HOT_EXECUTION_TRAITS.contains(trait.toShapeId().toString()))
                    .build()
                    .serialize(model);

            System.out.printf(
                    "[Setup] %s: JSON=%,d bytes, SMF=%,d bytes, splitSMF(deny3)=%,d bytes, splitSMF(allowlist)=%,d bytes%n",
                    service,
                    jsonBytes.length,
                    smfBytes.length,
                    splitSmfBytes.length,
                    splitAllowlistSmfBytes.length);

            // Set up selective load request: service + one representative operation
            ShapeId serviceId = model.getServiceShapes()
                    .stream()
                    .findFirst()
                    .get()
                    .getId();
            ShapeId operationId = getRepresentativeOperation(service);
            selectiveRequest = SelectiveLoadRequest.builder()
                    .service(serviceId)
                    .addOperation(operationId)
                    .verifyCrc(false)
                    .build();
            selectiveRequestCrc = SelectiveLoadRequest.builder()
                    .service(serviceId)
                    .addOperation(operationId)
                    .verifyCrc(true)
                    .build();

            // Export fixtures to a stable directory so out-of-process readers
            // (e.g. the Python spike) can consume the exact same bytes.
            // Controlled by -Psmf.exportDir=<dir> passed through as a system property.
            String exportDir = System.getProperty("smf.exportDir");
            if (exportDir != null) {
                Path base = Paths.get(exportDir);
                Files.createDirectories(base);
                Files.write(base.resolve(service + ".smf"), smfBytes);
                Files.write(base.resolve(service + ".split.smf"), splitSmfBytes);
                Files.write(base.resolve(service + ".json"), jsonBytes);
                System.out.printf("[Export] wrote %s.smf, %s.split.smf and %s.json to %s%n",
                        service,
                        service,
                        service,
                        base);
            }
        }

        @TearDown
        public void cleanup() throws Exception {
            Files.deleteIfExists(jsonFile);
            Files.deleteIfExists(smfFile);
        }

        private static String getResourcePath(String service) {
            switch (service) {
                case "ec2":
                    return "META-INF/smithy/2016-11-15/ec2-2016-11-15.json";
                case "s3":
                    return "META-INF/smithy/2006-03-01/s3-2006-03-01.json";
                case "dynamodb":
                    return "META-INF/smithy/2012-08-10/dynamodb-2012-08-10.json";
                case "sts":
                    return "META-INF/smithy/2011-06-15/sts-2011-06-15.json";
                default:
                    throw new IllegalArgumentException("Unknown service: " + service);
            }
        }

        private static ShapeId getRepresentativeOperation(String service) {
            switch (service) {
                case "ec2":
                    return ShapeId.from("com.amazonaws.ec2#DescribeInstances");
                case "s3":
                    return ShapeId.from("com.amazonaws.s3#GetObject");
                case "dynamodb":
                    return ShapeId.from("com.amazonaws.dynamodb#GetItem");
                case "sts":
                    return ShapeId.from("com.amazonaws.sts#AssumeRole");
                default:
                    throw new IllegalArgumentException("Unknown service: " + service);
            }
        }
    }

    @Benchmark
    public Model loadFromSmf(ModelState state) {
        return Model.assembler()
                .addImport(state.smfFile)
                .disableValidation()
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .assemble()
                .unwrap();
    }

    @Benchmark
    public Model loadFromJson(ModelState state) {
        return Model.assembler()
                .addImport(state.jsonFile)
                .disableValidation()
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .assemble()
                .unwrap();
    }

    /**
     * Direct SmfReader.read() from byte[] — bypasses ModelAssembler entirely.
     * This is the path a dynamic client would use.
     */
    @Benchmark
    public Model loadSmfDirect(ModelState state) {
        return SmfReader.read(state.smfBytes, false);
    }

    /**
     * Selective loading: service + one operation closure.
     * This is the dynamic client cold-start path.
     */
    @Benchmark
    public Model loadSmfSelective(ModelState state) {
        return SmfReader.readSelective(state.smfBytes, state.selectiveRequest);
    }

    /**
     * Selective loading over a hot/cold split model: cold traits (docs,
     * examples, endpoint tests) live in a separate table and are skipped
     * entirely by the selective reader. This is the intended dynamic-client
     * cold-start configuration. Compared against {@link #loadSmfSelective}
     * (same request over a flat model) to measure the cold-trait skip win.
     */
    @Benchmark
    public Model loadSmfSelectiveSplit(ModelState state) {
        return SmfReader.readSelective(state.splitSmfBytes, state.selectiveRequest);
    }

    /**
     * Selective loading over a model split with an explicit execution-hot
     * allowlist (everything not needed to run a call is cold). Compared against
     * {@link #loadSmfSelectiveSplit} (cold denylist of 3 traits) to measure the
     * benefit of pushing more traits cold. Symref key/value encoding is always
     * applied to hot values.
     */
    @Benchmark
    public Model loadSmfSelectiveSplitAllowlist(ModelState state) {
        return SmfReader.readSelective(state.splitAllowlistSmfBytes, state.selectiveRequest);
    }

    /**
     * Direct SmfReader.read() WITH full-file CRC-32C verification.
     * Compared against {@link #loadSmfDirect} to isolate the cost of the
     * default-on integrity check on the primary production path.
     */
    @Benchmark
    public Model loadSmfDirectCrc(ModelState state) {
        return SmfReader.read(state.smfBytes, true);
    }

    /**
     * Selective loading WITH full-file CRC-32C verification.
     * Compared against {@link #loadSmfSelective} to measure how much the
     * O(file-size) CRC pass erodes the selective-load win (hypothesis H1).
     */
    @Benchmark
    public Model loadSmfSelectiveCrc(ModelState state) {
        return SmfReader.readSelective(state.smfBytes, state.selectiveRequestCrc);
    }
}
