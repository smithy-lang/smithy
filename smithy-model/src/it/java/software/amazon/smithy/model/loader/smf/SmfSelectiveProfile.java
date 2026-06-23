/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Profiles where time is spent in selective loading.
 *
 * <p>Run with: {@code ./gradlew :smithy-model:integ -PawsModelsTests --tests "*SmfSelectiveProfile*"}
 */
@EnabledIfSystemProperty(named = "awsModelsTests", matches = "true")
public class SmfSelectiveProfile {

    @Test
    public void profilePhases() {
        // Suppress logging
        java.util.logging.Logger.getLogger("software.amazon.smithy").setLevel(java.util.logging.Level.OFF);

        Model fullModel = Model.assembler()
                .discoverModels(SmfSelectiveProfile.class.getClassLoader())
                .disableValidation()
                .assemble()
                .unwrap();

        byte[] smfBytes = SmfWriter.write(fullModel);
        System.out.printf("SMF size: %,d bytes, shapes: %,d%n%n", smfBytes.length, fullModel.toSet().size());

        SelectiveLoadRequest request = SelectiveLoadRequest.builder()
                .service(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"))
                .addOperation(ShapeId.from("com.amazonaws.dynamodb#PutItem"))
                .build();

        // Warmup
        for (int i = 0; i < 5; i++) {
            SmfReader.readSelective(smfBytes, request, false);
        }

        // Profile 10 iterations
        int iterations = 10;
        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            SmfReader.readSelective(smfBytes, request, false);
            times[i] = System.nanoTime() - start;
        }

        long sum = 0;
        for (long t : times) sum += t;
        double avgMs = (sum / iterations) / 1_000_000.0;
        System.out.printf("Average selective load: %.2f ms%n%n", avgMs);

        // Now profile individual phases using instrumented reader
        profileInstrumented(smfBytes, request);
    }

    private void profileInstrumented(byte[] data, SelectiveLoadRequest request) {
        // Phase breakdown by measuring sub-sections
        // We can't easily instrument SmfReader without modifying it,
        // so let's measure the major allocations/operations externally

        long t0, t1, t2, t3, t4, t5;

        // Phase 1: Just read header + symbol table (lazy)
        t0 = System.nanoTime();
        // Simulate by calling selective and timing sub-operations
        // Instead, measure what we can: the byte[] is already in memory,
        // so file I/O is 0. Let's measure findSymRef cost.

        // Find symrefs
        t0 = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            // Simulating findSymRef scanning all symbols
            String target = "com.amazonaws.dynamodb#DynamoDB_20120810";
            for (int j = 0; j < data.length / 100; j++) {} // placeholder
        }
        t1 = System.nanoTime();

        // Actually, let's just time the whole thing broken into: 
        // "everything before shapes" vs "shape parsing"
        // by loading with 0 operations (just overhead) vs with 1 operation

        SelectiveLoadRequest emptyRequest = SelectiveLoadRequest.builder()
                .service(ShapeId.from("com.amazonaws.dynamodb#DynamoDB_20120810"))
                .build();

        // Warmup
        for (int i = 0; i < 5; i++) {
            SmfReader.readSelective(data, emptyRequest, false);
        }

        long overheadTotal = 0;
        int n = 10;
        for (int i = 0; i < n; i++) {
            long start = System.nanoTime();
            SmfReader.readSelective(data, emptyRequest, false);
            overheadTotal += System.nanoTime() - start;
        }
        double overheadMs = (overheadTotal / n) / 1_000_000.0;

        // With operation
        long fullTotal = 0;
        for (int i = 0; i < n; i++) {
            long start = System.nanoTime();
            SmfReader.readSelective(data, request, false);
            fullTotal += System.nanoTime() - start;
        }
        double fullMs = (fullTotal / n) / 1_000_000.0;

        System.out.println("Phase breakdown:");
        System.out.printf("  Overhead (header + symbols + index scan + closure, no shapes): %.2f ms%n", overheadMs);
        System.out.printf("  Full (with shape parsing):                                    %.2f ms%n", fullMs);
        System.out.printf("  Shape parsing (difference):                                   %.2f ms%n", fullMs - overheadMs);
    }
}
