/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;

public class NullabilityReportPluginTest {

    @Test
    public void generatesReport() throws URISyntaxException {

        String testPath = "nullability/example.smithy";

        Model model = Model.assembler()
                .addImport(getClass().getResource(testPath))
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .projection("TEST", ProjectionConfig.builder().build())
                .sources(ListUtils.of(Paths.get(getClass().getResource(testPath).toURI()).getParent()))
                .build();
        new NullabilityReportPlugin().execute(context);

        String report = manifest.getFileString(NullabilityReportPlugin.NULLABILITY_REPORT_PATH).get();
        ObjectNode reportNode = Node.parse(report).expectObjectNode();

        Map<String, Boolean> isNullable = new LinkedHashMap<>();
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedTarget/v1", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedTarget/v1-via-box", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedTarget/v2-client", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedTarget/v2-client-careful", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedTarget/v2-server", true);

        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedNonPreludeTarget/v1", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedNonPreludeTarget/v1-via-box", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedNonPreludeTarget/v2-client", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedNonPreludeTarget/v2-client-careful", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedNonPreludeTarget/v2-server", true);

        isNullable.put("/smithy.example#Foo/nullableIntegerInV1BoxedTargetRequired/v1", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerInV1BoxedTargetRequired/v1-via-box", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerInV1BoxedTargetRequired/v2-client", false);
        isNullable.put("/smithy.example#Foo/nullableIntegerInV1BoxedTargetRequired/v2-client-careful", false);
        isNullable.put("/smithy.example#Foo/nullableIntegerInV1BoxedTargetRequired/v2-server", false);

        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedTarget/v1", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedTarget/v1-via-box", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedTarget/v2-client", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedTarget/v2-client-careful", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedTarget/v2-server", false);

        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedMember/v1", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedMember/v1-via-box", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedMember/v2-client", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedMember/v2-client-careful", true);
        isNullable.put("/smithy.example#Foo/nullableIntegerBoxedMember/v2-server", true);

        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedCustomTarget/v1", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedCustomTarget/v1-via-box", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedCustomTarget/v2-client", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedCustomTarget/v2-client-careful", false);
        isNullable.put("/smithy.example#Foo/nonNullableIntegerUnboxedCustomTarget/v2-server", false);

        isNullable.forEach((pointer, expectedIsNullable) -> {
            NodePointer nodePointer = NodePointer.parse(pointer);
            boolean actualIsNull = nodePointer.getValue(reportNode).expectBooleanNode().getValue();
            assertThat("Expected " + pointer + " to be " + expectedIsNullable,
                    actualIsNull,
                    equalTo(expectedIsNullable));
        });
    }
}
