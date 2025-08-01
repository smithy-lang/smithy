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

    // Node storage: three separate arrays
    private int[] variables = new int[1024];
    private int[] highs = new int[1024];
    private int[] lows = new int[1024];
    private int nodeCount;

    // Unique tables for node deduplication and ITE caching
    private final UniqueTable uniqueTable;
    private final UniqueTable iteCache;

    // Track the boundary between conditions and results
    private int conditionCount = -1;

    /**
     * Creates a new BDD engine.
     */
    public BddBuilder() {
        this.nodeCount = 1;
        this.uniqueTable = new UniqueTable();
        this.iteCache = new UniqueTable(4096); // Larger initial capacity for ITE cache
        initializeTerminalNode();
    }

    int getNodeCount() {
        return nodeCount;
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
        variables[idx] = var;
        highs[idx] = high;
        lows[idx] = low;
        nodeCount++;

        uniqueTable.put(var, high, low, idx);
        return applyFlip(flip, idx);
    }

    private void ensureCapacity() {
        if (nodeCount >= variables.length) {
            // Double the current capacity
            int newCapacity = variables.length * 2;
            variables = Arrays.copyOf(variables, newCapacity);
            highs = Arrays.copyOf(highs, newCapacity);
            lows = Arrays.copyOf(lows, newCapacity);
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
        return variables[nodeIndex];
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

        int nodeVar = variables[nodeIndex];

        if (nodeVar == varIndex) {
            // This node tests our variable, so take the appropriate branch
            int child = value ? highs[nodeIndex] : lows[nodeIndex];
            // Only negate if child is not a result
            return (complemented && !isResult(child)) ? negate(child) : child;
        } else if (nodeVar > varIndex) {
            // Variable doesn't appear in this BDD (due to ordering)
            return bdd;
        } else {
            // Variable appears deeper, so recurse on both branches
            int high = cofactor(highs[nodeIndex], varIndex, value);
            int low = cofactor(lows[nodeIndex], varIndex, value);
            int result = makeNode(nodeVar, high, low);
            return (complemented && !isResult(result)) ? negate(result) : result;
        }
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

        // Check cache
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

        // Build result node
        int result = makeNode(v, r1, r0);

        // Update cache with actual result
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
        int absRoot = rootComp ? negate(rootRef) : rootRef;

        // Allocate new tables
        int[] newVariables = new int[nodeCount];
        int[] newHighs = new int[nodeCount];
        int[] newLows = new int[nodeCount];

        // Clear and reuse the existing unique table
        uniqueTable.clear();

        // Initialize the terminal node
        newVariables[0] = -1;
        newHighs[0] = TRUE_REF;
        newLows[0] = FALSE_REF;

        // Prepare the visitation map
        int[] oldToNew = new int[nodeCount];
        Arrays.fill(oldToNew, -1);
        int[] newCount = {1}; // start after terminal

        // Recursively rebuild
        int newRoot = reduceRec(absRoot, oldToNew, newVariables, newHighs, newLows, newCount);

        // Swap in the new tables
        this.variables = newVariables;
        this.highs = newHighs;
        this.lows = newLows;
        this.nodeCount = newCount[0];
        clearCaches();

        return rootComp ? negate(newRoot) : newRoot;
    }

    private int reduceRec(
            int ref,
            int[] oldToNew,
            int[] newVariables,
            int[] newHighs,
            int[] newLows,
            int[] newCount
    ) {
        if (isLeaf(ref)) {
            return ref;
        }

        // Peel complement
        boolean comp = ref < 0;
        int abs = comp ? negate(ref) : ref;
        int idx = toNodeIndex(abs);

        // If already mapped, return it
        int mapped = oldToNew[idx];
        if (mapped != -1) {
            return comp ? negate(mapped) : mapped;
        }

        // Recurse on children
        int var = variables[idx];
        int hiNew = reduceRec(highs[idx], oldToNew, newVariables, newHighs, newLows, newCount);
        int loNew = reduceRec(lows[idx], oldToNew, newVariables, newHighs, newLows, newCount);

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
                newVariables[nodeIdx] = var;
                newHighs[nodeIdx] = hiNew;
                newLows[nodeIdx] = loNew;
                uniqueTable.put(var, hiNew, loNew, nodeIdx);
                resultAbs = toReference(nodeIdx);
            }

            if (flip) {
                resultAbs = negate(resultAbs);
            }
        }

        oldToNew[idx] = resultAbs;
        return comp ? negate(resultAbs) : resultAbs;
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
            return Math.min(currentMin, variables[absRef - 1]);
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
     * Clear out the state of the builder, but reuse the existing arrays, maps, etc.
     *
     * @return this builder
     */
    public BddBuilder reset() {
        clearCaches();
        uniqueTable.clear();
        Arrays.fill(variables, 0, nodeCount, 0);
        Arrays.fill(highs, 0, nodeCount, 0);
        Arrays.fill(lows, 0, nodeCount, 0);
        nodeCount = 1;
        initializeTerminalNode();
        conditionCount = -1;
        return this;
    }

    /**
     * Builds a BDD from the current state of the builder.
     *
     * @return a new BDD instance
     * @throws IllegalStateException if condition count has not been set
     */
    Bdd build(int rootRef, int resultCount) {
        if (conditionCount == -1) {
            throw new IllegalStateException("Condition count must be set before building BDD");
        }

        int[] v = Arrays.copyOf(variables, nodeCount);
        int[] h = Arrays.copyOf(highs, nodeCount);
        int[] l = Arrays.copyOf(lows, nodeCount);
        return new Bdd(v, h, l, nodeCount, rootRef, conditionCount, resultCount);
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
        variables[0] = -1;
        highs[0] = TRUE_REF;
        lows[0] = FALSE_REF;
    }
}
