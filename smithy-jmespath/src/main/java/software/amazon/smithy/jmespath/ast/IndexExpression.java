/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Gets a specific element by zero-based index. Use -1 to get the
 * last element in an array.
 */
public final class IndexExpression extends JmespathExpression {

    private final int index;

    public IndexExpression(int index) {
        this(index, 1, 1);
    }

    public IndexExpression(int index, int line, int column) {
        super(line, column);
        this.index = index;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitIndex(this);
    }

    /**
     * Gets the index to retrieve.
     *
     * @return Returns the index.
     */
    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof IndexExpression)) {
            return false;
        }
        IndexExpression other = (IndexExpression) o;
        return getIndex() == other.getIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIndex());
    }

    @Override
    public String toString() {
        return "IndexExpression{index=" + index + '}';
    }
}
