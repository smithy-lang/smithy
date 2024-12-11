/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

/**
 * Formats {@code ValidationEvent}s.
 */
public interface ValidationEventFormatter {
    /**
     * Converts the event to a string.
     *
     * @param event Event to write as a string.
     * @return Returns the event as a formatted string.
     */
    String format(ValidationEvent event);
}
