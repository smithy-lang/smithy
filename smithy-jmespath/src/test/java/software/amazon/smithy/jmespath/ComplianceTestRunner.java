/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.jmespath;

import org.junit.jupiter.api.Assumptions;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.utils.IoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

class ComplianceTestRunner<T> {
    private static final String DEFAULT_TEST_CASE_LOCATION = "compliance";
    private static final String SUBJECT_MEMBER = "given";
    private static final String CASES_MEMBER = "cases";
    private static final String EXPRESSION_MEMBER = "expression";
    private static final String RESULT_MEMBER = "result";
    private static final String ERROR_MEMBER = "result";
    // TODO: Remove these suppressions as remaining functions are supported
    private static final List<String> UNSUPPORTED_FUNCTIONS = List.of(
            "to_string",
            "to_array",
            "merge",
            "map");
    private final JmespathRuntime<T> runtime;
    private final List<TestCase<T>> testCases = new ArrayList<>();

    private ComplianceTestRunner(JmespathRuntime<T> runtime) {
        this.runtime = runtime;
    }

    public static <T> Stream<Object[]> defaultParameterizedTestSource(Class<?> contextClass, JmespathRuntime<T> runtime) {
        return new ComplianceTestRunner<>(runtime)
                .addTestCasesFromUrl(Objects.requireNonNull(contextClass.getResource(DEFAULT_TEST_CASE_LOCATION)))
                .parameterizedTestSource();
    }

    public ComplianceTestRunner<T> addTestCasesFromUrl(URL url) {
        if (!url.getProtocol().equals("file")) {
            throw new IllegalArgumentException("Only file URLs are supported by the test runner: " + url);
        }

        try {
            return addTestCasesFromDirectory(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Stream<Object[]> parameterizedTestSource() {
        return testCases.stream().map(testCase -> new Object[] {testCase.name(), testCase});
    }

    public ComplianceTestRunner<T> addTestCasesFromDirectory(Path directory) {
        for (File file : Objects.requireNonNull(directory.toFile().listFiles())) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                testCases.addAll(TestCase.from(file, runtime));
            }
        }
        return this;
    }

    private record TestCase<T>(JmespathRuntime<T> runtime, String testSuite, T given, String expression, T expectedResult, T expectedError)
            implements Runnable {
        public static <T> List<TestCase<T>> from(File file, JmespathRuntime<T> runtime) {
            var testSuiteName = file.getName().substring(0, file.getName().lastIndexOf('.'));
            var testCases = new ArrayList<TestCase<T>>();
            try (FileInputStream is = new FileInputStream(file)) {
                String text = IoUtils.readUtf8File(file.getPath());
                T tests = JmespathExpression.parseJson(text, runtime);

                for (var test : runtime.toIterable(tests)) {
                    var given = runtime.value(test, runtime.createString(SUBJECT_MEMBER));
                    for (var testCase : runtime.toIterable(runtime.value(test, runtime.createString(CASES_MEMBER)))) {
                        String expression = runtime.toString(runtime.value(testCase, runtime.createString(EXPRESSION_MEMBER)));
                        // Filters out unsupported functions
                        // TODO: Remove once all built-in functions are supported
                        if (testSuiteName.equals("functions")
                                && UNSUPPORTED_FUNCTIONS.stream().anyMatch(expression::contains)) {
                            continue;
                        }
                        T result = runtime.value(testCase, runtime.createString(RESULT_MEMBER));
                        T error = runtime.value(testCase, runtime.createString(ERROR_MEMBER));
                        testCases.add(new TestCase<T>(runtime, testSuiteName, given, expression, result, error));
                    }
                }
                return testCases;
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not find test file.", e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private String name() {
            return testSuite + " (" + given + ")[" + expression + "]";
        }

        @Override
        public void run() {
            var parsed = JmespathExpression.parse(expression);
            var result = new Evaluator<>(given, runtime).visit(parsed);
            if (!runtime.is(expectedError, RuntimeType.NULL)) {
                Assumptions.abort("Expected errors not yet supported");
            }
            if (!runtime.equal(expectedResult, result)) {
                throw new AssertionError("Expected does not match actual. \n"
                        + "Expected: " + expectedResult + "\n"
                        + "Actual: " + result + "\n"
                        + "For query: " + expression + "\n");
            }
        }
    }
}
