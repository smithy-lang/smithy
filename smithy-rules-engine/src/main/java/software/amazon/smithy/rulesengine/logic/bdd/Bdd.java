/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;

/**
 * Binary Decision Diagram (BDD) with complement edges for efficient endpoint rule evaluation.
 *
 * <p>A BDD provides a compact representation of decision logic where each condition is evaluated at most once along
 * any path. Complement edges (negative references) enable further size reduction through node sharing.
 *
 * <p><b>Reference Encoding:</b>
 * <ul>
 *   <li>{@code 0}: Invalid/unused reference (never appears in valid BDDs)</li>
 *   <li>{@code 1}: TRUE terminal; represents boolean true, treated as "no match" in endpoint resolution</li>
 *   <li>{@code -1}: FALSE terminal; represents boolean false, treated as "no match" in endpoint resolution</li>
 *   <li>{@code 2, 3, ...}: Node references (points to nodes array at index ref-1)</li>
 *   <li>{@code -2, -3, ...}: Complement node references (logical NOT of the referenced node)</li>
 *   <li>{@code 100_000_000+}: Result terminals (100_000_000 + resultIndex)</li>
 * </ul>
 *
 * <p>Result terminals are encoded as special references starting at 100_000_000 (RESULT_OFFSET).
 * When evaluating the BDD, any reference >= 100_000_000 represents a result terminal that
 * indexes into the results array (resultIndex = ref - 100_000_000). These are not stored
 * as nodes in the nodes array.
 *
 * <p><b>Node Format:</b> {@code [variable, high, low]}
 * <ul>
 *   <li>{@code variable}: Condition index (0 to conditionCount-1)</li>
 *   <li>{@code high}: Reference to follow when condition evaluates to true</li>
 *   <li>{@code low}: Reference to follow when condition evaluates to false</li>
 * </ul>
 */
public final class Bdd implements ToNode {
    /**
     * Result reference encoding.
     *
     * <p>Results start at 100M to avoid collision with node references.
     */
    public static final int RESULT_OFFSET = 100_000_000;

    private final Parameters parameters;
    private final List<Condition> conditions;
    private final List<Rule> results;
    private final int[][] nodes;
    private final int rootRef;

    /**
     * Builds a BDD from an endpoint ruleset.
     *
     * @param ruleSet the ruleset to convert
     * @return the constructed BDD
     */
    public static Bdd from(EndpointRuleSet ruleSet) {
        return from(Cfg.from(ruleSet));
    }

    /**
     * Builds a BDD from a control flow graph.
     *
     * @param cfg the control flow graph
     * @return the constructed BDD
     */
    public static Bdd from(Cfg cfg) {
        return from(cfg, new BddBuilder(), ConditionOrderingStrategy.defaultOrdering());
    }

    static Bdd from(Cfg cfg, BddBuilder bddBuilder, ConditionOrderingStrategy orderingStrategy) {
        return new BddCompiler(cfg, orderingStrategy, bddBuilder).compile();
    }

    public Bdd(Parameters params, List<Condition> conditions, List<Rule> results, int[][] nodes, int rootRef) {
        this.parameters = Objects.requireNonNull(params, "params is null");
        this.conditions = conditions;
        this.results = results;
        this.nodes = nodes;
        this.rootRef = rootRef;

        if (rootRef < 0 && rootRef != -1) {
            throw new IllegalArgumentException("Root reference cannot be complemented: " + rootRef);
        }
    }

    /**
     * Gets the ordered list of conditions.
     *
     * @return list of conditions in evaluation order
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Gets the number of conditions.
     *
     * @return condition count
     */
    public int getConditionCount() {
        return conditions.size();
    }

    /**
     * Gets the ordered list of results.
     *
     * @return list of results (null represents no match)
     */
    public List<Rule> getResults() {
        return results;
    }

    /**
     * Gets the BDD nodes.
     *
     * @return array of node triples
     */
    public int[][] getNodes() {
        return nodes;
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
     * Get the input parameters of the ruleset.
     *
     * @return input parameters.
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * Applies a transformation to the BDD and return a new BDD.
     *
     * @param transformer Optimization to apply.
     * @return the optimized BDD.
     */
    public Bdd transform(Function<Bdd, Bdd> transformer) {
        return transformer.apply(this);
    }

    /**
     * Checks if a reference points to a node (not a terminal or result).
     *
     * @param ref the reference to check
     * @return true if this is a node reference
     */
    public static boolean isNodeReference(int ref) {
        if (ref == 0 || isTerminal(ref)) {
            return false;
        }
        return Math.abs(ref) < RESULT_OFFSET;
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
        // -1 is FALSE terminal, not a complement
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
        return rootRef == other.rootRef
                && conditions.equals(other.conditions)
                && results.equals(other.results)
                && nodesEqual(nodes, other.nodes)
                && Objects.equals(parameters, other.parameters);
    }

    private static boolean nodesEqual(int[][] a, int[][] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Arrays.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 31 * rootRef + nodes.length;
        // Sample up to 16 nodes distributed across the BDD
        int step = Math.max(1, nodes.length / 16);
        for (int i = 0; i < nodes.length; i += step) {
            int[] node = nodes[i];
            hash = 31 * hash + node[0];
            hash = 31 * hash + node[1];
            hash = 31 * hash + node[2];
        }
        return hash;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * Appends a string representation to the given StringBuilder.
     *
     * @param sb the StringBuilder to append to
     * @return the given string builder.
     */
    public StringBuilder toString(StringBuilder sb) {
        // Calculate max width needed for first column identifiers
        int maxConditionIdx = conditions.size() - 1;
        int maxResultIdx = results.size() - 1;

        // Width needed for "C" + maxConditionIdx or "R" + maxResultIdx
        int conditionWidth = maxConditionIdx >= 0 ? String.valueOf(maxConditionIdx).length() + 1 : 2;
        int resultWidth = maxResultIdx >= 0 ? String.valueOf(maxResultIdx).length() + 1 : 2;
        int varWidth = Math.max(conditionWidth, resultWidth);

        sb.append("Bdd{\n");

        // Conditions
        sb.append("  conditions (").append(getConditionCount()).append("):\n");
        for (int i = 0; i < conditions.size(); i++) {
            sb.append(String.format("    %" + varWidth + "s: %s%n", "C" + i, conditions.get(i)));
        }

        // Results
        sb.append("  results (").append(results.size()).append("):\n");
        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("    %" + varWidth + "s: ", "R" + i));
            appendResult(sb, results.get(i));
            sb.append("\n");
        }

        // Root
        sb.append("  root: ").append(formatReference(rootRef)).append("\n");

        // Nodes
        sb.append("  nodes (").append(nodes.length).append("):\n");

        // Calculate width needed for node indices
        int indexWidth = String.valueOf(nodes.length - 1).length();

        for (int i = 0; i < nodes.length; i++) {
            sb.append(String.format("    %" + indexWidth + "d: ", i));
            if (i == 0) {
                sb.append("terminal");
            } else {
                int[] node = nodes[i];
                int varIdx = node[0];
                sb.append("[");

                // Use the calculated width for variable/result references
                if (varIdx < conditions.size()) {
                    sb.append(String.format("%" + varWidth + "s", "C" + varIdx));
                } else {
                    sb.append(String.format("%" + varWidth + "s", "R" + (varIdx - conditions.size())));
                }

                // Format the references with consistent spacing
                sb.append(", ")
                        .append(String.format("%6s", formatReference(node[1])))
                        .append(", ")
                        .append(String.format("%6s", formatReference(node[2])))
                        .append("]");
            }
            sb.append("\n");
        }

        sb.append("}");
        return sb;
    }

    private void appendResult(StringBuilder sb, Rule result) {
        if (result == null) {
            sb.append("(no match)");
        } else if (result instanceof EndpointRule) {
            sb.append("Endpoint: ").append(((EndpointRule) result).getEndpoint().getUrl());
        } else if (result instanceof ErrorRule) {
            sb.append("Error: ").append(((ErrorRule) result).getError());
        } else {
            sb.append(result.getClass().getSimpleName());
        }
    }

    private String formatReference(int ref) {
        if (ref == 0) {
            return "INVALID";
        } else if (ref == 1) {
            return "TRUE";
        } else if (ref == -1) {
            return "FALSE";
        } else if (ref >= Bdd.RESULT_OFFSET) {
            // This is a result reference
            int resultIdx = ref - Bdd.RESULT_OFFSET;
            return "R" + resultIdx;
        } else if (ref < 0) {
            return "!" + (Math.abs(ref) - 1);
        } else {
            return String.valueOf(ref - 1);
        }
    }

    public static Bdd fromNode(Node node) {
        return BddNodeHelpers.fromNode(node);
    }

    @Override
    public Node toNode() {
        return BddNodeHelpers.toNode(this);
    }
}
