/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.test;

import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;

class TestRunnerTest {
    static Stream<?> source() {
        return SmithyTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    void testRunner(String filename, Callable<SmithyTestCase.Result> callable) throws Exception {
        callable.call();
    }
}
