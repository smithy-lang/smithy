/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when {@link ValidatedResult#validate} is called and the result contains
 * ERROR events.
 */
public class ValidatedResultException extends RuntimeException {
    private final List<ValidationEvent> events;

    public ValidatedResultException(List<ValidationEvent> events) {
        super("Result contained ERROR severity validation events: \n"
                + events.stream().map(ValidationEvent::toString).sorted().collect(Collectors.joining("\n")));
        this.events = events;
    }

    public List<ValidationEvent> getValidationEvents() {
        return events;
    }
}
