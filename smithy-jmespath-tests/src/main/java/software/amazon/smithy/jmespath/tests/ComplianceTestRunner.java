/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.utils.IoUtils;

public class ComplianceTestRunner<T> {
    private static final String DEFAULT_TEST_CASE_LOCATION = "compliance";
    private static final String SUBJECT_MEMBER = "given";
    private static final String CASES_MEMBER = "cases";
    private static final String COMMENT_MEMBER = "comment";
    private static final String EXPRESSION_MEMBER = "expression";
    private static final String RESULT_MEMBER = "result";
    private static final String ERROR_MEMBER = "error";
    private static final String BENCH_MEMBER = "bench";
    // TODO: Remove these suppressions as remaining functions are supported
    private static final List<String> UNSUPPORTED_FUNCTIONS = List.of(
            "avg",
            "contains",
            "ceil",
            "ends_with",
            "floor",
            "join",
            "map",
            "max",
            "max_by",
            "merge",
            "min",
            "min_by",
            "not_null",
            "reverse",
            "sort",
            "sort_by",
            "starts_with",
            "sum",
            "to_array",
            "to_string",
            "to_number");
    private final JmespathRuntime<T> runtime;
    private final List<TestCase<T>> testCases = new ArrayList<>();

    private ComplianceTestRunner(JmespathRuntime<T> runtime) {
        this.runtime = runtime;
    }

    public static <T> Stream<Object[]> defaultParameterizedTestSource(JmespathRuntime<T> runtime) {
        ComplianceTestRunner<T> runner = new ComplianceTestRunner<>(runtime);
        URL manifest = ComplianceTestRunner.class.getResource(DEFAULT_TEST_CASE_LOCATION + "/MANIFEST");
        try (var reader = new BufferedReader(new InputStreamReader(manifest.openStream(), StandardCharsets.UTF_8))) {
            reader.lines().forEach(line -> {
                var url = ComplianceTestRunner.class.getResource(DEFAULT_TEST_CASE_LOCATION + "/" + line.trim());
                runner.testCases.addAll(TestCase.from(url, runtime));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return runner.parameterizedTestSource();
    }

    public Stream<Object[]> parameterizedTestSource() {
        return testCases.stream().map(testCase -> new Object[] {testCase.name(), testCase});
    }

    private record TestCase<T>(
            JmespathRuntime<T> runtime,
            String testSuite,
            String comment,
            T given,
            String expression,
            T expectedResult,
            JmespathExceptionType expectedError,
            String benchmark)
            implements Runnable {
        public static <T> List<TestCase<T>> from(URL url, JmespathRuntime<T> runtime) {
            var path = url.getPath();
            var testSuiteName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
            var testCases = new ArrayList<TestCase<T>>();
            String text = IoUtils.readUtf8Url(url);
            T tests = JmespathExpression.parseJson(text, runtime);

            for (var test : runtime.toIterable(tests)) {
                var given = value(runtime, test, SUBJECT_MEMBER);
                for (var testCase : runtime.toIterable(value(runtime, test, CASES_MEMBER))) {
                    String comment = valueAsString(runtime, testCase, COMMENT_MEMBER);
                    String expression = valueAsString(runtime, testCase, EXPRESSION_MEMBER);
                    var result = value(runtime, testCase, RESULT_MEMBER);
                    var expectedErrorString = valueAsString(runtime, testCase, ERROR_MEMBER);
                    var expectedError =
                            expectedErrorString != null ? JmespathExceptionType.fromID(expectedErrorString) : null;

                    // Special case: The spec says function names cannot be quoted,
                    // but our parser allows it and it may be useful in the future.
                    if ("function names cannot be quoted".equals(comment)) {
                        expectedError = JmespathExceptionType.UNKNOWN_FUNCTION;
                    }

                    var benchmark = valueAsString(runtime, testCase, BENCH_MEMBER);
                    testCases.add(new TestCase<>(runtime,
                            testSuiteName,
                            comment,
                            given,
                            expression,
                            result,
                            expectedError,
                            benchmark));
                }
            }
            return testCases;
        }

        private static <T> T value(JmespathRuntime<T> runtime, T object, String key) {
            return runtime.value(object, runtime.createString(key));
        }

        private static <T> String valueAsString(JmespathRuntime<T> runtime, T object, String key) {
            T result = runtime.value(object, runtime.createString(key));
            return runtime.is(result, RuntimeType.NULL) ? null : runtime.asString(result);
        }

        private String name() {
            return testSuite + (comment != null ? " - " + comment : "") + " (" + runtime.toString(given) + ")["
                    + expression + "]";
        }

        @Override
        public void run() {
            // Filters out unsupported functions
            // TODO: Remove once all built-in functions are supported
            if (UNSUPPORTED_FUNCTIONS.stream().anyMatch(expression::contains)) {
                Assumptions.abort("Unsupported functions");
            }

            try {
                var parsed = JmespathExpression.parse(expression);
                var result = new Evaluator<>(given, runtime).visit(parsed);
                if (benchmark != null) {
                    // Benchmarks don't include expected results or errors
                    return;
                }
                if (expectedError != null) {
                    throw new AssertionError("Expected " + expectedError + " error but no error occurred. \n"
                            + "Actual:    " + runtime.toString(result) + "\n"
                            + "For query: " + expression + "\n");
                } else {
                    if (!runtime.equal(expectedResult, result)) {
                        throw new AssertionError("Expected does not match actual. \n"
                                + "Expected:  " + runtime.toString(expectedResult) + "\n"
                                + "Actual:    " + runtime.toString(result) + "\n"
                                + "For query: " + expression + "\n");
                    }
                }
            } catch (JmespathException e) {
                if (!e.getType().equals(expectedError)) {
                    throw new AssertionError("Expected error does not match actual error. \n"
                            + "Expected: " + (expectedError != null ? expectedError : "(no error)") + "\n"
                            + "Actual: " + e.getType() + " - " + e.getMessage() + "\n"
                            + "For query: " + expression + "\n");
                }
            }
        }
    }
}
