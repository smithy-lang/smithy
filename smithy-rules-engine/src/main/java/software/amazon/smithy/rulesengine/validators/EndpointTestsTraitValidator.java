/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.traits.BddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestOperationInput;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates the {@link EndpointTestsTrait}.
 */
@SmithyUnstableApi
public final class EndpointTestsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            // Precompute shape ids to operations in the service.
            Map<String, OperationShape> operationNameMap = new HashMap<>();
            for (OperationShape operationShape : topDownIndex.getContainedOperations(serviceShape)) {
                operationNameMap.put(operationShape.getId().getName(), operationShape);
            }

            serviceShape.getTrait(EndpointRuleSetTrait.class).ifPresent(trait -> {
                validateEndpointRuleSet(events,
                        model,
                        serviceShape,
                        trait.getEndpointRuleSet().getParameters(),
                        operationNameMap);
            });

            serviceShape.getTrait(BddTrait.class).ifPresent(trait -> {
                validateEndpointRuleSet(events, model, serviceShape, trait.getBdd().getParameters(), operationNameMap);
            });
        }

        return events;
    }

    private void validateEndpointRuleSet(
            List<ValidationEvent> events,
            Model model,
            ServiceShape serviceShape,
            Parameters parameters,
            Map<String, OperationShape> operationNameMap
    ) {
        // Precompute the built-ins and their default states, as this will
        // be used frequently in downstream validation.
        List<Parameter> builtInParamsWithDefaults = new ArrayList<>();
        List<Parameter> builtInParamsWithoutDefaults = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if (parameter.isBuiltIn()) {
                if (parameter.getDefault().isPresent()) {
                    builtInParamsWithDefaults.add(parameter);
                } else {
                    builtInParamsWithoutDefaults.add(parameter);
                }
            }
        }

        for (EndpointTestCase testCase : serviceShape.expectTrait(EndpointTestsTrait.class).getTestCases()) {
            // If values for built-in parameters don't match the default, they MUST
            // be specified in the operation inputs. Precompute the ones that don't match
            // and capture their value.
            Map<Parameter, Node> builtInParamsWithNonDefaultValues =
                    getBuiltInParamsWithNonDefaultValues(builtInParamsWithDefaults, testCase);

            for (EndpointTestOperationInput testOperationInput : testCase.getOperationInputs()) {
                String operationName = testOperationInput.getOperationName();

                // It's possible for an operation defined to not be in the service closure.
                if (!operationNameMap.containsKey(operationName)) {
                    events.add(error(serviceShape,
                            testOperationInput,
                            String.format("Test case operation `%s` does not exist in service `%s`",
                                    operationName,
                                    serviceShape.getId())));
                    continue;
                }

                // Still emit events if the operation exists, but was just not bound.
                validateConfiguredBuiltInValues(serviceShape,
                        builtInParamsWithNonDefaultValues,
                        testOperationInput,
                        events);
                validateBuiltInsWithoutDefaultsHaveValues(serviceShape,
                        builtInParamsWithoutDefaults,
                        testCase,
                        testOperationInput,
                        events);

                StructureShape inputShape = model.expectShape(
                        operationNameMap.get(operationName).getInputShape(),
                        StructureShape.class);
                validateOperationInput(model, serviceShape, inputShape, testCase, testOperationInput, events);
            }
        }
    }

    private Map<Parameter, Node> getBuiltInParamsWithNonDefaultValues(
            List<Parameter> builtInParamsWithDefaults,
            EndpointTestCase testCase
    ) {
        Map<Parameter, Node> builtInParamsWithNonDefaultValues = new HashMap<>();
        for (Parameter parameter : builtInParamsWithDefaults) {
            String parameterName = parameter.getName().toString();
            Node defaultValue = parameter.getDefault().get().toNode();

            // Consider a parameter non-matching if the built-in's default and its
            // value in this test case aren't the same.
            if (testCase.getParams().containsMember(parameterName)
                    && !testCase.getParams().expectMember(parameterName).equals(defaultValue)) {
                builtInParamsWithNonDefaultValues.put(parameter, testCase.getParams().expectMember(parameterName));
            }
        }
        return builtInParamsWithNonDefaultValues;
    }

    private void validateConfiguredBuiltInValues(
            ServiceShape serviceShape,
            Map<Parameter, Node> builtInParamsWithNonDefaultValues,
            EndpointTestOperationInput testOperationInput,
            List<ValidationEvent> events
    ) {
        for (Map.Entry<Parameter, Node> builtInParasWithNonDefaultValue : builtInParamsWithNonDefaultValues
                .entrySet()) {
            String builtInName = builtInParasWithNonDefaultValue.getKey().getBuiltIn().get();
            // Emit if either the built-in with a non-matching value isn't
            // specified or the value set for it doesn't match.
            if (!testOperationInput.getBuiltInParams().containsMember(builtInName)
                    || !testOperationInput.getBuiltInParams()
                            .expectMember(builtInName)
                            .equals(builtInParasWithNonDefaultValue.getValue())) {
                events.add(error(serviceShape,
                        testOperationInput,
                        String.format("Test case does not supply the `%s` value for the `%s` parameter's "
                                + "`%s` built-in.",
                                Node.printJson(builtInParasWithNonDefaultValue.getValue()),
                                builtInParasWithNonDefaultValue.getKey().getNameString(),
                                builtInParasWithNonDefaultValue.getKey().getBuiltIn().get())));
            }
        }
    }

    private void validateBuiltInsWithoutDefaultsHaveValues(
            ServiceShape serviceShape,
            List<Parameter> builtInParamsWithoutDefaults,
            EndpointTestCase testCase,
            EndpointTestOperationInput testOperationInput,
            List<ValidationEvent> events
    ) {
        for (Parameter parameter : builtInParamsWithoutDefaults) {
            if (testCase.getParams().containsMember(parameter.getNameString())
                    && !testOperationInput.getBuiltInParams().containsMember(parameter.getBuiltIn().get())) {
                events.add(error(serviceShape,
                        testOperationInput,
                        String.format("Operation input does not supply a value for the `%s` built-in parameter "
                                + "and the `%s` parameter does not set a default.",
                                parameter.getBuiltIn().get(),
                                parameter.getName())));
            }
        }
    }

    private void validateOperationInput(
            Model model,
            ServiceShape serviceShape,
            StructureShape inputShape,
            EndpointTestCase testCase,
            EndpointTestOperationInput testOperationInput,
            List<ValidationEvent> events
    ) {
        NodeValidationVisitor validator = NodeValidationVisitor.builder()
                .model(model)
                .value(testOperationInput.getOperationParams())
                .eventId(getName())
                .eventShapeId(serviceShape.toShapeId())
                .startingContext("The operationInput value for an endpoint test "
                        + "does not match the operation's input shape")
                .build();

        // Error test cases may use invalid inputs as the mechanism to trigger their error,
        // so lower the severity before emitting. All other events here should be raised to
        // DANGER level as well.
        for (ValidationEvent event : inputShape.accept(validator)) {
            if (event.getSeverity() == Severity.WARNING
                    || event.getSeverity() == Severity.NOTE
                    || testCase.getExpect().getError().isPresent()) {
                events.add(event.toBuilder().severity(Severity.DANGER).build());
            } else {
                events.add(event);
            }
        }
    }
}
