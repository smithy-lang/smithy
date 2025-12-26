/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.docgen.SmithyDocPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public abstract class AbstractDocGenFileTest {

    protected Model model;
    protected final SmithyDocPlugin plugin = new SmithyDocPlugin();

    @BeforeEach
    public void setup() {
        model = Model.assembler()
                .addImport(testFile())
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
    }

    public void execute(FileManifest fileManifest) {
        execute(fileManifest, new MockManifest(), settings());
    }

    public void execute(FileManifest fileManifest, FileManifest sharedFileManifest, ObjectNode settings) {
        PluginContext context = PluginContext.builder()
                .fileManifest(fileManifest)
                .sharedFileManifest(sharedFileManifest)
                .model(model)
                .settings(settings)
                .build();
        plugin.execute(context);
        assertFalse(fileManifest.getFiles().isEmpty());
    }

    protected abstract URL testFile();

    protected ObjectNode settings() {
        return Node.objectNodeBuilder()
                .withMember("service", "smithy.example#TestService")
                .withMember("format", "sphinx-markdown")
                .withMember("integrations",
                        Node.objectNodeBuilder()
                                .withMember("sphinx",
                                        Node.objectNodeBuilder()
                                                .withMember("autoBuild", false)
                                                .build())
                                .build())
                .build();
    }
}
