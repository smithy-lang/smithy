/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.testrunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class SmithyDiffTestSuiteTest {
    @Test
    public void throwsWhenFailed() {
        try {
            SmithyDiffTestSuite.runner()
                    .addTestCasesFromUrl(getClass().getResource("testrunner/invalid"))
                    .run();
            fail("Expected to throw");
        } catch (SmithyDiffTestSuite.Error e) {
            assertThat(e.result.getSuccessCount(), is(1));
            assertThat(e.result.getFailedResults().size(), is(2));
            assertThat(e.getMessage(), containsString("Did not match the"));
            assertThat(e.getMessage(), containsString("Encountered unexpected"));
        }
    }

    @Test
    public void runsCaseWithFile() {
        SmithyDiffTestSuite.Result result = SmithyDiffTestSuite.runner()
                .addTestCasesFromUrl(getClass().getResource("testrunner/valid"))
                .run();

        assertThat(result.getFailedResults().size(), is(0));
        assertThat(result.getSuccessCount(), is(3));
    }

    @Test
    public void onlySupportsFiles() throws MalformedURLException {
        assertThrows(IllegalArgumentException.class, () -> {
            SmithyDiffTestSuite.runner().addTestCasesFromUrl(new URL("https://127.0.0.1/foo"));
        });
    }
}
