/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.value;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;

/**
 * A record value, containing a map of identifiers to other values.
 */
public final class RecordValue extends Value {
    private final Map<Identifier, Value> value;

    RecordValue(Map<Identifier, Value> value) {
        super(SourceLocation.none());
        this.value = value;
    }

    /**
     * Gets the map of identifiers to other values.
     *
     * @return the map of identifiers to other values.
     */
    public Map<Identifier, Value> getValue() {
        return this.value;
    }

    /**
     * Gets the value in the record for the provided key.
     *
     * @param key the key to retrieve a value for.
     * @return the value for the provided key, or null if the key is not present.
     */
    public Value get(String key) {
        return get(Identifier.of(key));
    }

    /**
     * Gets the value in the record for the provided key.
     *
     * @param key the key to retrieve a value for.
     * @return the value for the provided key, or null if the key is not present.
     */
    public Value get(Identifier key) {
        return this.value.get(key);
    }

    @Override
    public Type getType() {
        Map<Identifier, Type> type = new HashMap<>();
        for (Map.Entry<Identifier, Value> valueEntry : value.entrySet()) {
            type.put(valueEntry.getKey(), valueEntry.getValue().getType());
        }
        return Type.recordType(type);
    }

    @Override
    public RecordValue expectRecordValue() {
        return this;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        for (Map.Entry<Identifier, Value> valueEntry : value.entrySet()) {
            builder.withMember(valueEntry.getKey().getName(), valueEntry.getValue());
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
        RecordValue record = (RecordValue) o;
        return Objects.equals(value, record.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
