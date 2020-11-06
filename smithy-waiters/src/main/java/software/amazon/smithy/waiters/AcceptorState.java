/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.waiters;

import java.util.Locale;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * The transition state of a waiter.
 */
public enum AcceptorState implements ToNode {

    /** Transition to a final success state. */
    SUCCESS,

    /** Transition to a final failure state. */
    FAILURE,

    /** Transition to an intermediate retry state. */
    RETRY;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }

    /**
     * Create an AcceptorState from a Node.
     *
     * @param node Node to create the AcceptorState from.
     * @return Returns the created AcceptorState.
     * @throws ExpectationNotMetException when given an invalid Node.
     */
    public static AcceptorState fromNode(Node node) {
        StringNode value = node.expectStringNode();
        String constValue = value.expectOneOf("success", "failure", "retry").toUpperCase(Locale.ENGLISH);
        return AcceptorState.valueOf(constValue);
    }
}
