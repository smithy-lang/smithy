/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.RecordLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.TupleLiteral;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.ConditionReference;

/**
 * Builder for constructing Control Flow Graphs with node deduplication and result convergence.
 *
 * <p>This builder performs hash-consing during construction and creates convergence nodes
 * for structurally similar results to minimize BDD size.
 */
public final class CfgBuilder {
    private static final Logger LOGGER = Logger.getLogger(CfgBuilder.class.getName());

    // Configuration constants
    private static final int MAX_DIVERGENT_PATHS_FOR_CONVERGENCE = 5;
    private static final int MIN_RESULTS_FOR_CONVERGENCE = 2;

    final EndpointRuleSet ruleSet;

    // Node deduplication
    private final Map<NodeSignature, CfgNode> nodeCache = new HashMap<>();

    // Condition and result canonicalization
    private final Map<Condition, ConditionReference> conditionToReference = new HashMap<>();
    private final Map<Rule, Rule> resultCache = new HashMap<>();
    private final Map<Rule, ResultNode> resultNodeCache = new HashMap<>();

    // Result convergence support
    private final Map<Rule, CfgNode> resultToConvergenceNode = new HashMap<>();
    private int convergenceNodesCreated = 0;
    private int phiVariableCounter = 0;

    private String version;

    public CfgBuilder(EndpointRuleSet ruleSet) {
        // Apply SSA transform to ensure globally unique variable names
        this.ruleSet = SsaTransform.transform(ruleSet);
        this.version = ruleSet.getVersion();

        // Analyze results and create convergence nodes
        analyzeAndCreateConvergenceNodes();
    }

    /**
     * Build the CFG with the given root node.
     *
     * @param root Root node to use for the built CFG.
     * @return the built CFG.
     */
    public Cfg build(CfgNode root) {
        return new Cfg(ruleSet, Objects.requireNonNull(root));
    }

    /**
     * Set the version of the endpoint rules engine (e.g., 1.1).
     *
     * @param version Version to set.
     * @return the builder;
     */
    public CfgBuilder version(String version) {
        this.version = Objects.requireNonNull(version);
        return this;
    }

    /**
     * Creates a condition node, reusing existing nodes when possible.
     *
     * @param condition   the condition to evaluate
     * @param trueBranch  the node to evaluate when the condition is true
     * @param falseBranch the node to evaluate when the condition is false
     * @return a condition node (possibly cached)
     */
    public CfgNode createCondition(Condition condition, CfgNode trueBranch, CfgNode falseBranch) {
        return createCondition(createConditionReference(condition), trueBranch, falseBranch);
    }

    /**
     * Creates a condition node, reusing existing nodes when possible.
     *
     * @param condRef the condition reference to evaluate
     * @param trueBranch the node to evaluate when the condition is true
     * @param falseBranch the node to evaluate when the condition is false
     * @return a condition node (possibly cached)
     */
    public CfgNode createCondition(ConditionReference condRef, CfgNode trueBranch, CfgNode falseBranch) {
        NodeSignature signature = new NodeSignature(condRef, trueBranch, falseBranch);
        return nodeCache.computeIfAbsent(signature, key -> new ConditionNode(condRef, trueBranch, falseBranch));
    }

    /**
     * Creates a result node representing a terminal rule evaluation.
     *
     * <p>If this result is part of a convergence group, returns the shared convergence node instead.
     *
     * @param rule the result rule (endpoint or error)
     * @return a result node or convergence node
     */
    public CfgNode createResult(Rule rule) {
        // Intern the result
        Rule interned = intern(rule);

        // Check if this result has a convergence node
        CfgNode convergenceNode = resultToConvergenceNode.get(interned);
        if (convergenceNode != null) {
            LOGGER.fine("Using convergence node for result: " + interned);
            return convergenceNode;
        }

        // Regular result node
        return resultNodeCache.computeIfAbsent(interned, ResultNode::new);
    }

    /**
     * Creates a canonical condition reference, handling negation and deduplication.
     */
    public ConditionReference createConditionReference(Condition condition) {
        ConditionReference cached = conditionToReference.get(condition);
        if (cached != null) {
            return cached;
        }

        boolean negated = false;
        Condition canonical = condition;

        if (isNegationWrapper(condition)) {
            negated = true;
            canonical = unwrapNegation(condition);

            ConditionReference existing = conditionToReference.get(canonical);
            if (existing != null) {
                ConditionReference negatedReference = existing.negate();
                conditionToReference.put(condition, negatedReference);
                return negatedReference;
            }
        }

        canonical = canonical.canonicalize();

        Condition beforeBooleanCanon = canonical;
        canonical = canonicalizeBooleanEquals(canonical);

        if (!canonical.equals(beforeBooleanCanon)) {
            negated = !negated;
        }

        ConditionReference reference = new ConditionReference(canonical, negated);
        conditionToReference.put(condition, reference);

        if (!negated && !condition.equals(canonical)) {
            conditionToReference.put(canonical, reference);
        }

        return reference;
    }

    private void analyzeAndCreateConvergenceNodes() {
        LOGGER.info("Analyzing results for convergence opportunities");
        List<Rule> allResults = new ArrayList<>();
        for (Rule rule : ruleSet.getRules()) {
            collectResultsFromRule(rule, allResults);
        }
        LOGGER.info("Found " + allResults.size() + " total results");

        Map<ResultSignature, ResultGroup> groups = groupResultsByStructure(allResults);
        createConvergenceNodesForGroups(groups);

        LOGGER.info(String.format("Created %d convergence nodes for %d result groups",
                convergenceNodesCreated,
                groups.size()));
    }

    private void collectResultsFromRule(Rule rule, List<Rule> results) {
        if (rule instanceof EndpointRule || rule instanceof ErrorRule) {
            results.add(intern(rule));
        } else if (rule instanceof TreeRule) {
            for (Rule nestedRule : ((TreeRule) rule).getRules()) {
                collectResultsFromRule(nestedRule, results);
            }
        }
    }

    private Rule intern(Rule rule) {
        return resultCache.computeIfAbsent(canonicalizeResult(rule), k -> k);
    }

    private Map<ResultSignature, ResultGroup> groupResultsByStructure(List<Rule> results) {
        Map<ResultSignature, ResultGroup> groups = new HashMap<>();
        for (Rule result : results) {
            ResultSignature sig = new ResultSignature(result);
            groups.computeIfAbsent(sig, k -> new ResultGroup()).add(result);
        }
        return groups;
    }

    private void createConvergenceNodesForGroups(Map<ResultSignature, ResultGroup> groups) {
        for (ResultGroup group : groups.values()) {
            if (shouldGroupResults(group)) {
                createConvergenceNodeForGroup(group);
            }
        }
    }

    private boolean shouldGroupResults(ResultGroup group) {
        if (group.results.size() < MIN_RESULTS_FOR_CONVERGENCE) {
            return false;
        }

        int divergentCount = group.getDivergentPathCount();
        if (divergentCount == 0) {
            return false;
        }

        if (divergentCount > MAX_DIVERGENT_PATHS_FOR_CONVERGENCE) {
            LOGGER.fine(String.format("Skipping convergence for group with %d divergent paths (perf)",
                    divergentCount));
            return false;
        }

        return true;
    }

    private void createConvergenceNodeForGroup(ResultGroup group) {
        Map<LocationPath, Set<String>> divergentPaths = group.getDivergentPaths();
        Rule canonical = group.results.get(0); // already interned

        Map<LocationPath, String> phiVariableMap = createPhiVariablesByPath(divergentPaths);
        Rule rewrittenResult = rewriteResultWithPhiVariables(canonical, divergentPaths, phiVariableMap);

        CfgNode convergenceNode = buildConvergenceNode(rewrittenResult, divergentPaths, phiVariableMap);

        for (Rule result : group.results) {
            resultToConvergenceNode.put(result, convergenceNode); // keys are the interned instances
        }

        convergenceNodesCreated++;
    }

    private Map<LocationPath, String> createPhiVariablesByPath(Map<LocationPath, Set<String>> divergentPaths) {
        List<LocationPath> paths = new ArrayList<>(divergentPaths.keySet());
        Collections.sort(paths);

        Map<LocationPath, String> phiVariableMap = new LinkedHashMap<>();
        for (LocationPath path : paths) {
            phiVariableMap.put(path, "phi_result_" + (phiVariableCounter++));
        }
        return phiVariableMap; // already ordered
    }

    private CfgNode buildConvergenceNode(
            Rule result,
            Map<LocationPath, Set<String>> divergentPaths,
            Map<LocationPath, String> phiVariableMap
    ) {
        CfgNode resultNode = new ResultNode(result);

        // Apply phi nodes in the order already established by phiVariableMap
        for (Map.Entry<LocationPath, String> entry : phiVariableMap.entrySet()) {
            Set<String> versions = divergentPaths.get(entry.getKey());
            String phiVar = entry.getValue();

            Condition coalesceCondition = createCoalesceCondition(phiVar, versions);
            ConditionReference condRef = new ConditionReference(coalesceCondition, false);

            // Use NO_MATCH as false branch since coalesce should always succeed
            // This ensures we get a valid result index instead of null that would result from FALSE.
            CfgNode noMatch = createResult(NoMatchRule.INSTANCE);
            resultNode = new ConditionNode(condRef, resultNode, noMatch);

            // Log with deterministic ordering
            LOGGER.fine(() -> {
                List<String> sortedVersions = new ArrayList<>(versions);
                Collections.sort(sortedVersions);
                return String.format("Created convergence: %s = coalesce(%s) for path %s",
                        phiVar,
                        String.join(",", sortedVersions),
                        entry.getKey());
            });
        }

        return resultNode;
    }

    private Rule rewriteResultWithPhiVariables(
            Rule result,
            Map<LocationPath, Set<String>> divergentPaths,
            Map<LocationPath, String> phiVariableMap
    ) {
        // Build replacements for URL or error expression only (not headers/properties)
        Map<String, Expression> urlReplacements = buildPhiReplacements(divergentPaths, phiVariableMap);

        if (urlReplacements.isEmpty()) {
            return result;
        }

        TreeRewriter rewriter = TreeRewriter.forReplacements(urlReplacements);

        if (result instanceof EndpointRule) {
            return rewriteEndpointRule((EndpointRule) result, rewriter);
        } else if (result instanceof ErrorRule) {
            return rewriteErrorRule((ErrorRule) result, rewriter);
        }

        return result;
    }

    private Map<String, Expression> buildPhiReplacements(
            Map<LocationPath, Set<String>> divergentPaths,
            Map<LocationPath, String> phiVariableMap
    ) {
        Map<String, Expression> replacements = new HashMap<>();
        for (Map.Entry<LocationPath, Set<String>> entry : divergentPaths.entrySet()) {
            LocationPath path = entry.getKey();
            String phiVar = phiVariableMap.get(path);
            Expression phiRef = Expression.getReference(Identifier.of(phiVar));

            // Map all versions at this path to the phi variable
            for (String version : entry.getValue()) {
                replacements.put(version, phiRef);
            }
        }
        return replacements;
    }

    private Rule rewriteEndpointRule(EndpointRule rule, TreeRewriter rewriter) {
        Endpoint endpoint = rule.getEndpoint();
        Expression rewrittenUrl = rewriter.rewrite(endpoint.getUrl());

        if (rewrittenUrl != endpoint.getUrl()) {
            Endpoint rewrittenEndpoint = Endpoint.builder()
                    .url(rewrittenUrl)
                    .headers(endpoint.getHeaders())
                    .properties(endpoint.getProperties())
                    .build();

            return EndpointRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(Collections.emptyList())
                    .endpoint(rewrittenEndpoint);
        }
        return rule;
    }

    private Rule rewriteErrorRule(ErrorRule rule, TreeRewriter rewriter) {
        Expression rewrittenError = rewriter.rewrite(rule.getError());

        if (rewrittenError != rule.getError()) {
            return ErrorRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(Collections.emptyList())
                    .error(rewrittenError);
        }
        return rule;
    }

    private Condition createCoalesceCondition(String resultVar, Set<String> versions) {
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Cannot create coalesce with no versions");
        }

        // TreeSet for both deduplication and deterministic ordering
        Set<String> inputs = new TreeSet<>(versions);
        List<Expression> refs = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            refs.add(Expression.getReference(Identifier.of(input)));
        }

        return Condition.builder().fn(Coalesce.ofExpressions(refs)).result(Identifier.of(resultVar)).build();
    }

    private Rule canonicalizeResult(Rule rule) {
        return rule == null ? null : rule.withConditions(Collections.emptyList());
    }

    private Condition canonicalizeBooleanEquals(Condition condition) {
        if (!(condition.getFunction() instanceof BooleanEquals)) {
            return condition;
        }

        List<Expression> args = condition.getFunction().getArguments();
        if (args.size() != 2 || !(args.get(0) instanceof Reference) || !(args.get(1) instanceof Literal)) {
            return condition;
        }

        Reference ref = (Reference) args.get(0);
        Boolean literalValue = ((Literal) args.get(1)).asBooleanLiteral().orElse(null);

        if (literalValue != null && !literalValue && ruleSet != null) {
            String varName = ref.getName().toString();
            Optional<Parameter> param = ruleSet.getParameters().get(Identifier.of(varName));
            if (param.isPresent() && param.get().getDefault().isPresent()) {
                return condition.toBuilder().fn(BooleanEquals.ofExpressions(ref, true)).build();
            }
        }

        return condition;
    }

    private static boolean isNegationWrapper(Condition condition) {
        return condition.getFunction() instanceof Not
                && !condition.getResult().isPresent()
                && condition.getFunction().getArguments().get(0) instanceof LibraryFunction;
    }

    private static Condition unwrapNegation(Condition negatedCondition) {
        return negatedCondition.toBuilder()
                .fn((LibraryFunction) negatedCondition.getFunction().getArguments().get(0))
                .build();
    }

    /**
     * Path represents a structural location within an expression tree.
     */
    private static final class LocationPath implements Comparable<LocationPath> {
        private final String key;
        private final int hash;

        LocationPath(List<Object> parts) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    sb.append("/");
                }
                sb.append(parts.get(i).toString());
            }
            this.key = sb.toString();
            this.hash = key.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof LocationPath) {
                return ((LocationPath) o).key.equals(this.key);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return key;
        }

        @Override
        public int compareTo(LocationPath other) {
            return this.key.compareTo(other.key);
        }
    }

    /**
     * Signature for result grouping based on structural similarity.
     */
    private static class ResultSignature {
        private final String type;
        private final Object urlStructure;
        private final Object headersStructure;
        private final Object propertiesStructure;
        private final int hashCode;

        ResultSignature(Rule result) {
            this.type = result instanceof EndpointRule ? "endpoint" : "error";

            if (result instanceof EndpointRule) {
                Endpoint ep = ((EndpointRule) result).getEndpoint();
                this.urlStructure = buildExpressionStructure(ep.getUrl());
                this.headersStructure = buildHeadersStructure(ep.getHeaders());
                this.propertiesStructure = buildPropertiesStructure(ep.getProperties());
            } else if (result instanceof ErrorRule) {
                this.urlStructure = buildExpressionStructure(((ErrorRule) result).getError());
                this.headersStructure = null;
                this.propertiesStructure = null;
            } else {
                this.urlStructure = null;
                this.headersStructure = null;
                this.propertiesStructure = null;
            }

            this.hashCode = Objects.hash(type, urlStructure, headersStructure, propertiesStructure);
        }

        private Object buildExpressionStructure(Expression expr) {
            if (expr instanceof Reference) {
                return "VAR";
            } else if (expr instanceof StringLiteral) {
                return buildTemplateStructure(((StringLiteral) expr).value());
            } else if (expr instanceof LibraryFunction) {
                return buildFunctionStructure((LibraryFunction) expr);
            } else if (expr instanceof TupleLiteral) {
                return buildTupleStructure((TupleLiteral) expr);
            } else if (expr instanceof RecordLiteral) {
                return buildRecordStructure((RecordLiteral) expr);
            } else if (expr instanceof Literal) {
                return expr.toString();
            }
            return expr.getClass().getSimpleName();
        }

        private Object buildFunctionStructure(LibraryFunction fn) {
            Map<String, Object> fnStructure = new LinkedHashMap<>();
            fnStructure.put("fn", fn.getName());
            List<Object> args = new ArrayList<>();
            for (Expression arg : fn.getArguments()) {
                args.add(buildExpressionStructure(arg));
            }
            fnStructure.put("args", args);
            return fnStructure;
        }

        private Object buildTupleStructure(TupleLiteral tuple) {
            List<Object> structure = new ArrayList<>();
            for (Literal member : tuple.members()) {
                structure.add(buildExpressionStructure(member));
            }
            return structure;
        }

        private Object buildRecordStructure(RecordLiteral record) {
            Map<String, Object> recordStructure = new LinkedHashMap<>();
            for (Map.Entry<Identifier, Literal> entry : record.members().entrySet()) {
                recordStructure.put(entry.getKey().toString(), buildExpressionStructure(entry.getValue()));
            }
            return recordStructure;
        }

        private Object buildTemplateStructure(Template template) {
            if (template.isStatic()) {
                return template.expectLiteral();
            }

            List<Object> parts = new ArrayList<>();
            for (Template.Part part : template.getParts()) {
                if (part instanceof Template.Literal) {
                    parts.add(((Template.Literal) part).getValue());
                } else if (part instanceof Template.Dynamic) {
                    parts.add(buildExpressionStructure(((Template.Dynamic) part).toExpression()));
                }
            }
            return parts;
        }

        private Object buildHeadersStructure(Map<String, List<Expression>> headers) {
            if (headers.isEmpty()) {
                return Collections.emptyMap();
            }

            // Sort header keys for deterministic ordering
            List<String> sortedKeys = new ArrayList<>(headers.keySet());
            Collections.sort(sortedKeys);

            Map<String, Object> structure = new LinkedHashMap<>();
            for (String key : sortedKeys) {
                List<Object> values = new ArrayList<>();
                for (Expression expr : headers.get(key)) {
                    values.add(buildExpressionStructure(expr));
                }
                structure.put(key, values);
            }
            return structure;
        }

        private Object buildPropertiesStructure(Map<Identifier, Literal> properties) {
            if (properties.isEmpty()) {
                return Collections.emptyMap();
            }

            // Sort property keys by their string representation for deterministic ordering
            List<Identifier> sortedIds = new ArrayList<>(properties.keySet());
            sortedIds.sort(Comparator.comparing(Identifier::toString));

            Map<String, Object> structure = new LinkedHashMap<>();
            for (Identifier id : sortedIds) {
                structure.put(id.toString(), buildExpressionStructure(properties.get(id)));
            }
            return structure;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ResultSignature)) {
                return false;
            }
            ResultSignature that = (ResultSignature) o;
            return type.equals(that.type)
                    && Objects.equals(urlStructure, that.urlStructure)
                    && Objects.equals(headersStructure, that.headersStructure)
                    && Objects.equals(propertiesStructure, that.propertiesStructure);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Group of structurally similar results.
     */
    private static class ResultGroup {
        private final List<Rule> results = new ArrayList<>();
        private Map<LocationPath, Set<String>> divergentPaths = null;

        void add(Rule result) {
            results.add(result);
            divergentPaths = null; // Invalidate cache
        }

        Map<LocationPath, Set<String>> getDivergentPaths() {
            if (divergentPaths == null) {
                divergentPaths = computeDivergentByPath();
            }
            return divergentPaths;
        }

        int getDivergentPathCount() {
            return getDivergentPaths().size();
        }

        private Map<LocationPath, Set<String>> computeDivergentByPath() {
            Map<LocationPath, Set<String>> byPath = new LinkedHashMap<>();

            // Collect references by path for URL only (not headers/properties)
            for (Rule result : results) {
                if (result instanceof EndpointRule) {
                    Endpoint ep = ((EndpointRule) result).getEndpoint();
                    collectRefsByPath(ep.getUrl(), new ArrayList<>(), byPath);
                } else if (result instanceof ErrorRule) {
                    Expression error = ((ErrorRule) result).getError();
                    collectRefsByPath(error, new ArrayList<>(), byPath);
                }
            }

            // Remove paths with only one variable name (no divergence)
            byPath.entrySet().removeIf(e -> e.getValue().size() <= 1);

            return byPath;
        }

        private void collectRefsByPath(Expression expr, List<Object> path, Map<LocationPath, Set<String>> out) {
            if (expr instanceof StringLiteral) {
                collectTemplateRefs((StringLiteral) expr, path, out);
            } else if (expr instanceof Reference) {
                LocationPath p = new LocationPath(path);
                out.computeIfAbsent(p, k -> new LinkedHashSet<>()).add(((Reference) expr).getName().toString());
            } else if (expr instanceof LibraryFunction) {
                collectFunctionRefs((LibraryFunction) expr, path, out);
            } else {
                throw new UnsupportedOperationException("Unexpected URL or error type: " + expr);
            }
        }

        private void collectTemplateRefs(StringLiteral str, List<Object> path, Map<LocationPath, Set<String>> out) {
            Template template = str.value();
            int i = 0;
            for (Template.Part part : template.getParts()) {
                if (part instanceof Template.Dynamic) {
                    List<Object> newPath = new ArrayList<>(path);
                    newPath.add("T");
                    newPath.add(i);
                    collectRefsByPath(((Template.Dynamic) part).toExpression(), newPath, out);
                }
                i++;
            }
        }

        private void collectFunctionRefs(LibraryFunction fn, List<Object> path, Map<LocationPath, Set<String>> out) {
            int i = 0;
            for (Expression arg : fn.getArguments()) {
                List<Object> newPath = new ArrayList<>(path);
                newPath.add("F");
                newPath.add(fn.getName());
                newPath.add(i++);
                collectRefsByPath(arg, newPath, out);
            }
        }
    }

    /**
     * Signature for node deduplication during construction.
     */
    private static final class NodeSignature {
        private final ConditionReference condition;
        private final CfgNode trueBranch;
        private final CfgNode falseBranch;
        private final int hashCode;

        NodeSignature(ConditionReference condition, CfgNode trueBranch, CfgNode falseBranch) {
            this.condition = condition;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
            this.hashCode = Objects.hash(
                    condition,
                    System.identityHashCode(trueBranch),
                    System.identityHashCode(falseBranch));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NodeSignature)) {
                return false;
            }
            NodeSignature that = (NodeSignature) o;
            return Objects.equals(condition, that.condition)
                    && trueBranch == that.trueBranch
                    && falseBranch == that.falseBranch;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
