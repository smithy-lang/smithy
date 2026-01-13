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
import java.nio.file.Paths;
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
        String path;
        try {
            path = Paths.get(SNIPPETS_FILE.toURI()).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return super.settings().toBuilder()
                .withMember("snippetConfigs", Node.fromStrings(path))
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

        sharedManifest.writeFile("snippets/snippets.json", IoUtils.readUtf8Url(SNIPPETS_FILE));
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

    @Test
    public void testPaginatedOperation(@TempDir Path tempDir) {
        MockManifest manifest = new MockManifest();
        FileManifest sharedManifest = FileManifest.create(tempDir);
        ObjectNode settings = settings().toBuilder()
                .withoutMember("snippetConfigs")
                .build();
        sharedManifest.writeFile("snippets/snippets.json", IoUtils.readUtf8Url(SNIPPETS_FILE));
        execute(manifest, sharedManifest, settings);
        var operationDocs = manifest.expectFileString("/content/operations/PaginatedOperation.md");
        assertThat(operationDocs,
                containsString(
                        """
                                (paginatedoperation)=
                                # PaginatedOperation

                                Placeholder documentation for `smithy.example#PaginatedOperation`

                                :::{important}
                                This operation returns partial results in pages, whose maximum size may be
                                configured with [pageSize](./PaginatedOperation.md#paginatedoperation-pagesize). Each request may return an [output token](./PaginatedOperation.md#paginatedoperation-nexttoken) that may be used as an [input token](./PaginatedOperation.md#paginatedoperation-nexttoken) in subsequent requests to fetch the next page of results. If the operation does not return an [output token](./PaginatedOperation.md#paginatedoperation-nexttoken), that means that there are no more results. If the operation returns a repeated [output token](./PaginatedOperation.md#paginatedoperation-nexttoken), there MAY be more results later.
                                :::

                                (paginatedoperation-request-members)=
                                ## Request Members

                                **nextToken (String)**
                                : Placeholder documentation for `smithy.example#PaginatedOperationInput$nextToken`

                                **pageSize (Integer)**
                                : Placeholder documentation for `smithy.example#PaginatedOperationInput$pageSize`


                                (paginatedoperation-response-members)=
                                ## Response Members

                                **items (List\\<String\\>)**
                                : Placeholder documentation for `smithy.example#PaginatedOperationOutput$items`

                                **nextToken (String)**
                                : Placeholder documentation for `smithy.example#PaginatedOperationOutput$nextToken`
                                """));

    }
}
