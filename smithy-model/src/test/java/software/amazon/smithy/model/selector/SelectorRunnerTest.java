/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public final class SelectorRunnerTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void selectorTest(Path filename) {
        Model model = Model.assembler().addImport(filename).assemble().unwrap();
        List<ObjectNode> tests = findTestCases(model);

        for (ObjectNode test : tests) {
            Selector selector = Selector.parse(test.expectStringMember("selector").getValue());
            boolean skipPrelude = test.getBooleanMemberOrDefault("skipPreludeShapes", false);

            Set<ShapeId> expectedMatches = test.expectArrayMember("matches")
                    .getElementsAs(n -> n.expectStringNode("Each element of matches must be an ID").expectShapeId())
                    .stream()
                    .filter(shapeId -> {
                        String namespace = shapeId.getNamespace();
                        return !namespace.contains(Prelude.NAMESPACE)
                                || (!skipPrelude && namespace.contains(Prelude.NAMESPACE));
                    })
                    .collect(Collectors.toSet());

            Set<ShapeId> actualMatches = selector.shapes(model)
                    .map(Shape::getId)
                    .filter(shapeId -> {
                        String namespace = shapeId.getNamespace();
                        return !namespace.contains(Prelude.NAMESPACE)
                                || (!skipPrelude && namespace.contains(Prelude.NAMESPACE));
                    })
                    .collect(Collectors.toSet());

            if (!expectedMatches.equals(actualMatches)) {
                failTest(filename, test, expectedMatches, actualMatches);
            }
        }
    }

    private List<ObjectNode> findTestCases(Model model) {
        return model.getMetadataProperty("selectorTests")
                .orElseThrow(() -> new IllegalArgumentException("Missing selectorTests metadata key"))
                .expectArrayNode("selectorTests must be an array")
                .getElementsAs(ObjectNode.class);
    }

    private void failTest(Path filename, ObjectNode test, Set<ShapeId> expectedMatches, Set<ShapeId> actualMatches) {
        String selector = test.expectStringMember("selector").getValue();
        Set<ShapeId> missing = new TreeSet<>(expectedMatches);
        missing.removeAll(actualMatches);

        Set<ShapeId> extra = new TreeSet<>(actualMatches);
        extra.removeAll(expectedMatches);

        StringBuilder error = new StringBuilder("Selector ")
                .append(selector)
                .append(" test case failed.\n");

        if (!missing.isEmpty()) {
            error.append("The following shapes were not matched: ").append(missing).append(".\n");
        }

        if (!extra.isEmpty()) {
            error.append("The following shapes were matched unexpectedly: ").append(extra).append(".\n");
        }

        test.getStringMember("documentation")
                .ifPresent(docs -> error.append('(').append(docs.getValue()).append(")"));

        Assertions.fail(error.toString());
    }

    public static List<Path> source() throws Exception {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Paths.get(SelectorRunnerTest.class.getResource("cases").toURI()))) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> {
                        String filename = file.toString();
                        return filename.endsWith(".smithy") || filename.endsWith(".json");
                    })
                    .forEach(paths::add);
        } catch (IOException e) {
            throw new RuntimeException("Error loading models for selector runner", e);
        }

        return paths;
    }
}
