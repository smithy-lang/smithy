/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;
import software.amazon.smithy.utils.IoUtils;

public class TestRunnerTest {
    public static Stream<?> source() {
        return SmithyTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
    }

    public static EndpointRuleSet getMinimalEndpointRuleSet() {
        return getEndpointRuleSet(TestRunnerTest.class, "minimal-ruleset.json");
    }

    public static EndpointRuleSet getEndpointRuleSet(Class klass, String file) {
        return EndpointRuleSet.fromNode(Node.parseJsonWithComments(IoUtils.readUtf8Resource(klass, file)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyTestCase.Result> callable) throws Exception {
        callable.call();
    }
}
