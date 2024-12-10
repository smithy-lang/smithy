/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;

/**
 * Thrown by {@code Node} methods that expect a node to be of a particular
 * type or to have a particular member.
 */
public class ExpectationNotMetException extends SourceException {
    public ExpectationNotMetException(String message, FromSourceLocation fromSourceLocation) {
        super(message, fromSourceLocation);
    }
}
