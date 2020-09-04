/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.traits;

import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;

public class TestRunnerTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyTestCase.Result> callable) throws Exception {
        callable.call();
    }

    public static Stream<?> source() {
        return SmithyTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
    }
}

