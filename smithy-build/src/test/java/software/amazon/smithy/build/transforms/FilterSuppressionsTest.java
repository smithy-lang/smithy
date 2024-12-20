/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.ProjectionResult;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.SmithyBuildResult;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;

public class FilterSuppressionsTest {
    @Test
    public void cannotSetBothEventIdAllowAndDenyList() {
        SmithyBuildException thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            Model model = Model.builder().build();
            TransformContext context = TransformContext.builder()
                    .model(model)
                    .settings(Node.objectNode()
                            .withMember("eventIdAllowList", Node.fromStrings("a"))
                            .withMember("eventIdDenyList", Node.fromStrings("b")))
                    .build();
            new FilterSuppressions().transform(context);
        });

        assertThat(thrown.getMessage(), containsString("cannot set both eventIdAllowList values"));
    }

    @Test
    public void cannotSetBothNamespaceAllowAndDenyList() {
        SmithyBuildException thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            Model model = Model.builder().build();
            TransformContext context = TransformContext.builder()
                    .model(model)
                    .settings(Node.objectNode()
                            .withMember("namespaceAllowList", Node.fromStrings("a"))
                            .withMember("namespaceDenyList", Node.fromStrings("b")))
                    .build();
            new FilterSuppressions().transform(context);
        });

        assertThat(thrown.getMessage(), containsString("cannot set both namespaceAllowList values"));
    }

    @ParameterizedTest
    @CsvSource({
            "traits,removeUnused",
            "traits,eventIdAllowList",
            "traits,eventIdDenyList",
            "namespaces,filterByNamespaceAllowList",
            "namespaces,removeReasons",
            "namespaces,removeUnused",
            "namespaces,namespaceDenyList",
            "namespaces,filterWithTopLevelImports",
            "namespaces,filterWithProjectionImports",
            "namespaces,detectsValidatorRemoval",
            "namespaces,unchanged",
            "noSuppressions,removeUnused",
            "eventHierarchy,removeUnused"
    })
    public void runTransformTests(String modelFile, String testName) throws Exception {
        Model model = Model.assembler()
                .addImport(getClass().getResource("filtersuppressions/" + modelFile + ".smithy"))
                .assemble()
                .unwrap();

        SmithyBuild builder = new SmithyBuild()
                .model(model)
                .fileManifestFactory(MockManifest::new);

        SmithyBuildConfig.Builder configBuilder = SmithyBuildConfig.builder();
        configBuilder.load(Paths.get(
                getClass().getResource("filtersuppressions/" + modelFile + "." + testName + ".json").toURI()));
        configBuilder.outputDirectory("/mocked/is/not/used");
        builder.config(configBuilder.build());

        SmithyBuildResult results = builder.build();
        assertTrue(results.getProjectionResult("foo").isPresent());

        ProjectionResult projectionResult = results.getProjectionResult("foo").get();
        MockManifest manifest = (MockManifest) projectionResult.getPluginManifest("model").get();
        String modelText = manifest.getFileString("model.json").get();
        Model resultModel = Model.assembler().addUnparsedModel("/model.json", modelText).assemble().unwrap();
        Model expectedModel = Model.assembler()
                .addImport(getClass().getResource("filtersuppressions/" + modelFile + "." + testName + ".smithy"))
                .assemble()
                .unwrap();

        Node resultNode = ModelSerializer.builder().build().serialize(resultModel);
        Node expectedNode = ModelSerializer.builder().build().serialize(expectedModel);

        Node.assertEquals(resultNode, expectedNode);
    }
}
