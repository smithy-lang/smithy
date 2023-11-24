/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;

/**
 * The "record" type, a map of identifiers to other types.
 */
public final class RecordType extends AbstractType {
    private final Map<Identifier, Type> shape;

    RecordType(Map<Identifier, Type> shape) {
        this.shape = new LinkedHashMap<>(shape);
    }

    /**
     * Gets the type for the specified identifier.
     *
     * @param name the identifier to get the type of.
     * @return the type of the specified identifier.
     */
    public Optional<Type> get(Identifier name) {
        if (shape.containsKey(name)) {
            return Optional.of(shape.get(name));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the map of identifiers to their types.
     *
     * @return the map of identifiers to their types.
     */
    public Map<Identifier, Type> getShape() {
        return shape;
    }

    @Override
    public RecordType expectRecordType(String message) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        RecordType that = (RecordType) obj;
        return Objects.equals(this.shape, that.shape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape);
    }

    @Override
    public String toString() {
        return shape.toString();
    }
}
