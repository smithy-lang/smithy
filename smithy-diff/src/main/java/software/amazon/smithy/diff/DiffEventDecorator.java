/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Transforms diff validation events using the context of both models being compared, for example downgrading
 * the severity of a breaking change on a shape.
 */
@FunctionalInterface
public interface DiffEventDecorator {
    /**
     * Takes a diff event and potentially transforms it, returning the same event if this decorator does not
     * apply.
     *
     * @param differences The differences between the old and new models being evaluated.
     * @param event The event to decorate.
     * @return Returns the decorated event or the original one if no decoration took place.
     */
    ValidationEvent decorate(Differences differences, ValidationEvent event);
}
