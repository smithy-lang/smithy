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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.eval.type.RecordType;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;

public final class RecordValue extends Value {
    private final Map<Identifier, Value> value;

    RecordValue(Map<Identifier, Value> value) {
        super(SourceLocation.none());
        this.value = value;
    }

    @Override
    public Type getType() {
        Map<Identifier, Type> type = new HashMap<>();
        for (Map.Entry<Identifier, Value> valueEntry : value.entrySet()) {
            type.put(valueEntry.getKey(), valueEntry.getValue().getType());
        }
        return new RecordType(type);
    }

    @Override
    public RecordValue expectRecordValue() {
        return this;
    }

    public Value get(String key) {
        return get(Identifier.of(key));
    }

    public Value get(Identifier key) {
        return this.value.get(key);
    }

    public void forEach(BiConsumer<Identifier, Value> fn) {
        value.forEach(fn);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        value.forEach((k, v) -> builder.withMember(k.getName(), v));
        return builder.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordValue record = (RecordValue) o;
        return Objects.equals(value, record.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public Map<Identifier, Value> getValue() {
        return this.value;
    }
}
