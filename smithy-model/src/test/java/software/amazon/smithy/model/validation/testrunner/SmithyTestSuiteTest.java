/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.validation.testrunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmithyTestSuiteTest {
    @Test
    public void throwsWhenFailed() {
        try {
            SmithyTestSuite.runner()
                    .addTestCasesFromUrl(getClass().getResource("testrunner/invalid"))
                    .run();
            Assertions.fail("Expected to throw");
        } catch (SmithyTestSuite.Error e) {
            assertThat(e.result.getSuccessCount(), is(1));
            assertThat(e.result.getFailedResults().size(), is(2));
            assertThat(e.getMessage(), containsString("Did not match the"));
            assertThat(e.getMessage(), containsString("Encountered unexpected"));
        }
    }

    @Test
    public void runsCaseWithFile() {
        SmithyTestSuite.Result result = SmithyTestSuite.runner()
                .addTestCasesFromUrl(getClass().getResource("testrunner/valid"))
                .run();

        assertThat(result.getFailedResults().size(), is(0));
        assertThat(result.getSuccessCount(), is(4));
    }

    @Test
    public void onlySupportsFiles() throws MalformedURLException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SmithyTestSuite.runner().addTestCasesFromUrl(new URL("https://127.0.0.1/foo"));
        });
    }
}
