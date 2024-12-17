/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.value;

import java.util.Objects;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;

/**
 * A string value.
 */
public final class StringValue extends Value {
    private final String value;

    StringValue(String value) {
        super(SourceLocation.none());
        this.value = value;
    }

    /**
     * Gets the value of the string.
     *
     * @return the value of the string.
     */
    public String getValue() {
        return value;
    }

    @Override
    public Type getType() {
        return Type.stringType();
    }

    @Override
    public StringValue expectStringValue() {
        return this;
    }

    @Override
    public Node toNode() {
        return StringNode.from(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringValue other = (StringValue) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
