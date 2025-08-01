/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * A reference to a condition and whether it is negated.
 */
public final class ConditionReference {

    private final Condition condition;
    private final boolean negated;

    public ConditionReference(Condition condition, boolean negated) {
        this.condition = condition;
        this.negated = negated;
    }

    /**
     * Returns true if this condition is negated (e.g., wrapped in not).
     *
     * @return true if negated.
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * Create a negated version of this reference.
     *
     * @return returns the negated reference.
     */
    public ConditionReference negate() {
        return new ConditionReference(condition, !negated);
    }

    /**
     * Get the underlying condition.
     *
     * @return condition.
     */
    public Condition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return (negated ? "!" : "") + condition.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ConditionReference that = (ConditionReference) object;
        return negated == that.negated && condition.equals(that.condition);
    }

    @Override
    public int hashCode() {
        return condition.hashCode() ^ (negated ? 0x80000000 : 0);
    }
}
