/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.docgen.utils.AbstractDocGenFileTest;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.IoUtils;

public class OperationGeneratorTest extends AbstractDocGenFileTest {
    private static final URL TEST_FILE =
            Objects.requireNonNull(OperationGeneratorTest.class.getResource("operation-generator.smithy"));
    private static final URL SNIPPETS_FILE =
            Objects.requireNonNull(OperationGeneratorTest.class.getResource("snippets.json"));

    @Override
    protected URL testFile() {
        return TEST_FILE;
    }

    @Override
    protected ObjectNode settings() {
        return super.settings().toBuilder()
                .withMember("snippetConfigs", Node.fromStrings(SNIPPETS_FILE.getFile()))
                .build();
    }

    @Test
    public void testOperationWithoutSnippetsShowsNoExamples() {
        MockManifest manifest = new MockManifest();
        execute(manifest);
        var operationDocs = manifest.expectFileString("/content/operations/NoSnippets.md");
        assertThat(operationDocs, not(containsString("Examples")));
    }

    @Test
    public void testGeneratesSnippetsFromExplicitConfig() {
        MockManifest manifest = new MockManifest();
        execute(manifest);
        var operationDocs = manifest.expectFileString("/content/operations/BasicOperation.md");
        assertThat(operationDocs, containsString("""
                (basicoperation-examples)=
                ## Examples

                (basicoperation-basic-example)=
                ### Basic Example

                :::{tab} Text
                :new-set:

                ```txt
                Example pulled from explicit config.
                ```
                :::
                :::{tab} Text 2
                ```txt
                Example pulled from explicit config.
                ```
                :::

                (basicoperation-error-example)=
                ### Error Example

                :::{tab} Text
                :new-set:

                ```txt
                Example pulled from explicit config.
                ```
                :::
                :::{tab} Text 2
                ```txt
                Example pulled from explicit config.
                ```
                :::"""));
    }

    @Test
    public void testGeneratesSnippetsFromDiscoveredConfig(@TempDir Path tempDir) {
        MockManifest manifest = new MockManifest();
        FileManifest sharedManifest = FileManifest.create(tempDir);
        ObjectNode settings = settings().toBuilder()
                .withoutMember("snippetConfigs")
                .build();

        sharedManifest.writeFile("snippets/snippets.json", IoUtils.readUtf8File(SNIPPETS_FILE.getFile()));
        execute(manifest, sharedManifest, settings);
        var operationDocs = manifest.expectFileString("/content/operations/BasicOperation.md");
        assertThat(operationDocs, containsString("""
                (basicoperation-examples)=
                ## Examples

                (basicoperation-basic-example)=
                ### Basic Example

                :::{tab} Text
                :new-set:

                ```txt
                Example pulled from explicit config.
                ```
                :::
                :::{tab} Text 2
                ```txt
                Example pulled from explicit config.
                ```
                :::

                (basicoperation-error-example)=
                ### Error Example

                :::{tab} Text
                :new-set:

                ```txt
                Example pulled from explicit config.
                ```
                :::
                :::{tab} Text 2
                ```txt
                Example pulled from explicit config.
                ```
                :::"""));
    }
}
