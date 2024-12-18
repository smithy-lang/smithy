/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import java.nio.file.Paths;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Simple wrapper class used to execute the Trait codegen plugin for integration tests.
 */
public final class PluginExecutor {
    private PluginExecutor() {
        // Utility class does not have constructor
    }

    public static void main(String[] args) {
        TraitCodegenPlugin plugin = new TraitCodegenPlugin();
        Model model = Model.assembler(PluginExecutor.class.getClassLoader())
                .discoverModels(PluginExecutor.class.getClassLoader())
                .assemble()
                .unwrap();
        PluginContext context = PluginContext.builder()
                .fileManifest(FileManifest.create(Paths.get("build/integ")))
                .settings(ObjectNode.builder()
                        .withMember("package", "com.example.traits")
                        .withMember("namespace", "test.smithy.traitcodegen")
                        .withMember("header", ArrayNode.fromStrings("Header line One"))
                        .build())
                .model(model)
                .build();
        plugin.execute(context);
    }
}
