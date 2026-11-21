/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.generators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.docgen.utils.AbstractDocGenFileTest;

public class ServiceGeneratorTest extends AbstractDocGenFileTest {
    private static final URL TEST_FILE =
            Objects.requireNonNull(ServiceGeneratorTest.class.getResource("service-generator.smithy"));

    @Override
    protected URL testFile() {
        return TEST_FILE;
    }

    @Test
    public void testAuthSchemesHaveLinkTargets() {
        MockManifest manifest = new MockManifest();
        execute(manifest);
        var serviceDocs = manifest.expectFileString("/content/index.md");
        assertThat(serviceDocs, containsString("""
                (httpbearerauth)=
                **[httpBearerAuth](#httpbearerauth)**
                """));
    }
}
