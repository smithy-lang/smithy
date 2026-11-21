/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.docgen.utils.AbstractDocGenFileTest;

public class ResourceGeneratorTest extends AbstractDocGenFileTest {
    private static final URL TEST_FILE =
            Objects.requireNonNull(ResourceGeneratorTest.class.getResource("resource-generator.smithy"));

    @Override
    protected URL testFile() {
        return TEST_FILE;
    }

    @Test
    public void testResourcePropertyDoesNotDuplicateSectionAnchor() {
        MockManifest manifest = new MockManifest();
        execute(manifest);
        var resourceDocs = manifest.expectFileString("/content/resources/Widget.md");
        assertThat(resourceDocs, containsString("(widget-operations)="));
        assertThat(resourceDocs.indexOf("(widget-operations)="),
                equalTo(resourceDocs.lastIndexOf("(widget-operations)=")));
    }
}
