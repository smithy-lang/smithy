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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.Validator;

/**
 * Runs test cases against a directory of models and error files.
 */
public final class SmithyTestSuite {

    private static final Logger LOGGER = Logger.getLogger(SmithyTestSuite.class.getName());
    private static final String DEFAULT_TEST_CASE_LOCATION = "errorfiles";

    private final List<SmithyTestCase> cases = new ArrayList<>();
    private Supplier<ModelAssembler> modelAssemblerFactory = ModelAssembler::new;

    private SmithyTestSuite() {}

    /**
     * Creates a new Smithy test suite.
     *
     * @return Returns the created test suite.
     */
    public static SmithyTestSuite runner() {
        return new SmithyTestSuite();
    }

    /**
     * Factory method used to easily create a JUnit 5 {@code ParameterizedTest}
     * {@code MethodSource} based on the given {@code Class}.
     *
     * <p>This method assumes that there is a resource named {@code errorfiles}
     * relative to the given class that contains test cases. It also assumes
     * validators and traits should be loaded using the {@code ClassLoader}
     * of the given {@code contextClass}, and that model discovery should be
     * used using the given {@code contextClass}.
     *
     * <p>Each returned {@code Object[]} contains the filename of the test as
     * the first argument, followed by a {@code Callable<SmithyTestCase.Result>}
     * as the second argument. All a parameterized test needs to do is call
     * {@code call} on the provided {@code Callable} to execute the test and
     * fail if the test case is invalid.
     *
     * <p>For example, the following can be used as a unit test:
     *
     * <pre>{@code
     * import java.util.concurrent.Callable;
     * import java.util.stream.Stream;
     * import org.junit.jupiter.params.ParameterizedTest;
     * import org.junit.jupiter.params.provider.MethodSource;
     * import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
     * import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;
     *
     * public class TestRunnerTest {
     *     \@ParameterizedTest(name = "\{0\}")
     *     \@MethodSource("source")
     *     public void testRunner(String filename, Callable&lt;SmithyTestCase.Result&gt; callable) throws Exception {
     *         callable.call();
     *     }
     *
     *     public static Stream&lt;?&gt; source() {
     *         return SmithyTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
     *     }
     * }
     * }</pre>
     *
     * @param contextClass The class to use for loading errorfiles and model discovery.
     * @return Returns the Stream that should be used as a JUnit 5 {@code MethodSource} return value.
     */
    public static Stream<Object[]> defaultParameterizedTestSource(Class<?> contextClass) {
        ClassLoader classLoader = contextClass.getClassLoader();
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader);
        return SmithyTestSuite.runner()
                .setModelAssemblerFactory(assembler::copy)
                .addTestCasesFromUrl(contextClass.getResource(DEFAULT_TEST_CASE_LOCATION))
                .parameterizedTestSource();
    }

    /**
     * Factory method used to create a JUnit 5 {@code ParameterizedTest}
     * {@code MethodSource}.
     *
     * <p>Test cases need to be added to the test suite before calling this,
     * for example by using {@link #addTestCasesFromDirectory(Path)}.
     *
     * <p>Each returned {@code Object[]} contains the filename of the test as
     * the first argument, followed by a {@code Callable<SmithyTestCase.Result>}
     * as the second argument. All a parameterized test needs to do is call
     * {@code call} on the provided {@code Callable} to execute the test and
     * fail if the test case is invalid.
     *
     * <p>For example, the following can be used as a unit test:
     *
     * <pre>{@code
     * import java.util.concurrent.Callable;
     * import java.util.stream.Stream;
     * import org.junit.jupiter.params.ParameterizedTest;
     * import org.junit.jupiter.params.provider.MethodSource;
     * import software.amazon.smithy.model.validation.testrunner.SmithyTestCase;
     * import software.amazon.smithy.model.validation.testrunner.SmithyTestSuite;
     *
     * public class TestRunnerTest {
     *     \@ParameterizedTest(name = "\{0\}")
     *     \@MethodSource("source")
     *     public void testRunner(String filename, Callable&lt;SmithyTestCase.Result&gt; callable) throws Exception {
     *         callable.call();
     *     }
     *
     *     public static Stream&lt;?&gt; source() {
     *         ModelAssembler assembler = Model.assembler(TestRunnerTest.class.getClassLoader());
     *         return SmithyTestSuite.runner()
     *                 .setModelAssemblerFactory(assembler::copy)
     *                 .addTestCasesFromUrl(TestRunnerTest.class.getResource("errorfiles"))
     *                 .parameterizedTestSource();
     *     }
     * }
     * }</pre>
     *
     * @return Returns the Stream that should be used as a JUnit 5 {@code MethodSource} return value.
     */
    public Stream<Object[]> parameterizedTestSource() {
        return cases.stream().map(testCase -> {
            Callable<SmithyTestCase.Result> callable = createTestCaseCallable(testCase);
            Callable<SmithyTestCase.Result> wrappedCallable = () -> callable.call().unwrap();
            return new Object[] {testCase.getModelLocation(), wrappedCallable};
        });
    }

    /**
     * Adds a test case to the test suite.
     *
     * @param testCase Test case to add.
     * @return Returns the test suite.
     */
    public SmithyTestSuite addTestCase(SmithyTestCase testCase) {
        cases.add(testCase);
        return this;
    }

    /**
     * Adds test cases by crawling a directory and looking for model files
     * that end with ".json" and ".smithy". A corresponding ".errors" file is
     * expected to be found for each found model file.
     *
     * <p>See {@link SmithyTestCase#fromModelFile} for a description of how
     * the errors file is expected to be formatted.
     *
     * @param modelDirectory Directory that contains models.
     * @return Returns the test suite.
     * @see SmithyTestCase#fromModelFile
     */
    public SmithyTestSuite addTestCasesFromDirectory(Path modelDirectory) {
        try (Stream<Path> files = Files.walk(modelDirectory)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(file -> {
                        String filename = file.toString();
                        return filename.endsWith(".json") || filename.endsWith(".smithy");
                    })
                    .map(file -> SmithyTestCase.fromModelFile(file.toString()))
                    .forEach(this::addTestCase);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method for supplying a directory using a class loader.
     *
     * @param url URL that contains models.
     * @return Returns the test suite.
     * @throws IllegalArgumentException if a non-file scheme URL is provided.
     * @see #addTestCasesFromDirectory
     */
    public SmithyTestSuite addTestCasesFromUrl(URL url) {
        if (!url.getProtocol().equals("file")) {
            throw new IllegalArgumentException("Only file URLs are supported by the testrunner: " + url);
        }

        try {
            return addTestCasesFromDirectory(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a custom {@link ModelAssembler} factory to use to create a
     * {@code ModelAssembler} for each test case.
     *
     * <p>The supplier must return a new instance of a Model assembler
     * each time it is called. Model assemblers are mutated and execute
     * in parallel.
     *
     * @param modelAssemblerFactory Model assembler factory to use.
     * @return Returns the test suite.
     */
    public SmithyTestSuite setModelAssemblerFactory(Supplier<ModelAssembler> modelAssemblerFactory) {
        this.modelAssemblerFactory = Objects.requireNonNull(modelAssemblerFactory);
        return this;
    }

    /**
     * Creates a {@code Stream} of {@code Callable} objects that can be used
     * to execute each test case.
     *
     * <p>The {@link SmithyTestCase.Result#unwrap()} method must be called on
     * the result of each callable in order to actually assert that the test
     * case result is OK.
     *
     * @return Returns a stream of test case callables.
     */
    public Stream<Callable<SmithyTestCase.Result>> testCaseCallables() {
        return cases.stream().map(this::createTestCaseCallable);
    }

    private Callable<SmithyTestCase.Result> createTestCaseCallable(SmithyTestCase testCase) {
        boolean useLegacyValidationMode = isLegacyValidationRequired(testCase);
        return () -> {
            ModelAssembler assembler = modelAssemblerFactory.get();
            assembler.addImport(testCase.getModelLocation());
            if (useLegacyValidationMode) {
                assembler.putProperty("LEGACY_VALIDATION_MODE", true);
            }
            return testCase.createResult(assembler.assemble());
        };
    }

    // We introduced the concept of "critical" validation events after many errorfiles were created that relied
    // on all validators being run, including validators now considered critical that prevent further validation.
    // If we didn't account for that here, the addition of "critical" validators would have been a breaking change.
    // To make it backward compatible, we preemptively detect if the errorfiles contains both critical and
    // non-critical validation event assertions, and if so, we run validation using an internal-only validation
    // mode that doesn't fail after critical validators report errors.
    private boolean isLegacyValidationRequired(SmithyTestCase testCase) {
        Set<String> criticalEvents = new TreeSet<>();
        Set<String> nonCriticalEvents = new TreeSet<>();

        for (ValidationEvent event : testCase.getExpectedEvents()) {
            if (isCriticalValidationEvent(event)) {
                criticalEvents.add(event.getId());
            } else {
                nonCriticalEvents.add(event.getId());
            }
        }

        if (!criticalEvents.isEmpty() && !nonCriticalEvents.isEmpty()) {
            LOGGER.warning(String.format("Test suite `%s` relies on the emission of non-critical validation events "
                                         + "after critical validation events were emitted. This test case should be "
                                         + "refactored so that critical validation events are tested using separate "
                                         + "test cases from non-critical events. Critical events: %s. Non-critical "
                                         + "events: %s",
                                         testCase.getModelLocation(),
                                         criticalEvents,
                                         nonCriticalEvents));
            return true;
        }

        return false;
    }

    private static boolean isCriticalValidationEvent(ValidationEvent event) {
        if (ValidationUtils.isCriticalEvent(event.getId())) {
            return true;
        }

        // In addition to the method to the check based on Validator classes, MODEL validation event IDs marked as
        // ERROR that go along with non-critical events requires legacy validation.
        return event.getId().equals(Validator.MODEL_ERROR) && event.getSeverity() == Severity.ERROR;
    }

    /**
     * Executes the test runner.
     *
     * @return Returns the test case result object on success.
     * @throws Error if the validation events do not match expectations.
     */
    public Result run() {
        return run(ForkJoinPool.commonPool());
    }

    /**
     * Executes the test runner with a specific {@code ExecutorService}.
     *
     * <p>Tests ideally should use JUnit 5's ParameterizedTest as described
     * in {@link #parameterizedTestSource()}. However, this method can be
     * used to run tests in parallel in other scenarios (like if you aren't
     * using JUnit, or not running tests cases during unit tests).
     *
     * @param executorService Executor service to execute tests with.
     * @return Returns the test case result object on success.
     * @throws Error if the validation events do not match expectations.
     */
    public Result run(ExecutorService executorService) {
        List<SmithyTestCase.Result> failedResults = Collections.synchronizedList(new ArrayList<>());
        List<Callable<SmithyTestCase.Result>> callables = testCaseCallables().collect(Collectors.toList());

        try {
            for (Future<SmithyTestCase.Result> future : executorService.invokeAll(callables)) {
                SmithyTestCase.Result testCaseResult = waitOnFuture(future);
                if (testCaseResult.isInvalid()) {
                    failedResults.add(testCaseResult);
                }
            }

            Result result = new Result(callables.size() - failedResults.size(), failedResults);
            if (failedResults.isEmpty()) {
                return result;
            }

            throw new Error(result);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            throw new Error("Error executing test suite: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    private SmithyTestCase.Result waitOnFuture(Future<SmithyTestCase.Result> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            // Try to throw the original exception as-is if possible.
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new Error("Error executing test case: " + e.getMessage(), cause);
            }
        }
    }

    /**
     * Value result of executing the test suite.
     */
    public static final class Result {
        private final int successCount;
        private final List<SmithyTestCase.Result> failedResults;

        Result(int successCount, List<SmithyTestCase.Result> failedResults) {
            this.successCount = successCount;
            this.failedResults = Collections.unmodifiableList(failedResults);
        }

        /**
         * @return Returns the number of test cases that passed.
         */
        public int getSuccessCount() {
            return successCount;
        }

        /**
         * @return Returns the test cases that failed.
         */
        public List<SmithyTestCase.Result> getFailedResults() {
            return failedResults;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(String.format(
                    "Smithy validation test runner encountered %d successful result(s), and %d failed result(s)",
                    getSuccessCount(), getFailedResults().size()));
            getFailedResults().forEach(failed -> builder.append('\n').append(failed.toString()).append('\n'));
            return builder.toString();
        }
    }

    /**
     * Thrown when errors are encountered in the test runner.
     */
    public static final class Error extends RuntimeException {
        public final Result result;

        Error(Result result) {
            super(result.toString());
            this.result = result;
        }

        Error(String message, Throwable previous) {
            super(message, previous);
            this.result = new Result(0, Collections.emptyList());
        }
    }
}
