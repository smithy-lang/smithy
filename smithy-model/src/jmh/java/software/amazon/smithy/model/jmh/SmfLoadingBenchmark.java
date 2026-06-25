/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmh;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class SmfLoadingBenchmark {

    @State(Scope.Thread)
    public static class ModelState {

        @Param({"ec2", "s3", "dynamodb", "sts"})
        public String service;

        public Path jsonFile;
        public Path smfFile;
        public byte[] smfBytes;
        public byte[] jsonBytes;
        public SelectiveLoadRequest selectiveRequest;

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

            System.out.printf("[Setup] %s: JSON=%,d bytes, SMF=%,d bytes%n",
                    service,
                    jsonBytes.length,
                    smfBytes.length);

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
                    .build();
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
        return SmfReader.readSelective(state.smfBytes, state.selectiveRequest, false);
    }
}
