/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.IoUtils;

public class SmithyCfnJsonGoldenFileTest {

    @Test
    public void producesExpectedOutput() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integration-model.smithy"))
                .assemble()
                .unwrap();

        MockManifest manifest = new MockManifest();
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#MyService")
                .build();

        PluginContext context = PluginContext.builder()
                .model(model)
                .fileManifest(manifest)
                .settings(settings)
                .build();

        new SmithyCfnJson().execute(context);

        String output = manifest.getFileString("MyService.smithy.json").get();
        ObjectNode actual = Node.parse(output).expectObjectNode();
        ObjectNode expected = Node.parse(
                IoUtils.readUtf8Resource(getClass(), "integration-model.expected.json"))
                .expectObjectNode();

        Node.assertEquals(actual, expected);
    }
}
