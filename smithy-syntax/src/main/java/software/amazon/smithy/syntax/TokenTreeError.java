/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.Objects;

final class TokenTreeError extends TokenTreeNode {

    private final String error;

    TokenTreeError(String error) {
        super(TreeType.ERROR);
        this.error = Objects.requireNonNull(error);
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && error.equals(((TokenTreeError) o).error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getError());
    }
}
