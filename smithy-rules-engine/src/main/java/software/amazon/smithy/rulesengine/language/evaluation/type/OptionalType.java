/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.Objects;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;

/**
 * The "optional" type, a container for a type that may or may not be present.
 */
public final class OptionalType extends AbstractType {
    private final Type inner;

    OptionalType(Type inner) {
        this.inner = inner;
    }

    @Override
    public boolean isA(Type type) {
        if (!(type instanceof OptionalType)) {
            return false;
        }
        OptionalType other = ((OptionalType) type);
        return other.inner.isA(this.inner);
    }

    @Override
    public Type provenTruthy() {
        return inner;
    }

    /**
     * Gets the optional's contained value.
     *
     * @return the optional's value.
     */
    public Type inner() {
        return inner;
    }

    @Override
    public StringType expectStringType() throws InnerParseError {
        throw new InnerParseError(String.format("Expected string but found %s. hint: use `assign` in a condition "
                + "or `isSet` to prove that this value is non-null", this));
    }

    @Override
    public BooleanType expectBooleanType() throws InnerParseError {
        throw new InnerParseError(String.format("Expected boolean but found %s. hint: use `isSet` to convert "
                + "OptionalType[BooleanType] to bool", this));
    }

    @Override
    public OptionalType expectOptionalType() {
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
        OptionalType that = (OptionalType) obj;
        return Objects.equals(this.inner, that.inner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner);
    }

    @Override
    public String toString() {
        return String.format("OptionalType[%s]", inner);
    }
}
