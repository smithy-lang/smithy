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

package software.amazon.smithy.model.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.utils.ListUtils;

/**
 * Represents the result of an operation and the {@link ValidationEvent}s
 * that occurred.
 *
 * @param <T> The type being created.
 */
public final class ValidatedResult<T> {
    private static final ValidatedResult<?> EMPTY = new ValidatedResult<>(null, ListUtils.of());

    private final List<ValidationEvent> events;
    private final T result;

    /**
     * Creates a result with a value and events.
     *
     * @param result Value to set.
     * @param events Events to set.
     */
    public ValidatedResult(T result, List<ValidationEvent> events) {
        this.result = result;
        this.events = Collections.unmodifiableList(events);
    }

    @Deprecated
    public ValidatedResult(T result, Collection<ValidationEvent> events) {
        this(result, ListUtils.copyOf(events));
    }

    /**
     * Creates a new ValidatedResult with no values and a list of
     * {@link ValidationEvent}s.
     *
     * @param events Validation events on the result.
     * @param <T> The type of value in the result.
     * @return Returns the created ValidatedResult.
     */
    public static <T> ValidatedResult<T> fromErrors(List<ValidationEvent> events) {
        return new ValidatedResult<>(null, events);
    }

    @Deprecated
    public static <T> ValidatedResult<T> fromErrors(Collection<ValidationEvent> events) {
        return fromErrors(ListUtils.copyOf(events));
    }

    /**
     * Creates a new ValidatedResult with a value and no
     * {@link ValidationEvent}s.
     *
     * @param value Result value,
     * @param <T> The type of value in the result.
     * @return Returns the created ValidatedResult.
     */
    public static <T> ValidatedResult<T> fromValue(T value) {
        return new ValidatedResult<>(value, ListUtils.of());
    }

    /**
     * Creates an empty ValidatedResult with no value and no events.
     *
     * @param <T> The type of value in the result.
     * @return Returns the created ValidatedResult.
     */
    @SuppressWarnings("unchecked")
    public static <T> ValidatedResult<T> empty() {
        return (ValidatedResult<T>) EMPTY;
    }

    /**
     * Get the list of {@link ValidationEvent}s associated with the result.
     *
     * @return Returns the validation events.
     */
    public List<ValidationEvent> getValidationEvents() {
        return events;
    }

    /**
     * Get validation events of a particular severity.
     *
     * @param severity Severity to get.
     * @return Returns a list of events with the given severity.
     */
    public List<ValidationEvent> getValidationEvents(Severity severity) {
        return getValidationEvents().stream()
                .filter(event -> event.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    /**
     * Get the optional result.
     *
     * @return Returns the optional result.
     */
    public Optional<T> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * Get the result, but throw if there are any ERROR events or if the
     * result is empty.
     *
     * @return Returns the result.
     * @throws ValidatedResultException if there are any ERROR events.
     * @throws IllegalStateException if there is no result.
     */
    public T unwrap() {
        return validate().orElseThrow(() -> new IllegalStateException("Validated result contains no value"));
    }

    /**
     * Get the optional result, and throw if there are any ERROR events.
     *
     * @return Returns the optional result.
     * @throws ValidatedResultException if there are any ERROR events.
     */
    public Optional<T> validate() {
        if (!isBroken()) {
            return getResult();
        } else {
            throw new ValidatedResultException(events);
        }
    }

    /**
     * Checks if the result has any error or danger events..
     *
     * @return Returns true if there are errors or unsuppressed dangers.
     */
    public boolean isBroken() {
        for (ValidationEvent event : events) {
            if (event.getSeverity() == Severity.ERROR || event.getSeverity() == Severity.DANGER) {
                return true;
            }
        }
        return false;
    }
}
