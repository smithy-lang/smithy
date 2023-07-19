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

import static java.lang.String.format;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.IoUtils;

/**
 * Runs a single test case by loading a model and ensuring the resulting
 * events match the validation events stored in a newline separated file.
 */
public final class SmithyTestCase {
    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "^\\[(?<severity>SUPPRESSED|NOTE|WARNING|DANGER|ERROR)] (?<shape>[^ ]+): ?(?<message>.*) \\| (?<id>[^)]+)");

    private final List<ValidationEvent> expectedEvents;
    private final String modelLocation;

    /**
     * @param modelLocation Location of where the model is stored.
     * @param expectedEvents The expected validation events to encounter.
     */
    public SmithyTestCase(String modelLocation, List<ValidationEvent> expectedEvents) {
        this.modelLocation = Objects.requireNonNull(modelLocation);
        this.expectedEvents = Collections.unmodifiableList(expectedEvents);
    }

    /**
     * Creates a test case from a model file.
     *
     * <p>The error file is expected to be stored in the same directory
     * as the model file and is assumed to be named the same as the
     * file with the file extension replaced with ".errors".
     *
     * <p>The accompanying error file is a newline separated list of error
     * strings, where each error is defined in the following format:
     * {@code [SEVERITY] shapeId message | EventId filename:line:column}.
     * A shapeId of "-" means that a specific shape is not targeted.
     *
     * @param modelLocation File location of the model.
     * @return Returns the created test case.
     * @throws IllegalArgumentException if the file does not contain an extension.
     */
    public static SmithyTestCase fromModelFile(String modelLocation) {
        String errorFileLocation = inferErrorFileLocation(modelLocation);
        List<ValidationEvent> expectedEvents = loadExpectedEvents(errorFileLocation);
        return new SmithyTestCase(modelLocation, expectedEvents);
    }

    /**
     * Gets the expected validation events.
     *
     * @return Expected validation events.
     */
    public List<ValidationEvent> getExpectedEvents() {
        return expectedEvents;
    }

    /**
     * Gets the location of the model file.
     *
     * @return Model location.
     */
    public String getModelLocation() {
        return modelLocation;
    }

    /**
     * Creates a test case result from a test case and validated model.
     *
     * <p>The validation events encountered while validating a model are
     * compared against the expected validation events. An actual event (A) is
     * considered a match with an expected event (E) if A and E target the
     * same shape, have the same severity, the eventId of A contains the eventId
     * of E, and the message of E starts with the suppression reason or message
     * of A.
     *
     * @param validatedResult Result of creating and validating the model.
     * @return Returns the created test case result.
     */
    public Result createResult(ValidatedResult<Model> validatedResult) {
        List<ValidationEvent> actualEvents = validatedResult.getValidationEvents();
        List<ValidationEvent> unmatchedEvents = getExpectedEvents().stream()
                .filter(expectedEvent -> actualEvents.stream()
                        .noneMatch(actualEvent -> compareEvents(expectedEvent, actualEvent)))
                .collect(Collectors.toList());

        List<ValidationEvent> extraEvents = actualEvents.stream()
                .filter(actualEvent -> getExpectedEvents().stream()
                        .noneMatch(expectedEvent -> compareEvents(expectedEvent, actualEvent)))
                // Exclude suppressed events from needing to be defined as acceptable validation
                // events. However, these can still be defined as required events.
                .filter(event -> event.getSeverity() != Severity.SUPPRESSED)
                // Exclude ModelDeprecation events and deprecation warnings about traits
                // needing to be defined. Without this exclusion, existing 1.0 test cases will fail.
                .filter(event -> !isModelDeprecationEvent(event))
                .collect(Collectors.toList());

        return new SmithyTestCase.Result(getModelLocation(), unmatchedEvents, extraEvents);
    }

    private static boolean compareEvents(ValidationEvent expected, ValidationEvent actual) {
        String normalizedActualMessage = actual.getMessage();
        if (actual.getSuppressionReason().isPresent()) {
            normalizedActualMessage += " (" + actual.getSuppressionReason().get() + ")";
        }
        normalizedActualMessage = normalizeMessage(normalizedActualMessage);

        String comparedMessage = normalizeMessage(expected.getMessage());
        return expected.getSeverity() == actual.getSeverity()
               && actual.containsId(expected.getId())
               && expected.getShapeId().equals(actual.getShapeId())
               // Normalize new lines.
               && normalizedActualMessage.startsWith(comparedMessage);
    }

    // Newlines in persisted validation events are escaped.
    private static String normalizeMessage(String message) {
        return message.replace("\n", "\\n").replace("\r", "\\n");
    }

    private boolean isModelDeprecationEvent(ValidationEvent event) {
        return event.containsId(Validator.MODEL_DEPRECATION)
               // Trait vendors should be free to deprecate a trait without breaking consumers.
               || event.containsId("DeprecatedTrait")
               || event.containsId("DeprecatedShape");
    }

    private static String inferErrorFileLocation(String modelLocation) {
        int extensionPosition = modelLocation.lastIndexOf(".");
        if (extensionPosition == -1) {
            throw new IllegalArgumentException("Invalid Smithy model file: " + modelLocation);
        }
        return modelLocation.substring(0, extensionPosition) + ".errors";
    }

    private static List<ValidationEvent> loadExpectedEvents(String errorsFileLocation) {
        String contents = IoUtils.readUtf8File(errorsFileLocation);
        String fileName = Objects.requireNonNull(Paths.get(errorsFileLocation).getFileName()).toString();
        return Arrays.stream(contents.split(System.lineSeparator()))
                .filter(line -> !line.trim().isEmpty())
                .map(line -> parseValidationEvent(line, fileName))
                .collect(Collectors.toList());
    }

    static ValidationEvent parseValidationEvent(String event, String fileName) {
        Matcher matcher = EVENT_PATTERN.matcher(event);
        if (!matcher.find()) {
            throw new IllegalArgumentException(format("Invalid validation event in file `%s`, the following event did "
                    + "not match the expected regular expression `%s`: %s",
                    fileName, EVENT_PATTERN.pattern(), event));
        }

        // Construct a dummy source location since we don't validate it.
        SourceLocation location = new SourceLocation("/", 0, 0);

        ValidationEvent.Builder builder = ValidationEvent.builder()
                .severity(Severity.fromString(matcher.group("severity")).get())
                .sourceLocation(location)
                .id(matcher.group("id"))
                .message(matcher.group("message"));

        // A shape ID of "-" means no shape.
        if (!matcher.group("shape").equals("-")) {
            builder.shapeId(ShapeId.from(matcher.group("shape")));
        }

        return builder.build();
    }

    /**
     * Output of validating a model against a test case.
     */
    public static final class Result {
        private final String modelLocation;
        private final Collection<ValidationEvent> unmatchedEvents;
        private final Collection<ValidationEvent> extraEvents;

        Result(
                String modelLocation,
                Collection<ValidationEvent> unmatchedEvents,
                Collection<ValidationEvent> extraEvents
        ) {
            this.modelLocation = modelLocation;
            this.unmatchedEvents = Collections.unmodifiableCollection(new TreeSet<>(unmatchedEvents));
            this.extraEvents = Collections.unmodifiableCollection(new TreeSet<>(extraEvents));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append("=======================\n"
                           + "Model Validation Result\n"
                           + "=======================\n")
                    .append(getModelLocation())
                    .append('\n');

            if (!getUnmatchedEvents().isEmpty()) {
                builder.append("\nDid not match the following events\n"
                               + "----------------------------------\n");
                for (ValidationEvent event : getUnmatchedEvents()) {
                    builder.append(event.toString().replace("\n", "\\n")).append('\n');
                }
                builder.append('\n');
            }

            if (!getExtraEvents().isEmpty()) {
                builder.append("\nEncountered unexpected events\n"
                               + "-----------------------------\n");
                for (ValidationEvent event : getExtraEvents()) {
                    builder.append(event.toString().replace("\n", "\\n")).append("\n");
                }
                builder.append('\n');
            }

            return builder.toString();
        }

        /**
         * Checks if the result does not match expected results.
         *
         * @return True if there are extra or unmatched events.
         */
        public boolean isInvalid() {
            return !unmatchedEvents.isEmpty() || !extraEvents.isEmpty();
        }

        /**
         * @return Returns a description of where the model was stored.
         */
        public String getModelLocation() {
            return modelLocation;
        }

        /**
         * @return Returns the events that were expected but not encountered.
         */
        public Collection<ValidationEvent> getUnmatchedEvents() {
            return unmatchedEvents;
        }

        /**
         * @return Returns the events that were encountered but not expected.
         */
        public Collection<ValidationEvent> getExtraEvents() {
            return extraEvents;
        }

        /**
         * Throws an exception if the result is invalid, otherwise returns the result.
         *
         * @return Returns the result if it is ok.
         * @throws Error if the result contains invalid events.
         */
        public Result unwrap() {
            if (isInvalid()) {
                throw new Error(this);
            }

            return this;
        }
    }

    /**
     * Thrown when errors are encountered while unwrapping a test case.
     */
    public static final class Error extends RuntimeException {
        public final Result result;

        Error(Result result) {
            super(result.toString());
            this.result = result;
        }
    }
}
