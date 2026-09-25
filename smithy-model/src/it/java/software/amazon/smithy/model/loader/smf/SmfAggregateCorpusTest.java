/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Builds a single aggregate SMF file containing every AWS model on the
 * classpath, reports its size against the equivalent JSON AST (raw and
 * gzipped), and times selective reads of one operation per service across the
 * whole corpus.
 *
 * <p>This is a data-gathering harness rather than an assertion test. Run with:
 * {@code ./gradlew :smithy-model:integ -PawsModelsTests --tests "*SmfAggregateCorpusTest*"}
 */
@EnabledIfSystemProperty(named = "awsModelsTests", matches = "true")
public class SmfAggregateCorpusTest {

    private static final Set<String> COLD_TRAITS = new LinkedHashSet<>(Arrays.asList(
            "smithy.api#documentation",
            "smithy.api#examples",
            "smithy.rules#endpointTests"));

    @Test
    public void aggregateCorpus() throws Exception {
        // Assemble every AWS model into one Model.
        ModelAssembler assembler = Model.assembler()
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .disableValidation();
        int modelCount = 0;
        for (URL url : ModelDiscovery.findModels(getClass().getClassLoader())) {
            if (url.toString().endsWith(".json")) {
                assembler.addImport(url);
                modelCount++;
            }
        }
        Model model = assembler.assemble().unwrap();

        int shapeCount = model.toSet().size();
        int serviceCount = model.getServiceShapes().size();

        // Serialize to JSON AST and to SMF (flat and split-cold-traits).
        byte[] json = Node.printJson(ModelSerializer.builder().build().serialize(model))
                .getBytes(StandardCharsets.UTF_8);
        byte[] smfFlat = SmfWriter.builder().build().serialize(model);
        byte[] smfSplit = SmfWriter.builder()
                .splitColdTraits(true)
                .coldTraitFilter(t -> COLD_TRAITS.contains(t.toShapeId().toString()))
                .build()
                .serialize(model);

        System.out.println("\n================ SMF AGGREGATE CORPUS ================");
        System.out.printf("models=%d  services=%d  shapes=%,d%n", modelCount, serviceCount, shapeCount);
        System.out.println("\nSize (raw / gzip), bytes:");
        report("JSON AST", json);
        report("SMF (flat)", smfFlat);
        report("SMF (split cold traits)", smfSplit);

        // Verify the aggregate reads back (full read) and time it.
        long t0 = System.nanoTime();
        Model reloaded = SmfReader.read(smfFlat, false);
        long fullReadUs = (System.nanoTime() - t0) / 1000;
        System.out.printf("%nFull read of aggregate: %,d us, shapes=%,d%n",
                fullReadUs,
                reloaded.toSet().size());

        // Selective read of one representative operation per service, over the
        // whole aggregate SMF, using the split-cold-traits file (dynamic-client
        // profile). Time each and summarize.
        List<long[]> timings = new ArrayList<>(); // [micros, closureShapes]
        List<String> slowest = new ArrayList<>();
        long warmups = 0;
        for (ServiceShape service : model.getServiceShapes()) {
            OperationShape op = firstOperation(model, service);
            if (op == null) {
                continue;
            }
            SelectiveLoadRequest req = SelectiveLoadRequest.builder()
                    .service(service.getId())
                    .addOperation(op.getId())
                    .verifyCrc(false)
                    .build();
            // Warm the JIT a little on the first few.
            if (warmups++ < 20) {
                for (int i = 0; i < 50; i++) {
                    SmfReader.readSelective(smfSplit, req);
                }
            }
            long best = Long.MAX_VALUE;
            int closure = 0;
            for (int i = 0; i < 5; i++) {
                long s = System.nanoTime();
                SmfSelectiveLoadResult r = SmfReader.readSelectiveWithProfile(smfSplit, req);
                long us = (System.nanoTime() - s) / 1000;
                if (us < best) {
                    best = us;
                    closure = r.getProfile().getClosureShapeCount();
                }
            }
            timings.add(new long[] {best, closure});
            slowest.add(String.format("%8d us  closure=%-5d  %s -> %s",
                    best,
                    closure,
                    service.getId(),
                    op.getId().getName()));
        }

        timings.sort((a, b) -> Long.compare(a[0], b[0]));
        int n = timings.size();
        long min = timings.get(0)[0];
        long median = timings.get(n / 2)[0];
        long p90 = timings.get((int) (n * 0.90))[0];
        long p99 = timings.get((int) (n * 0.99))[0];
        long max = timings.get(n - 1)[0];
        long sum = 0;
        for (long[] t : timings) {
            sum += t[0];
        }

        System.out.printf("%nSelective read (service + 1 operation) over aggregate SMF, %d services:%n", n);
        System.out.printf("  min=%d us  median=%d us  p90=%d us  p99=%d us  max=%d us  mean=%d us%n",
                min,
                median,
                p90,
                p99,
                max,
                sum / n);

        slowest.sort((a, b) -> {
            long av = Long.parseLong(a.trim().split(" ")[0]);
            long bv = Long.parseLong(b.trim().split(" ")[0]);
            return Long.compare(bv, av);
        });
        System.out.println("\n  Slowest 10 selective reads:");
        for (int i = 0; i < Math.min(10, slowest.size()); i++) {
            System.out.println("   " + slowest.get(i));
        }
        System.out.println("======================================================\n");
    }

    private static OperationShape firstOperation(Model model, ServiceShape service) {
        ShapeId opId = service.getAllOperations().stream().findFirst().orElse(null);
        return opId == null ? null : model.expectShape(opId, OperationShape.class);
    }

    private static void report(String label, byte[] data) throws Exception {
        System.out.printf("  %-26s %,12d  %,12d%n", label, data.length, gzip(data));
    }

    private static long gzip(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bos)) {
            g.write(data);
        }
        return bos.size();
    }
}
