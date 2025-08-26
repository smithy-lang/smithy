/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.value;

import java.util.Objects;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;

/**
 * A boolean value of true or false.
 */
public final class BooleanValue extends Value {

    static final BooleanValue TRUE = new BooleanValue(true);
    static final BooleanValue FALSE = new BooleanValue(false);

    private final boolean value;

    static BooleanValue create(boolean v) {
        return v ? TRUE : FALSE;
    }

    private BooleanValue(boolean value) {
        super(SourceLocation.none());
        this.value = value;
    }

    @Override
    public boolean isTruthy() {
        return value;
    }

    /**
     * Gets the true or false value of this boolean.
     *
     * @return the value true or false.
     */
    public boolean getValue() {
        return value;
    }

    @Override
    public Type getType() {
        return Type.booleanType();
    }

    @Override
    public BooleanValue expectBooleanValue() {
        return this;
    }

    @Override
    public Node toNode() {
        return BooleanNode.from(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BooleanValue bool = (BooleanValue) o;

        return value == bool.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public Object toObject() {
        return value;
    }
}
