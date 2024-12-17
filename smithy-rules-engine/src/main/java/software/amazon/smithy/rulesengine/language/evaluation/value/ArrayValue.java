/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;

/**
 * An array of values of the same type.
 */
public final class ArrayValue extends Value {
    private final List<Value> values;

    ArrayValue(List<Value> values) {
        super(SourceLocation.none());
        this.values = values;
    }

    /**
     * Gets all the values in the array.
     *
     * @return the values in the array.
     */
    public List<Value> getValues() {
        return values;
    }

    /**
     * Gets the value at the index, or an empty value if none is present.
     *
     * @param index the index to get the value of.
     * @return the value at the index, or an empty value if not present.
     */
    public Value get(int index) {
        if (values.size() > index) {
            return values.get(index);
        } else {
            return Value.emptyValue();
        }
    }

    @Override
    public Type getType() {
        if (values.isEmpty()) {
            return Type.arrayType(Type.emptyType());
        } else {
            Type first = values.get(0).getType();
            for (Value value : values) {
                if (!value.getType().isA(first)) {
                    throw new SourceException("An array cannot contain different types", this);
                }
            }
            return Type.arrayType(first);
        }
    }

    @Override
    public ArrayValue expectArrayValue() {
        return this;
    }

    @Override
    public Node toNode() {
        ArrayNode.Builder builder = ArrayNode.builder();
        for (Value value : values) {
            builder.withValue(value.toNode());
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArrayValue array = (ArrayValue) o;
        return values.equals(array.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        List<String> valueStrings = new ArrayList<>();
        for (Value value : values) {
            valueStrings.add(value.toString());
        }
        return "[" + String.join(", ", valueStrings) + "]";
    }
}
