/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

/**
 * A value that can be converted to a {@link Node}.
 */
public interface ToNode {

    /**
     * Converts a value to a {@link Node}.
     *
     * @return Returns the creates Node.
     */
    Node toNode();
}
