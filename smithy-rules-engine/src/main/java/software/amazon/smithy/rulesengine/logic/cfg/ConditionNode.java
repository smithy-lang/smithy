/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.Objects;
import software.amazon.smithy.rulesengine.logic.ConditionReference;

/**
 * A CFG node that evaluates a condition and branches based on the result.
 */
public final class ConditionNode extends CfgNode {

    private final ConditionReference condition;
    private final CfgNode trueBranch;
    private final CfgNode falseBranch;
    private final int hash;

    /**
     * Creates a new condition node.
     *
     * @param condition condition reference (can be negated)
     * @param trueBranch node to evaluate if the condition is true
     * @param falseBranch node to evaluate if the condition is false
     */
    public ConditionNode(ConditionReference condition, CfgNode trueBranch, CfgNode falseBranch) {
        this.condition = Objects.requireNonNull(condition);
        this.trueBranch = Objects.requireNonNull(trueBranch, "trueBranch must not be null");
        this.falseBranch = Objects.requireNonNull(falseBranch, "falseBranch must not be null");
        this.hash = Objects.hash(condition, trueBranch, falseBranch);
    }

    /**
     * Returns the condition reference for this node.
     *
     * @return the condition reference
     */
    public ConditionReference getCondition() {
        return condition;
    }

    /**
     * Returns the node to evaluate if the condition is true.
     *
     * @return the true branch node
     */
    public CfgNode getTrueBranch() {
        return trueBranch;
    }

    /**
     * Returns the node to evaluate if the condition is false.
     *
     * @return the false branch node
     */
    public CfgNode getFalseBranch() {
        return falseBranch;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ConditionNode o = (ConditionNode) object;
        return condition.equals(o.condition) && trueBranch.equals(o.trueBranch) && falseBranch.equals(o.falseBranch);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "ConditionNode{condition=" + condition
                + ", trueBranch=" + System.identityHashCode(trueBranch)
                + ", falseBranch=" + System.identityHashCode(falseBranch) + '}';
    }
}
