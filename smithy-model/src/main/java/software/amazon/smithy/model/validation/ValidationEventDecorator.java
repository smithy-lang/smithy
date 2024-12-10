/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import java.util.List;

/**
 * Validation event decorators take validation events and transform them by adding more contextual information,
 * usually adding a hint to let the user know what can it be done to solve the issue. This might add context specific
 * information that is not relevant for all cases such as links to internal knowledge sites or explicit instructions
 * relevant only to the context where Smithy is being used.
 */
public interface ValidationEventDecorator {

    /** A decorator that does nothing. */
    ValidationEventDecorator IDENTITY = new ValidationEventDecorator() {
        @Override
        public boolean canDecorate(ValidationEvent ev) {
            return false;
        }

        @Override
        public ValidationEvent decorate(ValidationEvent ev) {
            return ev;
        }
    };

    /**
     * Returns true if this decorator knows how to decorate this event, usually by looking at the event id.
     *
     * @param ev The event to test against
     * @return true if this decorator knows how to decorate this event
     */
    boolean canDecorate(ValidationEvent ev);

    /**
     * Takes an event and potentially updates it to decorate it. Returns the same event if this decorators does not know
     * how to handle the event.
     *
     * @return The decorated event or the original one if no decoration took place.
     */
    ValidationEvent decorate(ValidationEvent ev);

    /**
     * Creates a decorator composed of one or more decorators.
     *
     * @param decorators Decorators to compose.
     * @return Returns the composed decorator.
     */
    static ValidationEventDecorator compose(List<ValidationEventDecorator> decorators) {
        if (decorators.isEmpty()) {
            return IDENTITY;
        } else {
            return new ValidationEventDecorator() {
                @Override
                public boolean canDecorate(ValidationEvent ev) {
                    for (ValidationEventDecorator decorator : decorators) {
                        if (decorator.canDecorate(ev)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public ValidationEvent decorate(ValidationEvent ev) {
                    ValidationEvent decoratedEvent = ev;
                    for (ValidationEventDecorator decorator : decorators) {
                        if (decorator.canDecorate(ev)) {
                            decoratedEvent = decorator.decorate(decoratedEvent);
                        }
                    }
                    return decoratedEvent;
                }
            };
        }
    }
}
