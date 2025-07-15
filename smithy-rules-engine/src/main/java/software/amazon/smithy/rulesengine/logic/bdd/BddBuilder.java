/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 * <p>Node storage format: [variableIndex, highRef, lowRef]
 * where variableIndex identifies the condition being tested:
 * <ul>
 *   <li>-1: terminal node marker (only used for index 0)</li>
 *   <li>0 to conditionCount-1: condition indices</li>
 * </ul>
 */
final class BddBuilder {

    // Terminal constants
    private static final int TRUE_REF = 1;
    private static final int FALSE_REF = -1;

    // ITE operation cache for memoization
    private final Map<TripleKey, Integer> iteCache;
    // Node storage: index 0 is reserved for the terminal node
    private List<int[]> nodes;
    // Unique table for node deduplication
    private Map<TripleKey, Integer> uniqueTable;
    // Track the boundary between conditions and results
    private int conditionCount = -1;

    private final TripleKey mutableKey = new TripleKey(0, 0, 0);

    /**
     * Creates a new BDD engine.
     */
    public BddBuilder() {
        this.nodes = new ArrayList<>();
        this.uniqueTable = new HashMap<>();
        this.iteCache = new HashMap<>();
        // Initialize with terminal node at index 0
        nodes.add(new int[] {-1, TRUE_REF, FALSE_REF});
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
        // Reduction rule: if both branches are identical, skip this test
        if (high == low) {
            return high;
        }

        // Complement edge canonicalization: ensure complement only on low branch.
        // Don't apply this to result nodes or when branches contain results
        boolean flip = false;
        if (!isResultVariable(var) && !isResult(high) && !isResult(low) && isComplement(low)) {
            high = negate(high);
            low = negate(low);
            flip = true;
        }

        // Check if this node already exists
        mutableKey.update(var, high, low);
        Integer existing = uniqueTable.get(mutableKey);

        if (existing != null) {
            int ref = toReference(existing);
            return flip ? negate(ref) : ref;
        }

        // Create new node
        return insertNode(var, high, low, flip, nodes, uniqueTable);
    }

    private int insertNode(int var, int high, int low, boolean flip, List<int[]> nodes, Map<TripleKey, Integer> tbl) {
        int idx = nodes.size();
        nodes.add(new int[] {var, high, low});
        tbl.put(new TripleKey(var, high, low), idx);
        int ref = toReference(idx);
        return flip ? negate(ref) : ref;
    }

    /**
     * Negates a BDD reference (logical NOT).
     *
     * @param ref the reference to negate
     * @return the negated reference
     * @throws IllegalArgumentException if ref is a result terminal
     */
    public int negate(int ref) {
        if (isResult(ref)) {
            throw new IllegalArgumentException("Cannot negate result terminal: " + ref);
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
     * Checks if a variable index represents a result.
     *
     * @param varIdx the variable index
     * @return true if result
     */
    private boolean isResultVariable(int varIdx) {
        return conditionCount != -1 && varIdx >= conditionCount;
    }

    /**
     * Checks if a reference is any kind of terminal.
     *
     * @param ref the reference to check
     * @return true if any terminal type
     */
    private boolean isAnyTerminal(int ref) {
        return isTerminal(ref) || isResult(ref);
    }

    /**
     * Gets the variable index for a BDD node.
     *
     * @param ref the BDD reference
     * @return the variable index, or -1 for terminals
     */
    public int getVariable(int ref) {
        if (isTerminal(ref)) {
            return -1;
        } else if (isResult(ref)) {
            // For results, return the virtual variable index (conditionCount + resultIndex)
            return conditionCount + (ref - Bdd.RESULT_OFFSET);
        } else {
            return nodes.get(Math.abs(ref) - 1)[0];
        }
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
        if (isAnyTerminal(bdd)) {
            return bdd;
        }

        boolean complemented = isComplement(bdd);
        int nodeIndex = toNodeIndex(bdd);
        int[] node = nodes.get(nodeIndex);
        int nodeVar = node[0];

        if (nodeVar == varIndex) {
            // This node tests our variable, so take the appropriate branch
            int child = value ? node[1] : node[2];
            // Only negate if child is not a result
            return (complemented && !isResult(child)) ? negate(child) : child;
        } else if (nodeVar > varIndex) {
            // Variable doesn't appear in this BDD (due to ordering)
            return bdd;
        } else {
            // Variable appears deeper, so recurse on both branches
            int high = cofactor(node[1], varIndex, value);
            int low = cofactor(node[2], varIndex, value);
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
        // Normalize: if condition is complemented, swap branches
        if (isComplement(f)) {
            f = negate(f);
            int tmp = g;
            g = h;
            h = tmp;
        }

        // Terminal cases and validation.
        if (isResult(f)) {
            throw new IllegalArgumentException("Condition f must be boolean, not a result terminal");
        } else if (f == TRUE_REF) {
            return g;
        } else if (f == FALSE_REF) {
            return h;
        } else if (g == h) {
            return g;
        }

        // Boolean-specific optimizations (don't apply to result terminals)
        if (!(isResult(g) || isResult(h))) {
            // Standard Boolean identities
            if (g == TRUE_REF && h == FALSE_REF) {
                return f;
            } else if (g == FALSE_REF && h == TRUE_REF) {
                return negate(f);
            } else if (isComplement(g) && isComplement(h) && !isResult(negate(g)) && !isResult(negate(h))) {
                // Factor out common complement only if the negated values aren't results
                return negate(ite(f, negate(g), negate(h)));
            } else if (g == f) { // Simplifications when f appears in branches
                return or(f, h);
            } else if (h == f) {
                return and(f, g);
            } else if (g == negate(f)) {
                return and(negate(f), h);
            } else if (h == negate(f)) {
                return or(negate(f), g);
            }
        }

        // Check cache using the mutable key.
        Integer cached = iteCache.get(mutableKey.update(f, g, h));
        if (cached != null) {
            return cached;
        }

        // Create the actual key, and reserve cache slot to handle recursive calls
        TripleKey key = new TripleKey(f, g, h);
        iteCache.put(key, FALSE_REF);

        // Shannon expansion: find the top variable
        int v = getTopVariable(f, g, h);

        // Compute cofactors
        int f0 = cofactor(f, v, false);
        int f1 = cofactor(f, v, true);
        int g0 = cofactor(g, v, false);
        int g1 = cofactor(g, v, true);
        int h0 = cofactor(h, v, false);
        int h1 = cofactor(h, v, true);

        // Recursive ITE on cofactors
        int r0 = ite(f0, g0, h0);
        int r1 = ite(f1, g1, h1);

        // Build result node
        int result = makeNode(v, r1, r0);

        // Update cache with actual result
        iteCache.put(key, result);
        return result;
    }

    /**
     * Reduces the BDD by eliminating redundant nodes.
     *
     * @param rootRef the root of the BDD to reduce
     * @return the reduced BDD root
     */
    public int reduce(int rootRef) {
        // Quick exit for terminals/results
        if (isTerminal(rootRef) || isResult(rootRef)) {
            return rootRef;
        }

        boolean rootComp = isComplement(rootRef);
        int absRoot = rootComp ? negate(rootRef) : rootRef;

        // Prep new storage
        int N = nodes.size();
        List<int[]> newNodes = new ArrayList<>(N);
        Map<TripleKey, Integer> newUnique = new HashMap<>(N * 2);
        newNodes.add(new int[] {-1, TRUE_REF, FALSE_REF});

        // Mapping array
        int[] oldToNew = new int[N];
        Arrays.fill(oldToNew, -1);

        // Recurse
        int newRoot = reduceRec(absRoot, oldToNew, newNodes, newUnique);

        // Swap in
        this.nodes = newNodes;
        this.uniqueTable = newUnique;
        clearCaches();

        return rootComp ? negate(newRoot) : newRoot;
    }

    private int reduceRec(int ref, int[] oldToNew, List<int[]> newNodes, Map<TripleKey, Integer> newUnique) {
        // Handle terminals and results first
        if (isTerminal(ref)) {
            return ref;
        }

        // Handle result references (not stored as nodes)
        if (isResult(ref)) {
            return ref;
        }

        // Peel complement
        boolean comp = isComplement(ref);
        int abs = comp ? negate(ref) : ref;
        int idx = toNodeIndex(abs);

        // Already processed?
        int mapped = oldToNew[idx];
        if (mapped != -1) {
            return comp ? negate(mapped) : mapped;
        }

        // Process children
        int[] nd = nodes.get(idx);
        int var = nd[0];
        int hiNew = reduceRec(nd[1], oldToNew, newNodes, newUnique);
        int loNew = reduceRec(nd[2], oldToNew, newNodes, newUnique);

        // Apply reduction rule
        int resultAbs;
        if (hiNew == loNew) {
            resultAbs = hiNew;
        } else {
            resultAbs = makeNodeInNew(var, hiNew, loNew, newNodes, newUnique);
        }

        oldToNew[idx] = resultAbs;
        return comp ? negate(resultAbs) : resultAbs;
    }

    private int makeNodeInNew(int var, int hi, int lo, List<int[]> newNodes, Map<TripleKey, Integer> newUnique) {
        if (hi == lo) {
            return hi;
        }

        // Canonicalize complement edges (but not for result nodes)
        boolean comp = false;
        if (!isResultVariable(var) && !isResult(hi) && !isResult(lo) && isComplement(lo)) {
            hi = negate(hi);
            lo = negate(lo);
            comp = true;
        }

        // Check if node already exists in new structure
        Integer existing = newUnique.get(mutableKey.update(var, hi, lo));
        if (existing != null) {
            int ref = toReference(existing);
            return comp ? negate(ref) : ref;
        } else {
            // Create new node
            return insertNode(var, hi, lo, comp, newNodes, newUnique);
        }
    }

    /**
     * Finds the topmost variable among three BDDs.
     */
    private int getTopVariable(int f, int g, int h) {
        int varF = getVariable(f);
        int varG = getVariable(g);
        int varH = getVariable(h);

        // Filter out -1 (terminal marker) and find minimum
        int min = Integer.MAX_VALUE;
        if (varF >= 0 && varF < min) {
            min = varF;
        }
        if (varG >= 0 && varG < min) {
            min = varG;
        }
        if (varH >= 0 && varH < min) {
            min = varH;
        }

        return min == Integer.MAX_VALUE ? -1 : min;
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
        nodes.clear();
        nodes.add(new int[] {-1, TRUE_REF, FALSE_REF});
        conditionCount = -1;
        return this;
    }

    /**
     * Returns a defensive copy of the node table.
     *
     * @return list of node arrays
     */
    public List<int[]> getNodes() {
        List<int[]> copy = new ArrayList<>(nodes.size());
        for (int[] node : nodes) {
            copy.add(node.clone());
        }
        return copy;
    }

    /**
     * Get the array of nodes.
     *
     * @return array of nodes.
     */
    public int[][] getNodesArray() {
        return nodes.toArray(new int[0][]);
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

    private static final class TripleKey {
        private int a, b, c, hash;

        private TripleKey(int a, int b, int c) {
            update(a, b, c);
        }

        TripleKey update(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
            int i = (a * 31 + b) * 31 + c;
            this.hash = (i ^ (i >>> 16));
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof TripleKey)) {
                return false;
            }
            TripleKey k = (TripleKey) o;
            return a == k.a && b == k.b && c == k.c;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
