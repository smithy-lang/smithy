/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.docgen.utils.AbstractDocGenFileTest;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.IoUtils;

public class SmithyDocPluginTest extends AbstractDocGenFileTest {
    private static final URL TEST_FILE =
            Objects.requireNonNull(SmithyDocPluginTest.class.getResource("sample-service.smithy"));

    @Override
    protected URL testFile() {
        return TEST_FILE;
    }

    @Override
    protected ObjectNode settings() {
        return super.settings().toBuilder()
                .withMember("service", "smithy.example#SampleService")
                .build();
    }

    @Test
    public void assertDocumentationFiles() {
        var fileManifest = new MockManifest();
        execute(fileManifest);
        var actual = fileManifest.expectFileString("/content/index.md");
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
