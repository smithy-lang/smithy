/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

/**
 * Node visitor interface.
 *
 * @param <R> Return type of the visitor.
 */
public interface NodeVisitor<R> {

    /**
     * Visits an array node.
     *
     * @param node Node to visit.
     * @return Return value of the visitor.
     */
    R arrayNode(ArrayNode node);

    /**
     * Visits a boolean node.
     *
     * @param node Node to visit.
     * @return Return value of the visitor.
     */
    R booleanNode(BooleanNode node);

    /**
     * Visits a null node.
     *
     * @param node Node to visit.
     * @return Return value of the visitor.
     */
    R nullNode(NullNode node);

    /**
     * Visits a number node.
     *
     * @param node Node to visit.
     * @return Return value of the visitor.
     */
    R numberNode(NumberNode node);

    /**
     * Visits an object node.
     *
     * @param node Node to visit.
     * @return Return value of the visitor.
     */
    R objectNode(ObjectNode node);

    /**
     * Visits a string node.
     *
     * @param node Node to visit.
     * @return Return value of the visitor.
     */
    R stringNode(StringNode node);

    /**
     * Creates {@link NodeVisitor} that return a default value when necessary
     * when visiting nodes.
     *
     * @param <R> Return type.
     */
    abstract class Default<R> implements NodeVisitor<R> {

        /**
         * Returns a default value for any unhandled node.
         *
         * @param node Node that is being visited.
         * @return Return value.
         */
        protected abstract R getDefault(Node node);

        @Override
        public R arrayNode(ArrayNode node) {
            return getDefault(node);
        }

        @Override
        public R booleanNode(BooleanNode node) {
            return getDefault(node);
        }

        @Override
        public R nullNode(NullNode node) {
            return getDefault(node);
        }

        @Override
        public R numberNode(NumberNode node) {
            return getDefault(node);
        }

        @Override
        public R objectNode(ObjectNode node) {
            return getDefault(node);
        }

        @Override
        public R stringNode(StringNode node) {
            return getDefault(node);
        }
    }
}
