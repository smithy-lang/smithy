/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Integration test that round-trips each AWS model on the classpath through SMF
 * serialization and verifies equivalence with the original.
 *
 * <p>Run with: {@code ./gradlew :smithy-model:integ -PawsModelsTests --tests "*SmfAwsModelRoundTripTest*"}
 */
@EnabledIfSystemProperty(named = "awsModelsTests", matches = "true")
@Execution(ExecutionMode.CONCURRENT)
public class SmfAwsModelRoundTripTest {

    static Stream<Named<URL>> awsModels() {
        return ModelDiscovery.findModels(SmfAwsModelRoundTripTest.class.getClassLoader())
                .stream()
                .filter(url -> url.toString().endsWith(".json"))
                .map(url -> Named.of(artifactName(url), url))
                .sorted(Comparator.comparing(Named::getName));
    }

    private static String artifactName(URL modelUrl) {
        String urlStr = modelUrl.toString();
        int bangIdx = urlStr.indexOf("!/");
        if (bangIdx < 0) {
            return urlStr;
        }
        String jarPath = urlStr.substring(0, bangIdx);
        String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
        return jarName.replaceFirst("-\\d[\\d.]*\\.jar$", "");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("awsModels")
    void roundTripModel(URL modelUrl) {
        // Load model from JSON
        Model original = Model.assembler()
                .addImport(modelUrl)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .disableValidation()
                .assemble()
                .unwrap();

        // Write to SMF and read back
        byte[] smfBytes = SmfWriter.builder().build().serialize(original);
        Model loaded = SmfReader.read(smfBytes);

        // Compare non-prelude shapes
        Set<ShapeId> originalIds = original.toSet()
                .stream()
                .map(Shape::getId)
                .filter(id -> !Prelude.isPreludeShape(id))
                .collect(Collectors.toSet());
        Set<ShapeId> loadedIds = loaded.toSet()
                .stream()
                .map(Shape::getId)
                .filter(id -> !Prelude.isPreludeShape(id))
                .collect(Collectors.toSet());

        assertEquals(originalIds, loadedIds, "Shape ID sets differ");

        // Compare each shape in detail
        List<String> failures = new ArrayList<>();
        for (ShapeId id : originalIds) {
            Shape origShape = original.expectShape(id);
            Shape loadedShape = loaded.expectShape(id);

            if (origShape.getType() != loadedShape.getType()) {
                failures.add(id + ": type mismatch " + origShape.getType()
                        + " vs " + loadedShape.getType());
                continue;
            }

            // Compare traits via Node equality
            Map<String, Node> origTraits = new TreeMap<>();
            for (Map.Entry<ShapeId, Trait> e : origShape.getAllTraits().entrySet()) {
                origTraits.put(e.getKey().toString(), e.getValue().toNode());
            }
            Map<String, Node> loadedTraits = new TreeMap<>();
            for (Map.Entry<ShapeId, Trait> e : loadedShape.getAllTraits().entrySet()) {
                loadedTraits.put(e.getKey().toString(), e.getValue().toNode());
            }
            if (!origTraits.equals(loadedTraits)) {
                failures.add(id + ": trait mismatch");
            }

            // Compare member order
            if (!origShape.getMemberNames().equals(loadedShape.getMemberNames())) {
                failures.add(id + ": member order mismatch");
            }
        }

        if (!original.getMetadata().equals(loaded.getMetadata())) {
            failures.add("metadata mismatch");
        }

        if (!failures.isEmpty()) {
            fail("Round-trip failures (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures.subList(0, Math.min(failures.size(), 20)))
                    + (failures.size() > 20 ? "\n  ... and " + (failures.size() - 20) + " more" : ""));
        }
    }
}
