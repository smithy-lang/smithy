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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.smithy.model.loader.ModelAssembler;

/**
 * Runs test cases against a directory of models and error files.
 */
public final class SmithyTestSuite {
    private List<Callable<Void>> testCaseCallables = new ArrayList<>();
    private Supplier<ModelAssembler> modelAssemblerFactory = ModelAssembler::new;
    private final List<SmithyTestCase.Result> failedResults = Collections.synchronizedList(new ArrayList<>());
    private ExecutorService executorService;

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
     * Adds a test case to the test suite.
     *
     * @param testCase Test case to add.
     * @return Returns the test suite.
     */
    public SmithyTestSuite addTestCase(SmithyTestCase testCase) {
        addTestCaseCallable(testCase);
        return this;
    }

    private void addTestCaseCallable(SmithyTestCase testCase) {
        testCaseCallables.add(() -> {
            ModelAssembler assembler = modelAssemblerFactory.get();
            assembler.addImport(testCase.getModelLocation());
            SmithyTestCase.Result result = testCase.createResult(assembler.assemble());
            if (result.isInvalid()) {
                failedResults.add(result);
            }
            return null;
        });
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
        try {
            Files.walk(modelDirectory)
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
     * Sets a custom {@code ExecutorService} to use for executing test cases.
     *
     * @param executorService ExecutorService to use.
     * @return Returns the test suite.
     */
    public SmithyTestSuite setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Executes the test runner.
     *
     * @return Returns the test case result object on success.
     * @throws Error if the validation events do not match expectations.
     */
    public Result run() {
        failedResults.clear();

        if (executorService == null) {
            // Tests are mostly CPU bound. But rather than use all available
            // cores, use cores - 1 since test suites are usually run along
            // with other unit tests which are probably running in multiple
            // threads.
            int numberOfCores = Runtime.getRuntime().availableProcessors();
            executorService = Executors.newFixedThreadPool(numberOfCores - 1);
        }

        try {
            for (Future<Void> future : executorService.invokeAll(testCaseCallables)) {
                waitOnFuture(future);
            }
            Result result = new Result(testCaseCallables.size() - failedResults.size(), failedResults);
            if (failedResults.isEmpty()) {
                return result;
            }
            throw new Error(result);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            throw new Error("Error executing test suite: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
            testCaseCallables.clear();
        }
    }

    private void waitOnFuture(Future<Void> future) throws InterruptedException {
        try {
            future.get();
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

        public String toString() {
            StringBuilder builder = new StringBuilder(String.format(
                    "Smithy validation test runner encountered %d successful result(s), and %d failed result(s)",
                    getSuccessCount(), getFailedResults().size()));
            getFailedResults().forEach(failed -> appendResult(failed, builder));
            return builder.toString();
        }

        private static void appendResult(SmithyTestCase.Result result, StringBuilder builder) {
            builder.append("\n\n============= Model Validation Result =============\n")
                    .append(result.getModelLocation())
                    .append("\n");

            if (!result.getUnmatchedEvents().isEmpty()) {
                builder.append("\n* Did not match the following events: \n");
                builder.append(result.getUnmatchedEvents().stream()
                                       .map(Object::toString)
                                       .map(SmithyTestSuite::formatString)
                                       .sorted()
                                       .collect(Collectors.joining("\n")));
                builder.append("\n");
            }

            if (!result.getExtraEvents().isEmpty()) {
                builder.append("\n* Encountered unexpected events: \n");
                builder.append(result.getExtraEvents().stream()
                                       .map(Object::toString)
                                       .map(SmithyTestSuite::formatString)
                                       .sorted()
                                       .collect(Collectors.joining("\n")));
                builder.append("\n");
            }
        }
    }

    private static String formatString(String value) {
        return value.replace("\n", "\\n");
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
