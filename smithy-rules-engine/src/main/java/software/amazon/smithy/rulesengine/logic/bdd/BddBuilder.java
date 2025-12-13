/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.Arrays;

/**
 * Binary Decision Diagram (BDD) builder with complement edges and multi-terminal support.
 *
 * <p>This implementation uses CUDD-style complement edges where negative references
 * represent logical negation. The engine supports both boolean operations and
 * multi-terminal decision diagrams (MTBDDs) for endpoint resolution.
 *
 * <p>Reference encoding:
 * <ul>
 *   <li>0: Invalid/unused reference (never appears in valid BDDs)</li>
 *   <li>1: TRUE terminal</li>
 *   <li>-1: FALSE terminal</li>
 *   <li>2, 3, 4, ...: BDD nodes (use index + 1)</li>
 *   <li>-2, -3, -4, ...: Complement of BDD nodes</li>
 *   <li>Bdd.RESULT_OFFSET+: Result terminals (100_000_000 + resultIndex)</li>
 * </ul>
 */
final class BddBuilder {

    // Terminal constants
    private static final int TRUE_REF = 1;
    private static final int FALSE_REF = -1;

    // Node storage: flat array [var0, high0, low0, var1, high1, low1, ...]
    private static final int INITIAL_SIZE = 256 * 3;
    private int[] nodes = new int[INITIAL_SIZE];
    private int nodeCount;

    // Unique tables for node deduplication and ITE caching
    private final UniqueTable uniqueTable;
    private final UniqueTable iteCache;

    // Track the boundary between conditions and results
    private int conditionCount = -1;
    private int resultCount = 0;
    private int root = 0;

    /**
     * Creates a new BDD builder.
     */
    public BddBuilder() {
        this.nodeCount = 1;
        this.uniqueTable = new UniqueTable();
        this.iteCache = new UniqueTable(1024);
        initializeTerminalNode();
    }

    int getNodeCount() {
        return nodeCount;
    }

    int getRoot() {
        return root;
    }

    void setRoot(int root) {
        validateRootRef(root, nodeCount);
        this.root = root;
    }

    void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    /**
     * Sets the number of conditions. Must be called before creating result nodes.
     *
     * @param count the number of conditions
     */
    public void setConditionCount(int count) {
        if (conditionCount != -1) {
            throw new IllegalStateException("Condition count already set");
        }
        this.conditionCount = count;
    }

    /**
     * Returns the TRUE terminal reference.
     *
     * @return TRUE reference (always 1)
     */
    public int makeTrue() {
        return TRUE_REF;
    }

    /**
     * Returns the FALSE terminal reference.
     *
     * @return FALSE reference (always -1)
     */
    public int makeFalse() {
        return FALSE_REF;
    }

    /**
     * Creates a result terminal reference.
     *
     * @param resultIndex the result index (must be non-negative)
     * @return reference to the result terminal (RESULT_OFFSET + resultIndex)
     * @throws IllegalArgumentException if resultIndex is negative
     * @throws IllegalStateException if condition count not set
     */
    public int makeResult(int resultIndex) {
        if (conditionCount == -1) {
            throw new IllegalStateException("Must set condition count before creating results");
        } else if (resultIndex < 0) {
            throw new IllegalArgumentException("Result index must be non-negative: " + resultIndex);
        } else {
            return Bdd.RESULT_OFFSET + resultIndex;
        }
    }

    /**
     * Creates or retrieves a BDD node for the given variable and branches.
     *
     * <p>Applies BDD reduction rules:
     * <ul>
     *   <li>Eliminates redundant tests where both branches are identical</li>
     *   <li>Ensures complement edges appear only on the low branch</li>
     *   <li>Reuses existing nodes via the unique table</li>
     * </ul>
     *
     * @param var  the variable index
     * @param high the reference for when variable is true
     * @param low  the reference for when variable is false
     * @return reference to the BDD node
     */
    public int makeNode(int var, int high, int low) {
        if (conditionCount >= 0 && (var < 0 || var >= conditionCount)) {
            throw new IllegalArgumentException("Variable out of bounds: " + var);
        } else if (high == low) {
            // Reduction rule: if both branches are identical, skip this test
            return high;
        }

        // Complement edge canonicalization: ensure complement only on low branch.
        // Don't apply this to result nodes or when branches contain results
        boolean flip = shouldFlip(high, low);
        if (flip) {
            high = negate(high);
            low = negate(low);
        }

        // Check if this node already exists
        Integer existing = uniqueTable.get(var, high, low);
        if (existing != null) {
            return applyFlip(flip, existing);
        } else {
            return insertNode(var, high, low, flip);
        }
    }

    private boolean shouldFlip(int high, int low) {
        return isComplement(low) && !isResult(high) && !isResult(low);
    }

    private int applyFlip(boolean flip, int idx) {
        return flip ? negate(toReference(idx)) : toReference(idx);
    }

    private int insertNode(int var, int high, int low, boolean flip) {
        ensureCapacity();

        int idx = nodeCount;
        int base = idx * 3;
        nodes[base] = var;
        nodes[base + 1] = high;
        nodes[base + 2] = low;
        nodeCount++;

        uniqueTable.put(var, high, low, idx);
        return applyFlip(flip, idx);
    }

    private void ensureCapacity() {
        if (nodeCount * 3 >= nodes.length) {
            // Double the current capacity
            int newCapacity = nodes.length * 2;
            nodes = Arrays.copyOf(nodes, newCapacity);
        }
    }

    /**
     * Negates a BDD reference (logical NOT).
     *
     * @param ref the reference to negate
     * @return the negated reference
     * @throws IllegalArgumentException if ref is a result terminal or invalid.
     */
    public int negate(int ref) {
        if (ref == 0 || isResult(ref)) {
            throw new IllegalArgumentException(
                    "Cannot negate " + (ref == 0 ? "invalid reference: " : "result terminal: ") + ref);
        }
        return -ref;
    }

    /**
     * Checks if a reference has a complement edge.
     *
     * @param ref the reference to check
     * @return true if complemented (negative)
     */
    public boolean isComplement(int ref) {
        return ref < 0;
    }

    /**
     * Checks if a reference is a boolean terminal (TRUE or FALSE).
     *
     * @param ref the reference to check
     * @return true if boolean terminal
     */
    public boolean isTerminal(int ref) {
        return Math.abs(ref) == 1;
    }

    /**
     * Checks if a reference is a result terminal.
     *
     * @param ref the reference to check
     * @return true if result terminal
     */
    public boolean isResult(int ref) {
        if (isTerminal(ref) || conditionCount == -1) {
            return false;
        }
        return ref >= Bdd.RESULT_OFFSET;
    }

    /**
     * Checks if a reference is a leaf (terminal or result).
     *
     * @param ref the reference to check
     * @return true if this is a leaf node
     */
    private boolean isLeaf(int ref) {
        return Math.abs(ref) == TRUE_REF || ref >= Bdd.RESULT_OFFSET;
    }

    /**
     * Gets the variable index for a BDD node.
     *
     * @param ref the BDD reference
     * @return the variable index, or -1 for terminals
     */
    public int getVariable(int ref) {
        if (isLeaf(ref)) {
            return -1;
        }

        int nodeIndex = Math.abs(ref) - 1;
        validateNodeIndex(nodeIndex);
        return nodes[nodeIndex * 3];
    }

    /**
     * Computes the cofactor of a BDD with respect to a variable assignment.
     *
     * @param bdd      the BDD to restrict
     * @param varIndex the variable to fix
     * @param value    the value to assign (true or false)
     * @return the restricted BDD
     */
    public int cofactor(int bdd, int varIndex, boolean value) {
        // Terminals and results are unaffected by cofactoring
        if (isLeaf(bdd)) {
            return bdd;
        }

        boolean complemented = isComplement(bdd);
        int nodeIndex = toNodeIndex(bdd);
        validateNodeIndex(nodeIndex);

        int base = nodeIndex * 3;
        int nodeVar = nodes[base];

        if (nodeVar == varIndex) {
            // This node tests our variable, so take the appropriate branch
            int child = value ? nodes[base + 1] : nodes[base + 2];
            return applyComplement(complemented, child);
        } else if (nodeVar > varIndex) {
            // Variable doesn't appear in this BDD (due to ordering)
            return bdd;
        } else {
            // Variable appears deeper, so recurse on both branches
            int high = cofactor(nodes[base + 1], varIndex, value);
            int low = cofactor(nodes[base + 2], varIndex, value);
            int result = makeNode(nodeVar, high, low);
            return applyComplement(complemented, result);
        }
    }

    /**
     * Applies complement to a ref, enforcing the invariant that results never appear
     * under complemented edges.
     */
    private int applyComplement(boolean complemented, int ref) {
        if (!complemented) {
            return ref;
        } else if (isResult(ref)) {
            throw new IllegalStateException("Invariant violated: complemented edge to result terminal: " + ref);
        } else {
            return negate(ref);
        }
    }

    /**
     * Returns the absolute (non-complemented) form of a ref.
     *
     * <p>This strips the complement bit to get the canonical positive ref,
     * used for memoization keys and traversal. Safe because complemented refs
     * are never results (by invariant).
     */
    private int absRef(int ref) {
        if (ref <= -Bdd.RESULT_OFFSET) {
            throw new IllegalStateException("Complemented result terminal: " + ref);
        }
        // Use long to handle Integer.MIN_VALUE correctly
        return (int) (ref < 0 ? -(long) ref : (long) ref);
    }

    /**
     * Computes the logical AND of two BDDs.
     *
     * @param f first operand
     * @param g second operand
     * @return f AND g
     * @throws IllegalArgumentException if operands are result terminals
     */
    public int and(int f, int g) {
        validateBooleanOperands(f, g, "AND");
        return ite(f, g, makeFalse());
    }

    /**
     * Computes the logical OR of two BDDs.
     *
     * @param f first operand
     * @param g second operand
     * @return f OR g
     * @throws IllegalArgumentException if operands are result terminals
     */
    public int or(int f, int g) {
        validateBooleanOperands(f, g, "OR");
        return ite(f, makeTrue(), g);
    }

    /**
     * Computes if-then-else (ITE) operation: "if f then g else h".
     *
     * <p>This is the fundamental BDD operation from which all others are derived.
     * Includes optimizations for special cases and complement edges.
     *
     * @param f the condition (must be boolean)
     * @param g the "then" branch
     * @param h the "else" branch
     * @return the resulting BDD
     * @throws IllegalArgumentException if f is a result terminal
     */
    public int ite(int f, int g, int h) {
        // Normalize complement edge on f
        if (f < 0) {
            f = -f;
            int tmp = g;
            g = h;
            h = tmp;
        }

        // Quick terminal cases
        if (f == TRUE_REF || g == h) {
            return g;
        } else if (isResult(f)) {
            throw new IllegalArgumentException("Condition f must be boolean, not a result terminal");
        } else if (!isResult(g) && !isResult(h)) {
            // Boolean-only identities
            if (g == TRUE_REF && h == FALSE_REF) {
                return f;
            } else if (g == FALSE_REF && h == TRUE_REF) {
                return negate(f);
            } else if (g == f) {
                return or(f, h);
            } else if (h == f) {
                return and(f, g);
            } else if (g == negate(f)) {
                return and(negate(f), h);
            } else if (h == negate(f)) {
                return or(negate(f), g);
            } else if (isComplement(g) && isComplement(h)) {
                // Factor out common complement
                return negate(ite(f, negate(g), negate(h)));
            }
        }

        Integer cached = iteCache.get(f, g, h);
        if (cached != null) {
            return cached;
        }

        // Reserve cache slot to handle recursive calls
        iteCache.put(f, g, h, 0); // placeholder

        // Shannon expansion
        int v = getTopVariable(f, g, h);
        int r0 = ite(cofactor(f, v, false), cofactor(g, v, false), cofactor(h, v, false));
        int r1 = ite(cofactor(f, v, true), cofactor(g, v, true), cofactor(h, v, true));

        // Build result node and cache it
        int result = makeNode(v, r1, r0);
        iteCache.put(f, g, h, result);
        return result;
    }

    /**
     * Reduces the BDD by eliminating redundant nodes.
     *
     * @param rootRef the root of the BDD to reduce
     * @return the reduced BDD root
     */
    public int reduce(int rootRef) {
        if (isLeaf(rootRef)) {
            return rootRef;
        }

        // Peel off complement on the root
        boolean rootComp = rootRef < 0;
        int absRoot = absRef(rootRef);

        // Allocate new nodes array
        int[] newNodes = new int[nodeCount * 3];

        // Clear and reuse the existing unique table
        uniqueTable.clear();

        // Initialize the terminal node
        newNodes[0] = -1;
        newNodes[1] = TRUE_REF;
        newNodes[2] = FALSE_REF;

        // Prepare the visitation map
        int[] oldToNew = new int[nodeCount];
        Arrays.fill(oldToNew, -1);
        int[] newCount = {1}; // start after terminal

        // Recursively rebuild
        int newRoot = reduceRec(absRoot, oldToNew, newNodes, newCount);

        // Swap in the new nodes array (trimmed to actual size)
        this.nodes = Arrays.copyOf(newNodes, newCount[0] * 3);
        this.nodeCount = newCount[0];
        clearCaches();

        return applyComplement(rootComp, newRoot);
    }

    private int reduceRec(
            int ref,
            int[] oldToNew,
            int[] newNodes,
            int[] newCount
    ) {
        if (isLeaf(ref)) {
            return ref;
        }

        // Peel complement
        boolean comp = ref < 0;
        int abs = absRef(ref);
        int idx = toNodeIndex(abs);

        // If already mapped, return it
        int mapped = oldToNew[idx];
        if (mapped != -1) {
            return applyComplement(comp, mapped);
        }

        // Recurse on children
        int base = idx * 3;
        int var = nodes[base];
        int hiNew = reduceRec(nodes[base + 1], oldToNew, newNodes, newCount);
        int loNew = reduceRec(nodes[base + 2], oldToNew, newNodes, newCount);

        // Reduction rule
        int resultAbs;
        if (hiNew == loNew) {
            resultAbs = hiNew;
        } else {
            // Canonicalize complement edges on the low branch
            boolean flip = shouldFlip(hiNew, loNew);
            if (flip) {
                hiNew = negate(hiNew);
                loNew = negate(loNew);
            }

            // Lookup or create a new node
            Integer existing = uniqueTable.get(var, hiNew, loNew);
            if (existing != null) {
                resultAbs = toReference(existing);
            } else {
                int nodeIdx = newCount[0]++;
                int newBase = nodeIdx * 3;
                newNodes[newBase] = var;
                newNodes[newBase + 1] = hiNew;
                newNodes[newBase + 2] = loNew;
                uniqueTable.put(var, hiNew, loNew, nodeIdx);
                resultAbs = toReference(nodeIdx);
            }

            if (flip) {
                resultAbs = negate(resultAbs);
            }
        }

        oldToNew[idx] = resultAbs;
        return applyComplement(comp, resultAbs);
    }

    /**
     * Finds the topmost variable among three BDDs.
     */
    private int getTopVariable(int f, int g, int h) {
        int minVar = Integer.MAX_VALUE;
        minVar = updateMinVariable(minVar, f);
        minVar = updateMinVariable(minVar, g);
        minVar = updateMinVariable(minVar, h);
        return (minVar == Integer.MAX_VALUE) ? -1 : minVar;
    }

    private int updateMinVariable(int currentMin, int ref) {
        int absRef = Math.abs(ref);
        if (absRef > 1 && absRef < Bdd.RESULT_OFFSET) {
            return Math.min(currentMin, nodes[(absRef - 1) * 3]);
        }
        return currentMin;
    }

    /**
     * Clears all operation caches.
     */
    public void clearCaches() {
        iteCache.clear();
    }

    /**
     * Validates that a root reference is valid.
     */
    private void validateRootRef(int ref, int maxNodeCount) {
        if (ref == 0) {
            throw new IllegalArgumentException("Invalid root: 0");
        }
        if (ref <= -Bdd.RESULT_OFFSET) {
            throw new IllegalArgumentException("Invalid root: complemented result");
        }
        // Use long to handle Integer.MIN_VALUE correctly
        long abs = ref < 0 ? -(long) ref : (long) ref;
        if (abs == 1 || ref >= Bdd.RESULT_OFFSET) {
            return; // terminal or result is valid
        }
        if (abs < 2 || abs > maxNodeCount) {
            throw new IllegalArgumentException("Invalid root: node ref out of range: " + ref);
        }
    }

    /**
     * Clear out the state of the builder, but reuse the existing arrays, maps, etc.
     *
     * @return this builder
     */
    BddBuilder reset() {
        clearCaches();
        uniqueTable.clear();
        Arrays.fill(nodes, 0, nodeCount * 3, 0);
        nodeCount = 1;
        initializeTerminalNode();
        conditionCount = -1;
        resultCount = 0;
        root = 0;
        return this;
    }

    /**
     * Builds a BDD from the current state of the builder.
     *
     * @return a new BDD instance
     * @throws IllegalStateException if condition count has not been set
     */
    Bdd build() {
        if (conditionCount == -1) {
            throw new IllegalStateException("Condition count must be set before building BDD");
        }
        validateRootRef(root, nodeCount);

        int[] n = Arrays.copyOf(nodes, nodeCount * 3);
        return new Bdd(root, conditionCount, resultCount, nodeCount, n);
    }

    private void validateBooleanOperands(int f, int g, String operation) {
        if (isResult(f) || isResult(g)) {
            throw new IllegalArgumentException("Cannot perform " + operation + " on result terminals");
        }
    }

    private int toNodeIndex(int ref) {
        return Math.abs(ref) - 1;
    }

    private int toReference(int nodeIndex) {
        return nodeIndex + 1;
    }

    private void validateNodeIndex(int nodeIndex) {
        if (nodeIndex >= nodeCount || nodeIndex < 0) {
            throw new IllegalStateException("Invalid node index: " + nodeIndex);
        }
    }

    private void initializeTerminalNode() {
        nodes[0] = -1;
        nodes[1] = TRUE_REF;
        nodes[2] = FALSE_REF;
    }
}
