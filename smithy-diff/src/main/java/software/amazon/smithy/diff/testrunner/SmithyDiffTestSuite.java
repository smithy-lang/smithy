/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.testrunner;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;

/**
 * Runs diff test cases against corresponding model `a`, model `b`, and validation `events` files.
 */
public final class SmithyDiffTestSuite {
    static final String EVENTS = ".events";
    private static final String DEFAULT_TEST_CASE_LOCATION = "diffs";
    private static final String EXT_SMITHY = ".smithy";
    private static final String EXT_JSON = ".json";
    private static final String MODEL_A = ".a";
    private static final String MODEL_B = ".b";

    private final List<SmithyDiffTestCase> cases = new ArrayList();
    private Supplier<ModelAssembler> modelAssemblerFactory = ModelAssembler::new;

    private SmithyDiffTestSuite() {}

    /**
     * Creates a new Smithy diff test suite.
     *
     * @return Returns the created test suite.
     */
    public static SmithyDiffTestSuite runner() {
        return new SmithyDiffTestSuite();
    }

    /**
     * Factory method used to easily create a JUnit 5 {@code ParameterizedTest}
     * {@code MethodSource} based on the given {@code Class}.
     *
     * <p>This method assumes that there is a resource named {@code diffs}
     * relative to the given class that contains test cases. It also assumes
     * validators and traits should be loaded using the {@code ClassLoader}
     * of the given {@code contextClass}, and that model discovery should be
     * used using the given {@code contextClass}.
     *
     * <p>Each returned {@code Object[]} contains the filename of the test as
     * the first argument, followed by a {@code Callable<SmithyDiffTestCase.Result>}
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
     * import software.amazon.smithy.diff.testrunner.SmithyDiffTestCase;
     * import software.amazon.smithy.diff.testrunner.SmithyDiffTestSuite;
     *
     * public class TestRunnerTest {
     *     \@ParameterizedTest(name = "\{0\}")
     *     \@MethodSource("source")
     *     public void testRunner(String filename, Callable&lt;SmithyDiffTestCase.Result&gt; callable)
     *         throws Exception {
     *         callable.call();
     *     }
     *
     *     public static Stream&lt;?&gt; source() {
     *         return SmithyDiffTestSuite.defaultParameterizedTestSource(TestRunnerTest.class);
     *     }
     * }
     * }</pre>
     *
     * @param contextClass The class to use for loading diffs and model discovery.
     * @return Returns the Stream that should be used as a JUnit 5 {@code MethodSource} return value.
     */
    public static Stream<Object[]> defaultParameterizedTestSource(Class<?> contextClass) {
        ClassLoader classLoader = contextClass.getClassLoader();
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader);
        return SmithyDiffTestSuite.runner()
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
     * <p>Each returned {@code Object[]} contains the name of the test as
     * the first argument, followed by a {@code Callable<SmithyDiffTestCase.Result>}
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
     * import software.amazon.smithy.diff.testrunner.SmithyDiffTestCase;
     * import software.amazon.smithy.diff.testrunner.SmithyDiffTestSuite;
     *
     * public class TestRunnerTest {
     *     \@ParameterizedTest(name = "\{0\}")
     *     \@MethodSource("source")
     *     public void testRunner(String filename, Callable&lt;SmithyDiffTestCase.Result&gt; callable)
     *         throws Exception {
     *         callable.call();
     *     }
     *
     *     public static Stream&lt;?&gt; source() {
     *         ModelAssembler assembler = Model.assembler(TestRunnerTest.class.getClassLoader());
     *         return SmithyDiffTestSuite.runner()
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
            Callable<SmithyDiffTestCase.Result> callable = createTestCaseCallable(testCase);
            Callable<SmithyDiffTestCase.Result> wrappedCallable = () -> callable.call().unwrap();
            return new Object[] {testCase.getName(), wrappedCallable};
        });
    }

    /**
     * Adds a test case to the test suite.
     *
     * @param testCase Test case to add.
     * @return Returns the test suite.
     */
    public SmithyDiffTestSuite addTestCase(SmithyDiffTestCase testCase) {
        cases.add(testCase);
        return this;
    }

    /**
     * Adds test cases by crawling a directory and looking for events files
     * that end with ".events". Corresponding ".a.(json|smithy)" and ".b.(json|smithy)"
     * files are expected to be found for each found events file.
     *
     * <p>See {@link SmithyDiffTestCase#from} for a description of how
     * the events file is expected to be formatted.
     *
     * @param modelDirectory Directory that contains diff models.
     * @return Returns the test suite.
     * @see SmithyDiffTestCase#from
     */
    public SmithyDiffTestSuite addTestCasesFromDirectory(Path modelDirectory) {
        try (Stream<Path> files = Files.walk(modelDirectory)) {
            String modelDirectoryName = modelDirectory.toString();
            files
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(fileName -> fileName.endsWith(EVENTS))
                    .map(fileName -> SmithyDiffTestCase.from(
                            modelDirectory,
                            fileName.substring(modelDirectoryName.length() + 1, fileName.length() - EVENTS.length())))
                    .forEach(this::addTestCase);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method for supplying a directory using a class loader.
     *
     * @param url URL that contains diff models.
     * @return Returns the test suite.
     * @throws IllegalArgumentException if a non-file scheme URL is provided.
     * @see #addTestCasesFromDirectory
     */
    public SmithyDiffTestSuite addTestCasesFromUrl(URL url) {
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
    public SmithyDiffTestSuite setModelAssemblerFactory(Supplier<ModelAssembler> modelAssemblerFactory) {
        this.modelAssemblerFactory = Objects.requireNonNull(modelAssemblerFactory);
        return this;
    }

    /**
     * Creates a {@code Stream} of {@code Callable} objects that can be used
     * to execute each test case.
     *
     * <p>The {@link SmithyDiffTestCase.Result#unwrap()} method must be called on
     * the result of each callable in order to actually assert that the test
     * case result is OK.
     *
     * @return Returns a stream of test case callables.
     */
    public Stream<Callable<SmithyDiffTestCase.Result>> testCaseCallables() {
        return cases.stream().map(this::createTestCaseCallable);
    }

    private Callable<SmithyDiffTestCase.Result> createTestCaseCallable(SmithyDiffTestCase testCase) {
        return () -> testCase.createResult(ModelDiff.compare(
                getModel(testCase, modelAssemblerFactory.get(), MODEL_A),
                getModel(testCase, modelAssemblerFactory.get(), MODEL_B)));
    }

    private static Model getModel(SmithyDiffTestCase testCase, ModelAssembler assembler, String infix) {
        Path modelPath = testCase.getPath().resolve(testCase.getName() + infix + EXT_SMITHY);
        if (!Files.exists(modelPath)) {
            modelPath = modelPath.resolveSibling(testCase.getName() + infix + EXT_JSON);
        }
        return assembler
                .addImport(modelPath)
                .assemble()
                .unwrap();
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
        List<SmithyDiffTestCase.Result> failedResults = Collections.synchronizedList(new ArrayList<>());
        List<Callable<SmithyDiffTestCase.Result>> callables = testCaseCallables().collect(Collectors.toList());

        try {
            for (Future<SmithyDiffTestCase.Result> future : executorService.invokeAll(callables)) {
                SmithyDiffTestCase.Result testCaseResult = waitOnFuture(future);
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

    private SmithyDiffTestCase.Result waitOnFuture(Future<SmithyDiffTestCase.Result> future)
            throws InterruptedException {
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
        private final List<SmithyDiffTestCase.Result> failedResults;

        Result(int successCount, List<SmithyDiffTestCase.Result> failedResults) {
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
        public List<SmithyDiffTestCase.Result> getFailedResults() {
            return failedResults;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(String.format(
                    "Smithy diff test runner encountered %d successful result(s), and %d failed result(s)",
                    successCount,
                    failedResults.size()));
            failedResults.forEach(failed -> builder.append('\n').append(failed.toString()).append('\n'));
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
