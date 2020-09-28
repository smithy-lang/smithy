/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnException;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

public class Smithy2CfnTest {
    @Test
    public void pluginConvertsModel() {
        Model model = Model.assembler()
                .addImport(Smithy2CfnTest.class.getResource("test-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .settings(Node.objectNode()
                        .withMember("organizationName", "Smithy")
                        .withMember("service", "smithy.example#TestService"))
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .build();
        new Smithy2Cfn().execute(context);

        assertEquals(manifest.getFiles().size(), 3);
        assertTrue(manifest.hasFile("/smithy-testservice-fooresource.json"));
        assertTrue(manifest.hasFile("/smithy-testservice-bar.json"));
        assertTrue(manifest.hasFile("/smithy-testservice-basil.json"));
    }

    @Test
    public void throwsWhenServiceNotConfigured() {
        Model model = Model.assembler()
                .addImport(Smithy2CfnTest.class.getResource("test-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .settings(Node.objectNode()
                        .withMember("organizationName", "Smithy"))
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .build();

        assertThrows(CfnException.class, () -> {
            new Smithy2Cfn().execute(context);
        });
    }

    @Test
    public void throwsWhenOrganizationNameNotConfigured() {
        Model model = Model.assembler()
                .addImport(Smithy2CfnTest.class.getResource("test-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .settings(Node.objectNode()
                        .withMember("service", "smithy.example#TestService"))
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .build();

        assertThrows(CfnException.class, () -> {
            new Smithy2Cfn().execute(context);
        });
    }
}
