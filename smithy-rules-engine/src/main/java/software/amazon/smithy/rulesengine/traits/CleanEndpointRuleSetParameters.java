/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.transforms.CompileBdd;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Removes orphaned endpoint ruleset parameters and their associated rules from a service's
 * {@code @endpointRuleSet} trait when operations are removed from the model.
 *
 * <p>When operations are removed via {@link ModelTransformer#filterShapes} or
 * {@link ModelTransformer#removeShapes}, parameters that were only bound through
 * the removed operations' {@code @staticContextParams}, {@code @operationContextParams},
 * or {@code @contextParam} traits may become orphaned. To prevent validation failures, this plugin
 * removes those orphaned parameters and prunes any rules that reference them, recompiles the
 * {@code @endpointBdd} trait from the cleaned ruleset, and drops {@code @endpointTests} cases that
 * reference them.
 */
@SmithyInternalApi
public final class CleanEndpointRuleSetParameters implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        if (!hasRelevantRemovals(removed)) {
            return model;
        }

        Set<Shape> servicesToUpdate = new HashSet<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        for (ServiceShape service : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            ServiceShape updated = cleanRuleSetParameters(model, topDownIndex, service);
            if (updated != null) {
                servicesToUpdate.add(updated);
            }
        }

        if (servicesToUpdate.isEmpty()) {
            return model;
        }
        return transformer.replaceShapes(model, servicesToUpdate);
    }

    private boolean hasRelevantRemovals(Collection<Shape> removed) {
        for (Shape shape : removed) {
            if (shape.isOperationShape() || shape.isMemberShape()) {
                return true;
            }
        }
        return false;
    }

    private ServiceShape cleanRuleSetParameters(Model model, TopDownIndex topDownIndex, ServiceShape service) {
        Set<String> boundParams = computeBoundParameters(model, topDownIndex, service);
        EndpointRuleSetTrait endpointRuleSetTrait = service.expectTrait(EndpointRuleSetTrait.class);
        EndpointRuleSet ruleSet = endpointRuleSetTrait.getEndpointRuleSet();

        Set<String> orphanedParams = new HashSet<>();
        for (Parameter parameter : ruleSet.getParameters()) {
            String name = parameter.getName().toString();
            if (!parameter.isBuiltIn() && !boundParams.contains(name)) {
                orphanedParams.add(name);
            }
        }

        if (orphanedParams.isEmpty()) {
            return null;
        }

        // Prune rules that reference orphaned parameters.
        List<Rule> prunedRules = pruneRules(ruleSet.getRules(), orphanedParams);

        // Remove orphaned parameter declarations.
        Parameters.Builder paramsBuilder = Parameters.builder();
        for (Parameter parameter : ruleSet.getParameters()) {
            if (!orphanedParams.contains(parameter.getName().toString())) {
                paramsBuilder.addParameter(parameter);
            }
        }

        EndpointRuleSet updatedRuleSet = ruleSet.toBuilder()
                .parameters(paramsBuilder.build())
                .rules(prunedRules)
                .build();

        EndpointRuleSetTrait updatedTrait = endpointRuleSetTrait.toBuilder()
                .ruleSet(updatedRuleSet.toNode())
                .build();

        ServiceShape.Builder serviceBuilder = service.toBuilder().addTrait(updatedTrait);

        // Compile BDD based on updated endpoint ruleset
        if (service.hasTrait(EndpointBddTrait.ID)) {
            serviceBuilder.addTrait(CompileBdd.compileBdd(updatedRuleSet));
        }

        // Clean endpoint tests that reference orphaned parameters.
        if (service.hasTrait(EndpointTestsTrait.ID)) {
            EndpointTestsTrait testsTrait = service.expectTrait(EndpointTestsTrait.class);
            serviceBuilder.addTrait(cleanEndpointTests(testsTrait, orphanedParams));
        }

        return serviceBuilder.build();
    }

    private List<Rule> pruneRules(List<Rule> rules, Set<String> orphanedParams) {
        List<Rule> result = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            Rule pruned = pruneRule(rule, orphanedParams);
            if (pruned != null) {
                result.add(pruned);
            }
        }
        return result;
    }

    private Rule pruneRule(Rule rule, Set<String> orphanedParams) {
        // Drop the rule if any of its conditions reference an orphaned parameter.
        if (conditionsReferenceOrphanedParam(rule.getConditions(), orphanedParams)) {
            return null;
        }

        if (rule instanceof TreeRule) {
            List<Rule> prunedChildren = pruneRules(((TreeRule) rule).getRules(), orphanedParams);
            // Drop the tree rule if all of its children were pruned.
            if (prunedChildren.isEmpty()) {
                return null;
            }
            // Always rebuild with the surviving children rather than trying to detect "unchanged"
            // and reuse the original.
            return Rule.builder(rule)
                    .conditions(rule.getConditions())
                    .description(rule.getDocumentation().orElse(null))
                    .treeRule(prunedChildren);
        }

        // Leaf rules do not need to be pruned.
        return rule;
    }

    private boolean conditionsReferenceOrphanedParam(List<Condition> conditions, Set<String> orphanedParams) {
        for (Condition condition : conditions) {
            for (String reference : condition.getFunction().getReferences()) {
                if (orphanedParams.contains(reference)) {
                    return true;
                }
            }
        }
        return false;
    }

    private EndpointTestsTrait cleanEndpointTests(EndpointTestsTrait testsTrait, Set<String> orphanedParams) {
        List<EndpointTestCase> cleanedCases = new ArrayList<>();
        for (EndpointTestCase testCase : testsTrait.getTestCases()) {
            if (!testCaseReferencesOrphanedParam(testCase, orphanedParams)) {
                cleanedCases.add(testCase);
            }
        }
        return testsTrait.toBuilder().testCases(cleanedCases).build();
    }

    private boolean testCaseReferencesOrphanedParam(EndpointTestCase testCase, Set<String> orphanedParams) {
        ObjectNode params = testCase.getParams();
        for (Map.Entry<StringNode, Node> entry : params.getMembers().entrySet()) {
            if (orphanedParams.contains(entry.getKey().getValue())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> computeBoundParameters(Model model, TopDownIndex topDownIndex, ServiceShape service) {
        Set<String> boundParams = new HashSet<>();
        if (service.hasTrait(ClientContextParamsTrait.ID)) {
            boundParams.addAll(service.expectTrait(ClientContextParamsTrait.class).getParameters().keySet());
        }
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            if (operation.hasTrait(StaticContextParamsTrait.ID)) {
                boundParams.addAll(operation.expectTrait(StaticContextParamsTrait.class).getParameters().keySet());
            }
            if (operation.hasTrait(OperationContextParamsTrait.ID)) {
                boundParams.addAll(
                        operation.expectTrait(OperationContextParamsTrait.class).getParameters().keySet());
            }

            StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);
            for (MemberShape member : input.members()) {
                if (member.hasTrait(ContextParamTrait.ID)) {
                    boundParams.add(member.expectTrait(ContextParamTrait.class).getName());
                }
            }
        }
        return boundParams;
    }
}
