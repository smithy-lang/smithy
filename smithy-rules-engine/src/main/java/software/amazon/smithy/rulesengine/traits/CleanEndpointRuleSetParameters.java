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
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.TestEvaluator;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
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
        List<Rule> retainedRules = pruneRules(ruleSet.getRules(), orphanedParams);

        // Remove orphaned parameter declarations.
        Parameters.Builder paramsBuilder = Parameters.builder();
        for (Parameter parameter : ruleSet.getParameters()) {
            if (!orphanedParams.contains(parameter.getName().toString())) {
                paramsBuilder.addParameter(parameter);
            }
        }

        EndpointRuleSet updatedRuleSet = ruleSet.toBuilder()
                .parameters(paramsBuilder.build())
                .rules(retainedRules)
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
            EndpointTestsTrait cleanedTests = cleanEndpointTests(testsTrait, orphanedParams, updatedRuleSet);
            serviceBuilder.addTrait(cleanedTests);
        }

        return serviceBuilder.build();
    }

    private List<Rule> pruneRules(List<Rule> rules, Set<String> orphanedParams) {
        List<Rule> result = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            Rule retainedRule = pruneRule(rule, orphanedParams);
            if (retainedRule != null) {
                result.add(retainedRule);
            }
        }
        return result;
    }

    private Rule pruneRule(Rule rule, Set<String> orphanedParams) {
        // Conditions gate every rule type, a condition referencing an orphaned parameter drops the
        // rule (and, for a tree rule, its whole subtree).
        if (conditionsReferenceOrphanedParam(rule.getConditions(), orphanedParams)) {
            return null;
        }

        if (rule instanceof TreeRule) {
            List<Rule> retainedChildren = pruneRules(((TreeRule) rule).getRules(), orphanedParams);
            // Drop the tree rule if all of its children were pruned.
            if (retainedChildren.isEmpty()) {
                return null;
            }
            // Always rebuild with the surviving children rather than trying to detect "unchanged"
            // and reuse the original.
            return Rule.builder(rule)
                    .conditions(rule.getConditions())
                    .description(rule.getDocumentation().orElse(null))
                    .treeRule(retainedChildren);
        }

        // A leaf rule can also reference a parameter in its value: an endpoint rule's URL, headers,
        // or properties, or an error rule's error expression.
        if (leafValueReferencesOrphanedParam(rule, orphanedParams)) {
            return null;
        }

        return rule;
    }

    private boolean leafValueReferencesOrphanedParam(Rule rule, Set<String> orphanedParams) {
        if (rule instanceof EndpointRule) {
            Endpoint endpoint = ((EndpointRule) rule).getEndpoint();
            if (referencesOrphanedParam(endpoint.getUrl().getReferences(), orphanedParams)) {
                return true;
            }
            for (List<Expression> headerValues : endpoint.getHeaders().values()) {
                for (Expression headerValue : headerValues) {
                    if (referencesOrphanedParam(headerValue.getReferences(), orphanedParams)) {
                        return true;
                    }
                }
            }
            for (Literal property : endpoint.getProperties().values()) {
                if (referencesOrphanedParam(property.getReferences(), orphanedParams)) {
                    return true;
                }
            }
        } else if (rule instanceof ErrorRule) {
            return referencesOrphanedParam(((ErrorRule) rule).getError().getReferences(), orphanedParams);
        }
        return false;
    }

    private boolean conditionsReferenceOrphanedParam(List<Condition> conditions, Set<String> orphanedParams) {
        for (Condition condition : conditions) {
            if (referencesOrphanedParam(condition.getFunction().getReferences(), orphanedParams)) {
                return true;
            }
        }
        return false;
    }

    private boolean referencesOrphanedParam(Set<String> references, Set<String> orphanedParams) {
        for (String reference : references) {
            if (orphanedParams.contains(reference)) {
                return true;
            }
        }
        return false;
    }

    private EndpointTestsTrait cleanEndpointTests(
            EndpointTestsTrait testsTrait,
            Set<String> orphanedParams,
            EndpointRuleSet updatedRuleSet
    ) {
        List<EndpointTestCase> cleanedCases = new ArrayList<>();
        for (EndpointTestCase testCase : testsTrait.getTestCases()) {
            // Static check: drop test cases that explicitly set an orphaned parameter.
            if (testCaseReferencesOrphanedParam(testCase, orphanedParams)) {
                continue;
            }
            // Dynamic check: a test case can also rely on an orphaned parameter implicitly.
            if (testCasePasses(updatedRuleSet, testCase)) {
                cleanedCases.add(testCase);
            }
        }
        return testsTrait.toBuilder().testCases(cleanedCases).build();
    }

    private boolean testCasePasses(EndpointRuleSet ruleSet, EndpointTestCase testCase) {
        try {
            TestEvaluator.evaluate(ruleSet, testCase);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
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
