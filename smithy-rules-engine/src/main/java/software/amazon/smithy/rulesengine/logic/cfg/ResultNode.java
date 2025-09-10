/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.Objects;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

/**
 * A terminal CFG node that represents a final result, either an endpoint or error.
 */
public final class ResultNode extends CfgNode {
    private final Rule result;
    private final int hash;
    private static final ResultNode TERMINAL = new ResultNode();

    public ResultNode(Rule result) {
        this.result = result;
        this.hash = result == null ? 11 : result.hashCode();
    }

    private ResultNode() {
        this(null);
    }

    /**
     * Returns a terminal node representing no match.
     *
     * @return the terminal result node
     */
    public static ResultNode terminal() {
        return TERMINAL;
    }

    /**
     * Get the underlying result.
     *
     * @return the result value.
     */
    public Rule getResult() {
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        } else {
            return Objects.equals(result, (((ResultNode) object)).result);
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "ResultNode{hash=" + hash + ", result=" + result + '}';
    }
}
