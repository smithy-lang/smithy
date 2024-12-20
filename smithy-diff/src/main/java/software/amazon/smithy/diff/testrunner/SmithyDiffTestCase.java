/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.testrunner;

import static java.lang.String.format;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.IoUtils;

/**
 * Runs a single test case by loading corresponding models `a` and `b` and
 * ensuring the resulting events match the diff events stored in a `-----`
 * separated file.
 */
public final class SmithyDiffTestCase {
    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "^\\[(?<severity>SUPPRESSED|NOTE|WARNING|DANGER|ERROR)] "
                    + "(?<shape>[^ ]+): "
                    + "?(?<message>.*) "
                    + "\\| "
                    + "(?<id>[^)]+)",
            Pattern.DOTALL);

    private final Path path;
    private final String name;
    private final List<ValidationEvent> expectedEvents;

    /**
     * @param path Parent path of where the model and event files are stored.
     * @param name Name of the test case
     * @param expectedEvents The expected diff events to encounter.
     */
    public SmithyDiffTestCase(
            Path path,
            String name,
            List<ValidationEvent> expectedEvents
    ) {
        this.path = Objects.requireNonNull(path);
        this.name = Objects.requireNonNull(name);
        this.expectedEvents = Collections.unmodifiableList(expectedEvents);
    }

    /**
     * Creates a test case from a test case path and name.
     *
     * <p>The models and events file are expected to be stored in the same
     * directory as the model and events file are assumed to be named the same
     * barring the file extensions: `.a.(json|smithy)`, `.b.(json|smithy)`,
     * `.events`.
     *
     * <p>The accompanying events file is a `-----` separated list of event
     * strings, where each event is defined in the following format:
     * {@code [SEVERITY] shapeId message | EventId filename:line:column}.
     * A shapeId of "-" means that a specific shape is not targeted.
     *
     * @param path Parent path of where the model and event files are stored.
     * @param name Name of the test case
     * @return Returns the created test case.
     */
    public static SmithyDiffTestCase from(Path path, String name) {
        List<ValidationEvent> expectedEvents = loadExpectedEvents(path, name);
        return new SmithyDiffTestCase(path, name, expectedEvents);
    }

    /**
     * Gets the parent path of the test case.
     *
     * @return parent path of the test case.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the name of the test case.
     *
     * @return name of the test case.
     */
    public String getName() {
        return name;
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
     * Creates a test case result from a list of model diff events.
     *
     * <p>The diff events encountered are compared against the expected
     * validation events. An actual event (A) is considered a match with an
     * expected event (E) if A and E target the same shape, have the same
     * severity, the eventId of A contains the eventId of E, and the message
     * of E starts with the suppression reason or message of A.
     *
     * @param actualEvents List of actual diff events.
     * @return Returns the created test case result.
     */
    public Result createResult(List<ValidationEvent> actualEvents) {
        List<ValidationEvent> unmatchedEvents = expectedEvents.stream()
                .filter(expectedEvent -> actualEvents.stream()
                        .noneMatch(actualEvent -> compareEvents(expectedEvent, actualEvent)))
                .collect(Collectors.toList());

        List<ValidationEvent> extraEvents = actualEvents.stream()
                .filter(actualEvent -> expectedEvents.stream()
                        .noneMatch(expectedEvent -> compareEvents(expectedEvent, actualEvent)))
                // Exclude suppressed events from needing to be defined as acceptable events.
                // However, these can still be defined as required events.
                .filter(event -> event.getSeverity() != Severity.SUPPRESSED)
                .collect(Collectors.toList());

        return new SmithyDiffTestCase.Result(name, unmatchedEvents, extraEvents);
    }

    private static boolean compareEvents(ValidationEvent expected, ValidationEvent actual) {
        String normalizedActualMessage = normalizeMessage(actual.getMessage());
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

    private static List<ValidationEvent> loadExpectedEvents(Path path, String name) {
        String fileName = path.resolve(name + SmithyDiffTestSuite.EVENTS).toString();
        String contents = IoUtils.readUtf8File(fileName);
        return Arrays.stream(contents.split("-----"))
                .map(chunk -> chunk.trim())
                .filter(chunk -> !chunk.isEmpty())
                .map(chunk -> parseValidationEvent(chunk, fileName))
                .collect(Collectors.toList());
    }

    static ValidationEvent parseValidationEvent(String event, String fileName) {
        Matcher matcher = EVENT_PATTERN.matcher(event);
        if (!matcher.find()) {
            throw new IllegalArgumentException(format("Invalid validation event in file `%s`, the following event did "
                    + "not match the expected regular expression `%s`: %s",
                    fileName,
                    EVENT_PATTERN.pattern(),
                    event));
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
     * Output of diffing a model against a test case.
     */
    public static final class Result {
        private final String name;
        private final Collection<ValidationEvent> unmatchedEvents;
        private final Collection<ValidationEvent> extraEvents;

        Result(
                String name,
                Collection<ValidationEvent> unmatchedEvents,
                Collection<ValidationEvent> extraEvents
        ) {
            this.name = name;
            this.unmatchedEvents = Collections.unmodifiableCollection(new TreeSet<>(unmatchedEvents));
            this.extraEvents = Collections.unmodifiableCollection(new TreeSet<>(extraEvents));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder
                    .append("============================\n"
                            + "Model Diff Validation Result\n"
                            + "============================\n")
                    .append(name)
                    .append('\n');

            if (!unmatchedEvents.isEmpty()) {
                builder.append("\nDid not match the following events\n"
                        + "----------------------------------\n");
                for (ValidationEvent event : unmatchedEvents) {
                    builder.append(event.toString()).append("\n\n");
                }
            }

            if (!extraEvents.isEmpty()) {
                builder.append("\nEncountered unexpected events\n"
                        + "-----------------------------\n");
                for (ValidationEvent event : extraEvents) {
                    builder.append(event.toString()).append("\n\n");
                }
            }

            return builder.toString();
        }

        /**
         * @return Returns the name of the test case.
         */
        public String getName() {
            return name;
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
         * Checks if the result does not match expected results.
         *
         * @return True if there are extra or unmatched events.
         */
        public boolean isInvalid() {
            return !unmatchedEvents.isEmpty() || !extraEvents.isEmpty();
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
