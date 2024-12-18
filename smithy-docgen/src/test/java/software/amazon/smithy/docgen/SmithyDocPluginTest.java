/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.IoUtils;

public class SmithyDocPluginTest {

    @Test
    public void assertDocumentationFiles() {
        MockManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("sample-service.smithy"))
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .settings(Node.objectNodeBuilder()
                        .withMember("service", "smithy.example#SampleService")
                        .build())
                .build();

        SmithyBuildPlugin plugin = new SmithyDocPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        assertServicePageContents(manifest);
    }

    private void assertServicePageContents(MockManifest manifest) {
        var actual = manifest.expectFileString("/content/index.md");
        var expected = readExpectedPageContent("expected-outputs/index.md");

        assertEquals(expected, actual);
    }

    private String readExpectedPageContent(String filename) {
        URI uri;

        try {
            uri = getClass().getResource(filename).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return IoUtils.readUtf8File(Paths.get(uri))
                .replace("\r\n", "\n");
    }
}
