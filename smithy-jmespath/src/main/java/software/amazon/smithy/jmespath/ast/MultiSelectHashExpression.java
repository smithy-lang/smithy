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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Creates an object using key-value pairs.
 *
 * @see <a href="https://jmespath.org/specification.html#multiselect-hash">MultiSelect Hash</a>
 */
public final class MultiSelectHashExpression extends JmespathExpression {

    private final Map<String, JmespathExpression> expressions;

    public MultiSelectHashExpression(Map<String, JmespathExpression> expressions) {
        this(expressions, 1, 1);
    }

    public MultiSelectHashExpression(Map<String, JmespathExpression> expressions, int line, int column) {
        super(line, column);
        this.expressions = Collections.unmodifiableMap(expressions);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitMultiSelectHash(this);
    }

    /**
     * Gets the map of key-value pairs to add to the created object.
     *
     * @return Returns the map of key names to expressions.
     */
    public Map<String, JmespathExpression> getExpressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof MultiSelectHashExpression)) {
            return false;
        }
        MultiSelectHashExpression that = (MultiSelectHashExpression) o;
        return getExpressions().equals(that.getExpressions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExpressions());
    }

    @Override
    public String toString() {
        return "MultiSelectHashExpression{expressions=" + expressions + '}';
    }
}
