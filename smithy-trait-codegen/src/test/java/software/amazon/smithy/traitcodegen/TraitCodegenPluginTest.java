/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;

public class TraitCodegenPluginTest {

    @Test
    public void generatesExpectedTraitFiles() {
        MockManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .settings(ObjectNode.builder()
                        .withMember("package", "com.example.traits")
                        .withMember("header", ArrayNode.fromStrings("Header line One"))
                        .build()
                )
                .model(model)
                .build();

        SmithyBuildPlugin plugin = new TraitCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
    }
}
