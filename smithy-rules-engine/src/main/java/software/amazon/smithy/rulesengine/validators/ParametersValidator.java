/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.rulesengine.language.EndpointRuleset;
import software.amazon.smithy.rulesengine.language.EndpointTestSuite;
import software.amazon.smithy.rulesengine.language.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.lang.parameters.ParameterType;
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamDefinition;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;

public final class ParametersValidator {

    private ParametersValidator() {
    }

    // validates ruleset-model parameters matching
    // returns set of validation errors, empty if no errors
    public static List<ValidationError> validateParametersMatching(
            Map<String, Parameter> rulesetParams, Map<String, Parameter> modelParams
    ) {
        ArrayList<ValidationError> errors = new ArrayList<>();
        HashSet<String> matchedParams = new HashSet<>();

        for (Map.Entry<String, Parameter> entry: rulesetParams.entrySet()) {
            String name = entry.getKey();
            Parameter modelParam = modelParams.getOrDefault(name, null);
            if (modelParam == null) {
                String error = String.format("Parameter '%s' exists in ruleset but not in service model", name);
                errors.add(new ValidationError(ValidationErrorType.PARAMETER_MISMATCH, error,
                        entry.getValue().getSourceLocation()));
            } else {
                matchedParams.add(name);
                if (entry.getValue().getType() != modelParam.getType()) {
                    String error = String.format("Type mismatch for parameter '%s'", name);
                    errors.add(new ValidationError(ValidationErrorType.PARAMETER_TYPE_MISMATCH, error,
                            entry.getValue().getSourceLocation()));
                }
            }
        }

        for (Map.Entry<String, Parameter> entry : modelParams.entrySet()) {
            String name = entry.getKey();
            if (!matchedParams.contains(name)) {
                String error = String.format("Parameter '%s' exists in service model but not in ruleset", name);
                errors.add(new ValidationError(ValidationErrorType.PARAMETER_MISMATCH, error,
                        entry.getValue().getSourceLocation()));
            }
        }

        return errors;
    }

    // Validate for
    //
    // @endpointParams and endpointParam combined must have consistent value types for same parameter across the model.
    // We can have multiple operations/inputs sharing same parameter's name.
    // @endpointParam can only be applied to a members of string or boolean types
    //
    // returns set of validation errors and map of parameters extracted from model, empty if no errors
    // fills map of endpoint parameters extracted from the model
    public static List<ValidationError> validateModelAndExtractParameters(
            Model model, ShapeId serviceShape, Map<String, Parameter> endpointParams
    ) {
        ArrayList<ValidationError> errors = new ArrayList<ValidationError>();
        // TODO: is this validator still even right?
        ServiceShape service = model.expectShape(serviceShape, ServiceShape.class);
        Set<ShapeId> ops = service.getAllOperations();

        service.getTrait(ClientContextParamsTrait.class).ifPresent(t -> {

        });

        ops.forEach(shapeId -> {

            OperationShape operation = model.expectShape(shapeId, OperationShape.class);

            operation.getTrait(StaticContextParamsTrait.class).ifPresent(t -> {
                List<Parameter> params = t.getParameters().entrySet().stream()
                        .map(entry -> {
                            StaticContextParamDefinition definition = entry.getValue();
                            Parameter.Builder builder = Parameter.builder()
                                    .name(entry.getKey())
                                    .value(definition.getValue());
                            switch (definition.getValue().getType()) {
                                case STRING:
                                    builder.type(ParameterType.STRING);
                                    break;
                                case BOOLEAN:
                                    builder.type(ParameterType.BOOLEAN);
                                    break;
                                default:
                                    throw new IllegalArgumentException(String.format(
                                            "invalid parameter value type: `%s`", definition.getValue().getType()));
                            }
                            return builder.build();
                        })
                        .collect(Collectors.toList());

                params.forEach(p -> {
                    String name = p.getName().asString();
                    Parameter param = endpointParams.getOrDefault(name, null);
                    if (param != null && param.getType() != p.getType()) {
                        String error = String.format("Inconsistent type for '%s' parameter", name);
                        errors.add(new ValidationError(ValidationErrorType.INCONSISTENT_PARAMETER_TYPE, error,
                                t.getSourceLocation()));
                    } else {
                        endpointParams.put(name, p);
                    }
                });
            });

            operation.getInput().ifPresent(input -> {
                StructureShape inputShape = model.expectShape(input, StructureShape.class);
                inputShape.members().forEach(member -> member.getTrait(ContextParamTrait.class).ifPresent(t -> {
                    String name = t.getName();

                    Shape targetType = model.expectShape(member.getTarget());
                    if (!targetType.isStringShape() && !targetType.isBooleanShape()) {
                        String error = String.format("Unsupported type %s for '%s' parameter", targetType, name);
                        errors.add(new ValidationError(ValidationErrorType.UNSUPPORTED_PARAMETER_TYPE, error,
                                t.getSourceLocation()));
                    } else {
                        ParameterType type = targetType.isStringShape() ? ParameterType.STRING : ParameterType.BOOLEAN;

                        Parameter existingParam = endpointParams.getOrDefault(name, null);
                        if (existingParam != null && type != existingParam.getType()) {
                            String error = String.format("Inconsistent type for '%s' parameter", name);
                            errors.add(new ValidationError(ValidationErrorType.INCONSISTENT_PARAMETER_TYPE, error,
                                    t.getSourceLocation()));
                        } else {
                            Parameter param = Parameter
                                    .builder()
                                    .name(name)
                                    .type(type)
                                    .sourceLocation(t.getSourceLocation())
                                    .build();
                            endpointParams.put(name, param);
                        }
                    }
                }));
            });
        });

        return errors;
    }

    // validates model for correct endpoint parameters annotations, then validates ruleset-model parameters matching
    // returns set of validation errors, empty if no errors
    public static List<ValidationError> validateParameters(
            EndpointRuleset ruleset, ShapeId serviceShape, Model smithyModel
    ) {

        Map<String, Parameter> rulesetParams = ruleset.getParameters().toList().stream()
                .filter(p -> !p.isBuiltIn())
                .collect(Collectors.toMap(k -> k.getName().asString(), item -> item));

        HashMap<String, Parameter> modelParams = new HashMap<String, Parameter>();

        List<ValidationError> errors = ParametersValidator.validateModelAndExtractParameters(smithyModel, serviceShape,
                modelParams);

        if (!errors.isEmpty()) {
            return errors;
        }

        List<ValidationError> matchingErrors = ParametersValidator.validateParametersMatching(rulesetParams,
                modelParams);

        if (!matchingErrors.isEmpty()) {
            return matchingErrors;
        }

        return new ArrayList<>();
    }

    /**
     * Validates ruleset-tests parameters matching/validity.
     *
     * @param tests   the endpoint test suite
     * @param ruleset the rule set to be tested
     * @return set of validation errors if present.
     */
    public static List<ValidationError> validateTestsParameters(EndpointTestSuite tests, EndpointRuleset ruleset) {

        ArrayList<ValidationError> errors = new ArrayList<ValidationError>();

        Map<String, List<Parameter>> testSuiteParams = extractTestSuiteParameters(tests);

        // all required params from ruleset must be present in all test cases
        List<Parameter> requiredRulesetParams = ruleset.getParameters().toList().stream()
                .filter(Parameter::isRequired)
                .collect(Collectors.toList());
        requiredRulesetParams.forEach(rp -> {
            String name = rp.getName().asString();
            if (!testSuiteParams.containsKey(name) || testSuiteParams.get(name).size() != tests.getTestCases().size()) {
                String error = String.format("Required parameter '%s' is missing in at least one test case", name);
                errors.add(new ValidationError(ValidationErrorType.REQUIRED_PARAMETER_MISSING, error,
                        rp.getSourceLocation()));
            }
        });

        // all test parameter types from corresponding ruleset parameters must match in all test cases
        List<Parameter> rulesetParams = ruleset.getParameters().toList();
        rulesetParams.forEach(rp -> {
            String name = rp.getName().asString();
            List<Parameter> testParams = testSuiteParams.getOrDefault(name, null);
            if (testParams == null) {
                String error = String.format("Parameter '%s' is never used in test cases", name);
                errors.add(new ValidationError(ValidationErrorType.PARAMETER_IS_NOT_USED, error,
                        rp.getSourceLocation()));
            } else {
                testParams.forEach(tp -> {
                    if (tp.getType() != rp.getType()) {
                        String error = String.format("Type mismatch for parameter '%s', '%s' expected",
                                tp.getName().asString(), rp.getType());
                        errors.add(new ValidationError(ValidationErrorType.PARAMETER_TYPE_MISMATCH, error,
                                tp.getSourceLocation()));
                    }
                });
            }
        });

        // there might be parameters in test cases not defined in ruleset
        Set<String> rulesetParamNames = ruleset.getParameters().toList().stream()
                .map(p -> p.getName().asString())
                .collect(Collectors.toSet());
        testSuiteParams.forEach((name, list) -> {
            if (!rulesetParamNames.contains(name)) {
                String error = String.format("Test parameter '%s' is not defined in ruleset", name);
                errors.add(new ValidationError(ValidationErrorType.PARAMETER_IS_NOT_DEFINED, error,
                        list.get(0).getSourceLocation()));
            }
        });

        return errors;
    }

    /**
     * Extracts parameters from EndpointTestSuite, returns list of validation errors.
     *
     * @param tests the endpoint test suite
     * @return the list of validation errors if present
     */
    public static Map<String, List<Parameter>> extractTestSuiteParameters(EndpointTestSuite tests) {

        HashMap<String, List<Parameter>> params = new HashMap<String, List<Parameter>>();

        tests.getTestCases().forEach(tc -> {
            List<Parameter> testParams = tc.getParameters();
            testParams.forEach(p -> {
                String name = p.getName().asString();
                List<Parameter> existingParams = params.getOrDefault(name, null);
                if (existingParams == null) {
                    params.put(p.getName().asString(), new ArrayList<>(Collections.singletonList(p)));
                } else {
                    existingParams.add(p);
                }
            });
        });

        return params;
    }

}
