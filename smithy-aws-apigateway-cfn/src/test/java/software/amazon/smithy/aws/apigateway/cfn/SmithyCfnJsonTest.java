/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuildPlugin;

public class SmithyCfnJsonTest {

    @Test
    public void hasCorrectName() {
        assertThat(new SmithyCfnJson().getName(), equalTo("smithy-cfn-json"));
    }

    @Test
    public void requiresValidModel() {
        assertTrue(new SmithyCfnJson().requiresValidModel());
    }

    @Test
    public void isDiscoverableViaSpi() {
        boolean found = false;
        for (SmithyBuildPlugin plugin : ServiceLoader.load(SmithyBuildPlugin.class)) {
            if (plugin.getName().equals("smithy-cfn-json")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "smithy-cfn-json plugin should be discoverable via ServiceLoader");
    }
}
