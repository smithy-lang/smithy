/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.List;

/**
 * Validation event decorators take validation events and transform them by adding more contextual information,
 * usually adding a hint to let the user know what can it be done to solve the issue. This might add context specific
 * information that is not relevant for all cases such as links to internal knowledge sites or explicit instructions
 * relevant only to the context where Smithy is being used.
 */
public interface ValidationEventDecorator {
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
            return new ValidationEventDecorator() {
                @Override
                public boolean canDecorate(ValidationEvent ev) {
                    return false;
                }

                @Override
                public ValidationEvent decorate(ValidationEvent ev) {
                    return ev;
                }
            };
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
