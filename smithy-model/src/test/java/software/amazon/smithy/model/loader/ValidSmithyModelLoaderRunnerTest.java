/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;

/**
 * Loads all of the ".smithy" files in idl/valid.
 *
 * Each .smithy file is loaded and then turned into a JSON model. This
 * is then compared against the contents of the corresponding .json file
 * to make sure that the model was loaded correctly. This also ensures
 * that the json and smithy files are isomorphic.
 */
public class ValidSmithyModelLoaderRunnerTest {

    private static Model shared;

    @BeforeAll
    public static void before() {
        shared = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class.getResource("valid/__shared.json"))
                .assemble()
                .unwrap();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void parserRunnerTest(String file) {
        Path smithyFilename = Paths.get(file.replace(".json", ".smithy"));
        Model expected = Model.assembler()
                .addImport(file)
                // Always include the __shared.json file. This is used for
                // cross-model loading testing.
                .addModel(shared)
                .assemble()
                .unwrap();

        Model result;
        try {
            result = Model.assembler()
                    .addImport(smithyFilename)
                    .addModel(shared)
                    .assemble()
                    .unwrap();
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Error parsing: %s\n\n%s", smithyFilename, e.getMessage()),
                    e);
        }

        validateMatch(result, expected, file);
    }

    private void validateMatch(Model result, Model expected, String file) {
        if (!result.equals(expected)) {
            ModelSerializer serializer = ModelSerializer.builder().build();
            throw new IllegalStateException(String.format(
                    "Result did not match the expected model for %s.\nResult:\n\n%s\n\nExpected:\n\n%s\r\nDiff: %s",
                    file,
                    formatModel(result),
                    formatModel(expected),
                    Node.diff(serializer.serialize(result), serializer.serialize(expected))));
        }
    }

    private static String formatModel(Model model) {
        ModelSerializer serializer = ModelSerializer.builder()
                .shapeFilter(shape -> !shape.getSourceLocation().getFilename().contains("__shared.json"))
                .build();
        return Node.prettyPrintJson(serializer.serialize(model));
    }

    public static Collection<String> data() throws Exception {
        try {
            Stream<Path> paths = Files.walk(Paths.get(
                    ValidSmithyModelLoaderRunnerTest.class.getResource("valid").toURI()));
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".json"))
                    .filter(file -> !file.toString().contains("__shared.json"))
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void canAddTraitsToForwardReferenceMembersWithUseStatements() {
        Model result = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class.getResource("forwardrefs/use/use.smithy"))
                .addModel(shared)
                .assemble()
                .unwrap();
        Model expected = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class.getResource("forwardrefs/use/result.json"))
                .addModel(shared)
                .assemble()
                .unwrap();

        validateMatch(result, expected, "forwardrefs/use-shapes.smithy");
    }

    @Test
    public void canAddTraitsToForwardReferenceMembersWithNoUseStatements() {
        Model result = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class.getResource("forwardrefs/use/no-use.smithy"))
                .addModel(shared)
                .assemble()
                .unwrap();
        Model expected = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class
                        .getResource("forwardrefs/use/result.json"))
                .addModel(shared)
                .assemble()
                .unwrap();

        validateMatch(result, expected, "forwardrefs/user/no-use.smithy");
    }

    @Test
    public void canHandleForwardRefsInResourceProperties() {
        Model modelA = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class
                        .getResource("forwardrefs/resource/operation.smithy"))
                .assemble()
                .unwrap();
        Model result = Model.assembler()
                .addModel(modelA)
                .addImport(ValidSmithyModelLoaderRunnerTest.class
                        .getResource("forwardrefs/resource/resource.smithy"))
                .assemble()
                .unwrap();
        Model expected = Model.assembler()
                .addImport(ValidSmithyModelLoaderRunnerTest.class
                        .getResource("forwardrefs/resource/result.json"))
                .assemble()
                .unwrap();

        validateMatch(result, expected, "forwardrefs/resource/operation.smithy");
    }
}
