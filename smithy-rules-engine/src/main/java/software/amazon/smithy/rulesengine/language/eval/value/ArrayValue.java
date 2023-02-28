/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.eval.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.eval.type.Type;

public final class ArrayValue extends Value {
    private final List<Value> values;

    ArrayValue(List<Value> values) {
        super(SourceLocation.none());
        this.values = values;
    }

    public List<Value> getValues() {
        return values;
    }

    @Override
    public Type getType() {
        if (values.isEmpty()) {
            return Type.arrayType(Type.emptyType());
        } else {
            Type first = values.get(0).getType();
            for (Value value : values) {
                if (value.getType() != first) {
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

    public Value get(int idx) {
        if (this.values.size() > idx) {
            return this.values.get(idx);
        } else {
            return Value.emptyValue();
        }
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
    public int hashCode() {
        return Objects.hash(values);
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        List<String> valueStrings = new ArrayList<>();
        for (Value value : values) {
            valueStrings.add(value.toString());
        }
        sb.append(String.join(", ", valueStrings));
        sb.append("]");
        return sb.toString();
    }
}
