/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.diff.testrunner.SmithyDiffTestCase;
import software.amazon.smithy.diff.testrunner.SmithyDiffTestSuite;

public class DiffTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyDiffTestCase.Result> callable) throws Exception {
        callable.call();
    }

    public static Stream<?> source() {
        return SmithyDiffTestSuite.defaultParameterizedTestSource(DiffTest.class);
    }
}
