/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import software.amazon.smithy.rulesengine.logic.ConditionEvaluator;

/**
 * Binary Decision Diagram (BDD) with complement edges for efficient rule evaluation.
 *
 * <p>This class represents a pure BDD structure without any knowledge of the specific
 * conditions or results it represents. The interpretation of condition indices and
 * result indices is left to the caller.
 *
 * <p><b>Reference Encoding:</b>
 * <ul>
 *   <li>{@code 0}: Invalid/unused reference (never appears in valid BDDs)</li>
 *   <li>{@code 1}: TRUE terminal</li>
 *   <li>{@code -1}: FALSE terminal</li>
 *   <li>{@code 2, 3, ...}: Node references (points to nodes array at index ref-1)</li>
 *   <li>{@code -2, -3, ...}: Complement node references (logical NOT)</li>
 *   <li>{@code 100_000_000+}: Result terminals (100_000_000 + resultIndex)</li>
 * </ul>
 */
public final class Bdd {
    /**
     * Result reference encoding offset.
     */
    public static final int RESULT_OFFSET = 100_000_000;

    private final int[] nodes; // Flat array: [var0, high0, low0, var1, high1, low1, ...]
    private final int rootRef;
    private final int conditionCount;
    private final int resultCount;
    private final int nodeCount;

    /**
     * Creates a BDD by streaming nodes directly into the structure.
     *
     * @param rootRef the root reference
     * @param conditionCount the number of conditions
     * @param resultCount the number of results
     * @param nodeCount the exact number of nodes
     * @param nodeHandler a handler that will provide nodes via a consumer
     */
    public Bdd(int rootRef, int conditionCount, int resultCount, int nodeCount, Consumer<BddNodeConsumer> nodeHandler) {
        validateCounts(conditionCount, resultCount, nodeCount);
        validateRootReference(rootRef, nodeCount);

        this.rootRef = rootRef;
        this.conditionCount = conditionCount;
        this.resultCount = resultCount;
        this.nodeCount = nodeCount;

        InputNodeConsumer consumer = new InputNodeConsumer(nodeCount);
        nodeHandler.accept(consumer);
        this.nodes = consumer.nodes;

        if (consumer.index != nodeCount * 3) {
            throw new IllegalStateException("Expected " + nodeCount + " nodes, but got " + (consumer.index / 3));
        }
    }

    /**
     * Package-private constructor for direct array initialization (used by BddTrait).
     */
    Bdd(int rootRef, int conditionCount, int resultCount, int nodeCount, int[] nodes) {
        validateCounts(conditionCount, resultCount, nodeCount);
        validateRootReference(rootRef, nodeCount);

        if (nodes.length != nodeCount * 3) {
            throw new IllegalArgumentException("Nodes array length must be nodeCount * 3");
        }

        this.rootRef = rootRef;
        this.conditionCount = conditionCount;
        this.resultCount = resultCount;
        this.nodeCount = nodeCount;
        this.nodes = nodes;
    }

    private static void validateCounts(int conditionCount, int resultCount, int nodeCount) {
        if (conditionCount < 0) {
            throw new IllegalArgumentException("Condition count cannot be negative: " + conditionCount);
        } else if (resultCount < 0) {
            throw new IllegalArgumentException("Result count cannot be negative: " + resultCount);
        } else if (nodeCount < 0) {
            throw new IllegalArgumentException("Node count cannot be negative: " + nodeCount);
        }
    }

    private static void validateRootReference(int rootRef, int nodeCount) {
        if (isComplemented(rootRef) && !isTerminal(rootRef)) {
            throw new IllegalArgumentException("Root reference cannot be complemented: " + rootRef);
        } else if (isNodeReference(rootRef)) {
            int idx = Math.abs(rootRef) - 1;
            if (idx >= nodeCount) {
                throw new IllegalArgumentException("Root points to invalid BDD node: " + idx +
                        " (node count: " + nodeCount + ")");
            }
        }
    }

    private static final class InputNodeConsumer implements BddNodeConsumer {
        private int index = 0;
        private final int[] nodes;

        private InputNodeConsumer(int nodeCount) {
            this.nodes = new int[nodeCount * 3];
        }

        @Override
        public void accept(int var, int high, int low) {
            nodes[index++] = var;
            nodes[index++] = high;
            nodes[index++] = low;
        }
    }

    /**
     * Gets the number of conditions.
     *
     * @return condition count
     */
    public int getConditionCount() {
        return conditionCount;
    }

    /**
     * Gets the number of results.
     *
     * @return result count
     */
    public int getResultCount() {
        return resultCount;
    }

    /**
     * Gets the number of nodes in the BDD.
     *
     * @return the node count
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Gets the root node reference.
     *
     * @return root reference
     */
    public int getRootRef() {
        return rootRef;
    }

    /**
     * Gets the variable index for a node.
     *
     * @param nodeIndex the node index (0-based)
     * @return the variable index
     */
    public int getVariable(int nodeIndex) {
        validateRange(nodeIndex);
        return nodes[nodeIndex * 3];
    }

    private void validateRange(int index) {
        if (index < 0 || index >= nodeCount) {
            throw new IndexOutOfBoundsException("Node index out of bounds: " + index + " (size: " + nodeCount + ")");
        }
    }

    /**
     * Gets the high (true) reference for a node.
     *
     * @param nodeIndex the node index (0-based)
     * @return the high reference
     */
    public int getHigh(int nodeIndex) {
        validateRange(nodeIndex);
        return nodes[nodeIndex * 3 + 1];
    }

    /**
     * Gets the low (false) reference for a node.
     *
     * @param nodeIndex the node index (0-based)
     * @return the low reference
     */
    public int getLow(int nodeIndex) {
        validateRange(nodeIndex);
        return nodes[nodeIndex * 3 + 2];
    }

    /**
     * Write all nodes to the consumer.
     *
     * @param consumer the consumer to receive the integers
     */
    public void getNodes(BddNodeConsumer consumer) {
        for (int i = 0; i < nodeCount; i++) {
            int base = i * 3;
            consumer.accept(nodes[base], nodes[base + 1], nodes[base + 2]);
        }
    }

    /**
     * Evaluates the BDD using the provided condition evaluator.
     *
     * @param ev the condition evaluator
     * @return the result index, or -1 for no match
     */
    public int evaluate(ConditionEvaluator ev) {
        int ref = rootRef;
        int[] n = this.nodes;

        while (isNodeReference(ref)) {
            int idx = ref > 0 ? ref - 1 : -ref - 1;
            int base = idx * 3;
            // test ^ complement, pick hi or lo
            ref = (ev.test(n[base]) ^ (ref < 0)) ? n[base + 1] : n[base + 2];
        }

        return isTerminal(ref) ? -1 : ref - RESULT_OFFSET;
    }

    /**
     * Checks if a reference points to a node (not a terminal or result).
     *
     * @param ref the reference to check
     * @return true if this is a node reference
     */
    public static boolean isNodeReference(int ref) {
        return (ref > 1 && ref < RESULT_OFFSET) || (ref < -1 && ref > -RESULT_OFFSET);
    }

    /**
     * Checks if a reference points to a result.
     *
     * @param ref the reference to check
     * @return true if this is a result reference
     */
    public static boolean isResultReference(int ref) {
        return ref >= RESULT_OFFSET;
    }

    /**
     * Checks if a reference is a terminal (TRUE or FALSE).
     *
     * @param ref the reference to check
     * @return true if this is a terminal reference
     */
    public static boolean isTerminal(int ref) {
        return ref == 1 || ref == -1;
    }

    /**
     * Checks if a reference is complemented (negative).
     *
     * @param ref the reference to check
     * @return true if the reference is complemented
     */
    public static boolean isComplemented(int ref) {
        return ref < 0 && ref != -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Bdd)) {
            return false;
        }

        Bdd other = (Bdd) obj;
        if (rootRef != other.rootRef
                || conditionCount != other.conditionCount
                || resultCount != other.resultCount
                || nodeCount != other.nodeCount) {
            return false;
        }

        return Arrays.equals(nodes, other.nodes);
    }

    @Override
    public int hashCode() {
        return 31 * rootRef + nodeCount + Arrays.hashCode(nodes);
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            new BddFormatter(this, writer, "").format();
            writer.flush();
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
