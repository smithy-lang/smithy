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

    private final int[] variables;
    private final int[] highs;
    private final int[] lows;
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
        this.variables = consumer.variables;
        this.highs = consumer.highs;
        this.lows = consumer.lows;

        if (consumer.index != nodeCount) {
            throw new IllegalStateException("Expected " + nodeCount + " nodes, but got " + consumer.index);
        }
    }

    Bdd(int[] variables, int[] highs, int[] lows, int nodeCount, int rootRef, int conditionCount, int resultCount) {
        validateArrays(variables, highs, lows, nodeCount);
        validateCounts(conditionCount, resultCount, nodeCount);
        validateRootReference(rootRef, nodeCount);

        this.variables = variables;
        this.highs = highs;
        this.lows = lows;
        this.rootRef = rootRef;
        this.conditionCount = conditionCount;
        this.resultCount = resultCount;
        this.nodeCount = nodeCount;
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

    private static void validateArrays(int[] variables, int[] highs, int[] lows, int nodeCount) {
        if (variables.length != highs.length || variables.length != lows.length) {
            throw new IllegalArgumentException("Array lengths must match: variables=" + variables.length +
                    ", highs=" + highs.length + ", lows=" + lows.length);
        } else if (nodeCount > variables.length) {
            throw new IllegalArgumentException("Node count (" + nodeCount +
                    ") exceeds array capacity (" + variables.length + ")");
        }
    }

    private static final class InputNodeConsumer implements BddNodeConsumer {
        private int index = 0;
        private final int[] variables;
        private final int[] highs;
        private final int[] lows;

        private InputNodeConsumer(int nodeCount) {
            this.variables = new int[nodeCount];
            this.highs = new int[nodeCount];
            this.lows = new int[nodeCount];
        }

        @Override
        public void accept(int var, int high, int low) {
            variables[index] = var;
            highs[index] = high;
            lows[index] = low;
            index++;
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
        return variables[nodeIndex];
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
        return highs[nodeIndex];
    }

    /**
     * Gets the low (false) reference for a node.
     *
     * @param nodeIndex the node index (0-based)
     * @return the low reference
     */
    public int getLow(int nodeIndex) {
        validateRange(nodeIndex);
        return lows[nodeIndex];
    }

    /**
     * Write all nodes to the consumer.
     *
     * @param consumer the consumer to receive the integers
     */
    public void getNodes(BddNodeConsumer consumer) {
        for (int i = 0; i < nodeCount; i++) {
            consumer.accept(variables[i], highs[i], lows[i]);
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
        int[] vars = this.variables;
        int[] hi = this.highs;
        int[] lo = this.lows;

        while (isNodeReference(ref)) {
            int idx = ref > 0 ? ref - 1 : -ref - 1; // Math.abs
            // test ^ complement, pick hi or lo
            ref = (ev.test(vars[idx]) ^ (ref < 0)) ? hi[idx] : lo[idx];
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

        // Now check the views of arrays of each.
        for (int i = 0; i < nodeCount; i++) {
            if (variables[i] != other.variables[i] || highs[i] != other.highs[i] || lows[i] != other.lows[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 31 * rootRef + nodeCount;
        // Sample up to 16 nodes distributed across the BDD
        int step = Math.max(1, nodeCount / 16);
        for (int i = 0; i < nodeCount; i += step) {
            hash = 31 * hash + variables[i];
            hash = 31 * hash + highs[i];
            hash = 31 * hash + lows[i];
        }
        return hash;
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
