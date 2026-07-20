/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.transforms.CompileBdd;
import software.amazon.smithy.utils.ListUtils;

public class CleanEndpointRuleSetParametersTest {
    private static final ShapeId SERVICE_ID = ShapeId.from("smithy.example#EndpointService");
    private static final ShapeId OPERATION_A = ShapeId.from("smithy.example#OperationA");
    private static final ShapeId OPERATION_B = ShapeId.from("smithy.example#OperationB");

    @Test
    public void removesOrphanedParametersAndRulesWhenOperationRemoved() {
        Model model = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-test-model.smithy"))
                .assemble()
                .unwrap();
        Model transformed = ModelTransformer.create()
                .filterShapes(model, shape -> !shape.getId().equals(OPERATION_A));

        assertFalse(transformed.getShape(OPERATION_A).isPresent());

        Set<String> paramNames = getRuleSetParameterNames(transformed);

        // OperationType and StreamARN were only bound by OperationA — they must be removed.
        assertFalse(paramNames.contains("OperationType"));
        assertFalse(paramNames.contains("StreamARN"));

        // Region (clientContextParams) and endpoint (builtIn) must be retained.
        assertTrue(paramNames.contains("Region"));
        assertTrue(paramNames.contains("endpoint"));

        // Rules referencing orphaned params must be pruned.
        List<Rule> rules = getRules(transformed);
        // Original had 5 rules, the OperationType rule and StreamARN rule should be removed.
        assertEquals(3, rules.size());
    }

    @Test
    public void retainsAllParametersWhenUnrelatedOperationRemoved() {
        Model model = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-test-model.smithy"))
                .assemble()
                .unwrap();
        Model transformed = ModelTransformer.create()
                .filterShapes(model, shape -> !shape.getId().equals(OPERATION_B));

        assertFalse(transformed.getShape(OPERATION_B).isPresent());

        // OperationB binds no endpoint parameters, so nothing should change.
        Set<String> paramNames = getRuleSetParameterNames(transformed);
        assertTrue(paramNames.contains("Region"));
        assertTrue(paramNames.contains("OperationType"));
        assertTrue(paramNames.contains("StreamARN"));
        assertTrue(paramNames.contains("endpoint"));

        List<Rule> rules = getRules(transformed);
        assertEquals(5, rules.size());
    }

    @Test
    public void retainsParameterWhileStillBoundByAnotherOperation() {
        Model sharedModel = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-shared-model.smithy"))
                .assemble()
                .unwrap();
        ShapeId serviceId = ShapeId.from("smithy.example#SharedEndpointService");
        ModelTransformer transformer = ModelTransformer.create();

        // Remove only OperationA: OperationB still binds SharedParam, so it must be retained.
        Model afterARemoved = transformer.filterShapes(sharedModel, shape -> !shape.getId().equals(OPERATION_A));
        EndpointRuleSet ruleSetAfterARemoved = afterARemoved.expectShape(serviceId, ServiceShape.class)
                .expectTrait(EndpointRuleSetTrait.class)
                .getEndpointRuleSet();
        assertTrue(getRuleSetParameterNames(ruleSetAfterARemoved).contains("SharedParam"));
        assertTrue(endpointUrlExists(ruleSetAfterARemoved.getRules(), "https://{SharedParam}.example.com"));

        // Remove both operations: SharedParam is now orphaned and must be removed.
        Model afterBothRemoved = transformer.filterShapes(sharedModel,
                shape -> !shape.getId().equals(OPERATION_A) && !shape.getId().equals(OPERATION_B));
        EndpointRuleSet ruleSetAfterBothRemoved = afterBothRemoved.expectShape(serviceId, ServiceShape.class)
                .expectTrait(EndpointRuleSetTrait.class)
                .getEndpointRuleSet();
        assertFalse(getRuleSetParameterNames(ruleSetAfterBothRemoved).contains("SharedParam"));
        assertFalse(endpointUrlExists(ruleSetAfterBothRemoved.getRules(), "https://{SharedParam}.example.com"));
    }

    @Test
    public void removesTestCasesReferencingOrphanedParams() {
        Model model = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-test-model.smithy"))
                .assemble()
                .unwrap();
        Model transformed = ModelTransformer.create()
                .filterShapes(model, shape -> !shape.getId().equals(OPERATION_A));

        ServiceShape service = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        assertTrue(service.hasTrait(EndpointTestsTrait.class));

        EndpointTestsTrait testsTrait = service.expectTrait(EndpointTestsTrait.class);
        // Original had 4 test cases, the OperationType and StreamARN ones should be removed.
        // The endpoint override and default regional cases reference no orphaned params and survive.
        assertEquals(2, testsTrait.getTestCases().size());
    }

    @Test
    public void transformedModelPassesValidation() {
        Model model = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-test-model.smithy"))
                .assemble()
                .unwrap();
        ModelTransformer transformer = ModelTransformer.create();
        Model transformed = transformer.filterShapes(model, shape -> !shape.getId().equals(OPERATION_A));
        transformed = transformer.removeUnreferencedShapes(transformed);

        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addModel(transformed)
                .assemble();

        result.getValidationEvents(Severity.ERROR)
                .forEach(event -> assertFalse(event.getId().startsWith("RuleSetParameter")));
        assertFalse(result.isBroken());
    }

    @Test
    public void prunesDeeplyNestedLeafReferencingOrphanedParam() {
        Model nestedModel = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-nested-model.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model transformed = transformer.filterShapes(nestedModel, shape -> !shape.getId().equals(OPERATION_A));

        ServiceShape service = transformed.expectShape(
                ShapeId.from("smithy.example#NestedEndpointService"),
                ServiceShape.class);
        EndpointRuleSet ruleSet = service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();

        Set<String> paramNames = getParameterNames(ruleSet.getParameters());

        assertFalse(paramNames.contains("StreamARN"));
        assertFalse(paramNames.contains("OperationType"));
        assertTrue(paramNames.contains("Region"));
        assertTrue(paramNames.contains("Bucket"));
        assertTrue(paramNames.contains("endpoint"));

        // The deep StreamARN leaf must be pruned, its non-referencing siblings must survive.
        assertFalse(endpointUrlExists(ruleSet.getRules(), "https://stream.example.com"));
        assertTrue(endpointUrlExists(ruleSet.getRules(), "https://special.{Region}.example.com"));
        assertTrue(endpointUrlExists(ruleSet.getRules(), "https://bucket.{Region}.example.com"));

        // Empty-tree collapse
        assertFalse(endpointUrlExists(ruleSet.getRules(), "https://control.example.com"));
        assertFalse(endpointUrlExists(ruleSet.getRules(), "https://data.example.com"));

        // The cleaned model must validate.
        Model pruned = transformer.removeUnreferencedShapes(transformed);
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addModel(pruned)
                .assemble();
        result.getValidationEvents(Severity.ERROR)
                .forEach(event -> assertFalse(event.getId().startsWith("RuleSetParameter")));
        assertFalse(result.isBroken());
    }

    @Test
    public void recompilesBddParametersWhenOperationRemoved() {
        Model model = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-test-model.smithy"))
                .assemble()
                .unwrap();

        Model withBdd = addCompiledBdd(model);

        Model transformed = ModelTransformer.create()
                .filterShapes(withBdd, shape -> !shape.getId().equals(OPERATION_A));

        ServiceShape service = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        assertTrue(service.hasTrait(EndpointBddTrait.class));

        Set<String> bddParams = getParameterNames(service.expectTrait(EndpointBddTrait.class).getParameters());
        // The same orphans removed from the ruleset must be gone from the BDD too.
        assertFalse(bddParams.contains("OperationType"));
        assertFalse(bddParams.contains("StreamARN"));
        assertTrue(bddParams.contains("Region"));
        assertTrue(bddParams.contains("endpoint"));

        // BDD and ruleset parameter sets must be identical after cleaning.
        assertEquals(getRuleSetParameterNames(transformed), bddParams);
    }

    @Test
    public void bddBearingModelPassesValidationAfterRemoval() {
        Model model = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addImport(CleanEndpointRuleSetParametersTest.class.getResource(
                        "clean-ruleset-params-test-model.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model withBdd = addCompiledBdd(model);
        Model transformed = transformer.filterShapes(withBdd, shape -> !shape.getId().equals(OPERATION_A));
        transformed = transformer.removeUnreferencedShapes(transformed);

        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(CleanEndpointRuleSetParametersTest.class.getClassLoader())
                .addModel(transformed)
                .assemble();

        result.getValidationEvents(Severity.ERROR)
                .forEach(event -> assertFalse(event.getId().startsWith("RuleSetParameter")));
        assertFalse(result.isBroken());
    }

    private static Model addCompiledBdd(Model source) {
        ServiceShape service = source.expectShape(SERVICE_ID, ServiceShape.class);
        EndpointRuleSet ruleSet = service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        ServiceShape withBdd = service.toBuilder()
                .addTrait(CompileBdd.compileBdd(ruleSet))
                .build();
        return ModelTransformer.create().replaceShapes(source, ListUtils.of(withBdd));
    }

    private static Set<String> getRuleSetParameterNames(Model transformed) {
        ServiceShape service = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        return getParameterNames(service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet().getParameters());
    }

    private static Set<String> getRuleSetParameterNames(EndpointRuleSet ruleSet) {
        return getParameterNames(ruleSet.getParameters());
    }

    private static List<Rule> getRules(Model transformed) {
        ServiceShape service = transformed.expectShape(SERVICE_ID, ServiceShape.class);
        EndpointRuleSet ruleSet = service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        return ruleSet.getRules();
    }

    private static Set<String> getParameterNames(Iterable<Parameter> parameters) {
        Set<String> names = new HashSet<>();
        for (Parameter parameter : parameters) {
            names.add(parameter.getName().toString());
        }
        return names;
    }

    private static boolean endpointUrlExists(List<Rule> rules, String url) {
        for (Rule rule : rules) {
            if (rule instanceof TreeRule) {
                if (endpointUrlExists(((TreeRule) rule).getRules(), url)) {
                    return true;
                }
            } else if (rule.toString().contains(url)) {
                return true;
            }
        }
        return false;
    }
}
