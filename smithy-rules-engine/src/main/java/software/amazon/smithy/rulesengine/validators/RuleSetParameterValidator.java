/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.traits.ClientContextParamDefinition;
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestOperationInput;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamDefinition;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validator for rule-set parameters.
 */
public final class RuleSetParameterValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        List<ValidationEvent> errors = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            // Pull all the parameters used in this service related to endpoints, validating that
            // they are of matching types across the traits that can define them.
            Pair<List<ValidationEvent>, Map<String, Parameter>> errorsParamsPair = validateAndExtractParameters(
                    model, serviceShape, topDownIndex.getContainedOperations(serviceShape));
            errors.addAll(errorsParamsPair.getLeft());

            // Make sure parameters align across Params <-> RuleSet transitions.
            EndpointRuleSet ruleSet = serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
            errors.addAll(validateParametersMatching(serviceShape, ruleSet.getParameters(),
                    errorsParamsPair.getRight()));

            // Check that tests declare required parameters, only defined parameters, etc.
            if (serviceShape.hasTrait(EndpointTestsTrait.class)) {
                errors.addAll(validateTestsParameters(model, topDownIndex, serviceShape,
                        serviceShape.expectTrait(EndpointTestsTrait.class), ruleSet));
            }
        }

        return errors;
    }

    private Pair<List<ValidationEvent>, Map<String, Parameter>> validateAndExtractParameters(
            Model model,
            ServiceShape serviceShape,
            Set<OperationShape> containedOperations
    ) {
        List<ValidationEvent> errors = new ArrayList<>();
        Map<String, Parameter> endpointParams = new HashMap<>();

        if (serviceShape.hasTrait(ClientContextParamsTrait.class)) {
            ClientContextParamsTrait trait = serviceShape.expectTrait(ClientContextParamsTrait.class);
            for (Map.Entry<String, ClientContextParamDefinition> entry : trait.getParameters().entrySet())  {
                endpointParams.put(entry.getKey(), Parameter.builder()
                        .name(entry.getKey())
                        .type(ParameterType.fromShapeType(entry.getValue().getType()))
                        .build());
            }
        }

        for (OperationShape operationShape : containedOperations) {
            if (operationShape.hasTrait(StaticContextParamsTrait.ID)) {
                StaticContextParamsTrait trait = operationShape.expectTrait(StaticContextParamsTrait.class);
                for (Map.Entry<String, StaticContextParamDefinition> entry : trait.getParameters().entrySet()) {
                    String name = entry.getKey();
                    ParameterType parameterType = ParameterType.fromNode(entry.getValue().getValue());

                    if (endpointParams.containsKey(name) && endpointParams.get(name).getType() != parameterType) {
                        errors.add(parameterError(operationShape, trait, "StaticContextParams.InconsistentType",
                                String.format("Inconsistent type for `%s` parameter", name)));
                    } else {
                        endpointParams.put(name, Parameter.builder()
                                .name(name)
                                .value(entry.getValue().getValue())
                                .type(parameterType)
                                .build());
                    }
                }
            }

            StructureShape input = model.expectShape(operationShape.getInputShape(), StructureShape.class);
            for (MemberShape memberShape : input.members()) {
                if (memberShape.hasTrait(ContextParamTrait.class)) {
                    ContextParamTrait trait = memberShape.expectTrait(ContextParamTrait.class);
                    String name = trait.getName();
                    Shape targetType = model.expectShape(memberShape.getTarget());

                    if (!targetType.isStringShape() && !targetType.isBooleanShape()) {
                        errors.add(parameterError(memberShape, trait, "ContextParam.UnsupportedType",
                                String.format("Unsupported type `%s` for `%s` parameter", targetType, name)));
                    } else {
                        ParameterType type = targetType.isStringShape() ? ParameterType.STRING : ParameterType.BOOLEAN;

                        if (endpointParams.containsKey(name) && type != endpointParams.get(name).getType()) {
                            errors.add(parameterError(memberShape, trait, "ContextParam.InconsistentType",
                                    String.format("Inconsistent type for `%s` parameter", name)));
                        } else {
                            endpointParams.put(name, Parameter.builder()
                                    .name(name)
                                    .type(type)
                                    .sourceLocation(trait)
                                    .build());
                        }
                    }
                }
            }
        }

        return Pair.of(errors, endpointParams);
    }

    private List<ValidationEvent> validateParametersMatching(
            ServiceShape serviceShape,
            Parameters ruleSetParams,
            Map<String, Parameter> modelParams
    ) {
        List<ValidationEvent> errors = new ArrayList<>();
        Set<String> matchedParams = new HashSet<>();
        for (Parameter parameter : ruleSetParams) {
            String name = parameter.getName().toString();

            // Don't worry about checking built-in based parameters.
            if (parameter.isBuiltIn()) {
                matchedParams.add(name);
                continue;
            }

            if (!modelParams.containsKey(name)) {
                errors.add(parameterError(serviceShape, parameter, "RuleSet.UnmatchedName",
                        String.format("Parameter `%s` exists in ruleset but not in service model, existing params: %s",
                                name, String.join(", ", modelParams.keySet()))));
            } else {
                matchedParams.add(name);
                if (parameter.getType() != modelParams.get(name).getType()) {
                    errors.add(parameterError(serviceShape, parameter, "RuleSet.TypeMismatch",
                            String.format("Type mismatch for parameter `%s`", name)));
                }
            }
        }

        for (Map.Entry<String, Parameter> entry : modelParams.entrySet()) {
            if (!matchedParams.contains(entry.getKey())) {
                errors.add(parameterError(serviceShape, serviceShape.expectTrait(EndpointRuleSetTrait.class),
                        "RuleSet.UnmatchedName",
                        String.format("Parameter `%s` exists in service model but not in ruleset, existing params: %s",
                                entry.getKey(), matchedParams)));
            }
        }

        return errors;
    }

    private List<ValidationEvent> validateTestsParameters(
            Model model,
            TopDownIndex topDownIndex,
            ServiceShape serviceShape,
            EndpointTestsTrait trait,
            EndpointRuleSet ruleSet
    ) {
        List<ValidationEvent> errors = new ArrayList<>();
        Set<String> rulesetParamNames = new HashSet<>();
        Map<String, List<Parameter>> testSuiteParams = extractTestSuiteParameters(model, topDownIndex,
                serviceShape, ruleSet, trait);

        for (Parameter parameter : ruleSet.getParameters()) {
            String name = parameter.getName().toString();
            rulesetParamNames.add(name);
            boolean testSuiteHasParam = testSuiteParams.containsKey(name);

            // All test parameter types from corresponding ruleset parameters must match in all test cases.
            if (!testSuiteHasParam) {
                errors.add(danger(serviceShape, parameter,
                                String.format("Parameter `%s` is never used in an `EndpointTests` test case", name))
                        .toBuilder()
                        .id(getName() + ".TestCase.Unused").build());
            } else {
                for (Parameter testParam : testSuiteParams.get(name)) {
                    if (testParam.getType() != parameter.getType()) {
                        errors.add(parameterError(serviceShape, testParam, "TestCase.TypeMismatch",
                                String.format("Type mismatch for parameter `%s`, `%s` expected",
                                testParam.getName().toString(), parameter.getType())));
                    }
                }
            }

            // All required params from a ruleset must be present in all test cases.
            if (parameter.isRequired() && !parameter.getDefault().isPresent()
                    && (!testSuiteHasParam || testSuiteParams.get(name).size() != trait.getTestCases().size())
            ) {
                errors.add(parameterError(serviceShape, parameter, "TestCase.RequiredMissing",
                        String.format("Required parameter `%s` is missing in at least one test case", name)));
            }
        }

        // There might be parameters in test cases not defined in a ruleset.
        for (Map.Entry<String, List<Parameter>> entry : testSuiteParams.entrySet()) {
            if (!rulesetParamNames.contains(entry.getKey())) {
                errors.add(parameterError(serviceShape, entry.getValue().get(0), "TestCase.Undefined",
                        String.format("Test parameter `%s` is not defined in ruleset", entry.getKey())));
            }
        }

        return errors;
    }

    private Map<String, List<Parameter>> extractTestSuiteParameters(
            Model model,
            TopDownIndex topDownIndex,
            ServiceShape serviceShape,
            EndpointRuleSet ruleSet,
            EndpointTestsTrait trait
    ) {
        Map<String, List<Parameter>> params = new HashMap<>();
        for (EndpointTestCase testCase : trait.getTestCases()) {
            List<Parameter> testParams = new ArrayList<>();
            for (Map.Entry<String, Node> entry : testCase.getParams().getStringMap().entrySet()) {
                testParams.add(buildParameter(entry));
            }

            for (EndpointTestOperationInput input : testCase.getOperationInputs()) {
                // These are easy, they're defined to match the clientContextParams.
                for (Map.Entry<String, Node> entry : input.getClientParams().getStringMap().entrySet()) {
                    testParams.add(buildParameter(entry));
                }

                // A little indirection here, since only parameters tracks the name of the built-in.
                for (Map.Entry<String, Node> entry : input.getBuiltInParams().getStringMap().entrySet()) {
                    for (Parameter parameter : ruleSet.getParameters()) {
                        if (parameter.isBuiltIn() && parameter.getBuiltIn().get().equals(entry.getKey())) {
                            testParams.add(buildParameter(entry, parameter.getName().toString()));
                        }
                    }
                }

                // Lots of indirection, the tests define operation inputs based on the name of the input
                // member, but the map here needs to use the value of the parameter via contextParam.
                for (OperationShape operationShape : topDownIndex.getContainedOperations(serviceShape)) {
                    if (operationShape.getId().getName().equals(input.getOperationName())) {
                        StructureShape inputShape = model.expectShape(
                                operationShape.getInputShape(), StructureShape.class);
                        for (String name : inputShape.getMemberNames()) {
                            MemberShape memberShape = inputShape.getMember(name).get();
                            if (input.getOperationParams().containsMember(name)
                                    && memberShape.hasTrait(ContextParamTrait.class)
                            ) {
                                String paramName = memberShape.expectTrait(ContextParamTrait.class).getName();
                                testParams.add(buildParameter(paramName,
                                        input.getOperationParams().expectMember(name)));
                            }
                        }
                        break;
                    }
                }
            }

            for (Parameter parameter : testParams) {
                params.merge(parameter.getName().toString(), new ArrayList<>(ListUtils.of(parameter)), this::merge);
            }
        }

        return params;
    }

    private Parameter buildParameter(Map.Entry<String, Node> entry) {
        return buildParameter(entry.getKey(), entry.getValue());
    }

    private Parameter buildParameter(Map.Entry<String, Node> entry, String name) {
        return buildParameter(name, entry.getValue());
    }

    private Parameter buildParameter(String name, Node node) {
        Value value = Value.fromNode(node);
        return Parameter.builder()
                       .sourceLocation(value)
                       .name(name)
                       .value(value)
                       .type(ParameterType.fromType(value.getType()))
                       .build();
    }

    private List<Parameter> merge(List<Parameter> previousList, List<Parameter> newList) {
        previousList.addAll(newList);
        return previousList;
    }

    private ValidationEvent parameterError(
            Shape shape,
            FromSourceLocation sourceLocation,
            String id,
            String message
    ) {
        return error(shape, sourceLocation, message).toBuilder().id(getName() + "." + id).build();
    }
}
