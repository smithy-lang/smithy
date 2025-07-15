/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * A reference to a condition and whether it is negated.
 */
public final class ConditionReference implements ConditionInfo {

    private final ConditionInfo delegate;
    private final boolean negated;

    public ConditionReference(ConditionInfo delegate, boolean negated) {
        this.delegate = delegate;
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
        return new ConditionReference(delegate, !negated);
    }

    @Override
    public Condition getCondition() {
        return delegate.getCondition();
    }

    @Override
    public int getComplexity() {
        return delegate.getComplexity();
    }

    @Override
    public Set<String> getReferences() {
        return delegate.getReferences();
    }

    @Override
    public String getReturnVariable() {
        return delegate.getReturnVariable();
    }

    @Override
    public String toString() {
        return (negated ? "!" : "") + delegate.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ConditionReference that = (ConditionReference) object;
        return negated == that.negated && delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode() ^ (negated ? 0x80000000 : 0);
    }
}
