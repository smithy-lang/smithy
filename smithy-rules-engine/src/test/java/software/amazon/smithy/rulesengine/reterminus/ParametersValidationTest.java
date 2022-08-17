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

package software.amazon.smithy.rulesengine.reterminus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.ParameterType;
import software.amazon.smithy.rulesengine.validators.ParametersValidator;
import software.amazon.smithy.rulesengine.validators.ValidationError;
import software.amazon.smithy.rulesengine.validators.ValidationErrorType;
import software.amazon.smithy.utils.MapUtils;

class ParametersValidationTest {
    private EndpointRuleset parseRuleset(String resource) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        assert is != null;
        Node node = ObjectNode.parse(is);
        return EndpointRuleset.fromNode(node);
    }

    private EndpointTestSuite parseTestSuite(String resource) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        assert is != null;
        Node node = ObjectNode.parse(is);
        return EndpointTestSuite.fromNode(node);
    }

    @Test
    void testParametersMatching() {
        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
        List<ValidationError> errors = ParametersValidator.validateParametersMatching(params1, params2);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testParametersTypeMismatch() {
        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.STRING).build());
        List<ValidationError> errors = ParametersValidator.validateParametersMatching(params1, params2);
        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_TYPE_MISMATCH);
    }

    @Test
    void testParametersMismatchExtraParamInFirstSet() {
        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build());
        List<ValidationError> errors = ParametersValidator.validateParametersMatching(params1, params2);
        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
    }

    @Test
    void testParametersMismatchExtraParamInSecondSet() {
        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build());
        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.STRING).build());
        List<ValidationError> errors = ParametersValidator.validateParametersMatching(params1, params2);
        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
    }

    @Test
    void testParametersMismatch() {
        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param3", Parameter.builder().name("param3").type(ParameterType.BOOLEAN).build());
        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.STRING).build());
        List<ValidationError> errors = ParametersValidator.validateParametersMatching(params1, params2);
        assertEquals(2, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
        assertEquals(errors.get(1).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
    }

    @Test
    void testParametersAndTypesMismatch() {
        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.BOOLEAN).build(), "param3", Parameter.builder().name("param3").type(ParameterType.BOOLEAN).build());
        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
        List<ValidationError> errors = ParametersValidator.validateParametersMatching(params1, params2);
        assertEquals(3, errors.size());
    }

    @Test
    void validateTestParametersMatching() {
        EndpointRuleset ruleset = parseRuleset("params-validation/ruleset.json");

        ShapeId serviceId = ShapeId.from("example#FizzBuzz");

        Map<String, Parameter> rulesetParams = ruleset.getParameters().toList().stream()
                .filter(p -> !p.isBuiltIn())
                .collect(Collectors.toMap(param -> param.getName().asString(), item -> item));

        Model model = Model.assembler()
                .discoverModels()
                .addImport(Objects.requireNonNull(getClass().getClassLoader().getResource("params-validation/model.smithy")))
                .assemble()
                .unwrap();

        Map<String, Parameter> modelParams = new HashMap<>();

        List<ValidationError> errors = ParametersValidator.validateModelAndExtractParameters(model, serviceId, modelParams);
        assertEquals(1, errors.size());

        List<ValidationError> matchingErrors = ParametersValidator.validateParametersMatching(rulesetParams, modelParams);
        assertEquals(5, matchingErrors.size());
    }

    @Test
    void validateInconsistentParamsTypesModel() {

        ShapeId serviceId = ShapeId.from("example#FizzBuzz");

        Model model = Model.assembler()
                .discoverModels()
                .addImport(Objects.requireNonNull(getClass().getClassLoader().getResource("params-validation/inconsistent-params-types.smithy")))
                .assemble()
                .unwrap();

        Map<String, Parameter> modelParams = new HashMap<>();

        List<ValidationError> errors = ParametersValidator.validateModelAndExtractParameters(model, serviceId, modelParams);
        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.INCONSISTENT_PARAMETER_TYPE);
    }

    @Test
    void validateInconsistentParamTypesModel() {

        ShapeId serviceId = ShapeId.from("example#FizzBuzz");

        Model model = Model.assembler()
                .discoverModels()
                .addImport(Objects.requireNonNull(getClass().getClassLoader().getResource("params-validation/inconsistent-param-types.smithy")))
                .assemble()
                .unwrap();

        Map<String, Parameter> modelParams = new HashMap<>();

        List<ValidationError> errors = ParametersValidator.validateModelAndExtractParameters(model, serviceId, modelParams);
        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.INCONSISTENT_PARAMETER_TYPE);
    }

    @Test
    void validateInvalidMemberTypeModel() {

        ShapeId serviceId = ShapeId.from("example#FizzBuzz");

        Model model = Model.assembler()
                .discoverModels()
                .addImport(Objects.requireNonNull(getClass().getClassLoader().getResource("params-validation/invalid-member-type.smithy")))
                .assemble()
                .unwrap();

        Map<String, Parameter> modelParams = new HashMap<>();

        List<ValidationError> errors = ParametersValidator.validateModelAndExtractParameters(model, serviceId, modelParams);
        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.UNSUPPORTED_PARAMETER_TYPE);
    }

    @Test
    void validateDuplicateParamModel() {

        ShapeId serviceId = ShapeId.from("example#FizzBuzz");

        Model model = Model.assembler()
                .discoverModels()
                .addImport(Objects.requireNonNull(getClass().getClassLoader().getResource("params-validation/duplicate-param.smithy")))
                .assemble()
                .unwrap();

        Map<String, Parameter> modelParams = new HashMap<>();
        List<ValidationError> errors = ParametersValidator.validateModelAndExtractParameters(model, serviceId, modelParams);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidParams() {

        Model model = Model.assembler()
                .discoverModels()
                .addImport(Objects.requireNonNull(getClass().getClassLoader().getResource("params-validation/valid-model.smithy")))
                .assemble()
                .unwrap();

        EndpointRuleset ruleset = parseRuleset("params-validation/ruleset.json");
        List<ValidationError> errors = ParametersValidator.validateParameters(ruleset, ShapeId.from("example#FizzBuzz"), model);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidTestParams() {

        EndpointRuleset ruleset = parseRuleset("params-validation/eventbridge-rules.json");

        EndpointTestSuite tests = parseTestSuite("params-validation/eventbridge-tests.json");

        List<ValidationError> errors = ParametersValidator.validateTestsParameters(tests, ruleset);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testRequiredTestParams() {

        EndpointRuleset ruleset = parseRuleset("params-validation/eventbridge-rules.json");

        EndpointTestSuite tests = parseTestSuite("params-validation/eventbridge-tests-req-params-missing.json");

        List<ValidationError> errors = ParametersValidator.validateTestsParameters(tests, ruleset);

        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.REQUIRED_PARAMETER_MISSING);
    }

    @Test
    void testParamsTypesMismatch() {

        EndpointRuleset ruleset = parseRuleset("params-validation/eventbridge-rules.json");

        EndpointTestSuite tests = parseTestSuite("params-validation/eventbridge-tests-param-type-mismatch.json");

        List<ValidationError> errors = ParametersValidator.validateTestsParameters(tests, ruleset);

        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_TYPE_MISMATCH);
    }

    @Test
    void testParamsNotUsed() {

        EndpointRuleset ruleset = parseRuleset("params-validation/eventbridge-rules-extra-param.json");

        EndpointTestSuite tests = parseTestSuite("params-validation/eventbridge-tests.json");

        List<ValidationError> errors = ParametersValidator.validateTestsParameters(tests, ruleset);

        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_IS_NOT_USED);
    }

    @Test
    void testParamsNotDefined() {

        EndpointRuleset ruleset = parseRuleset("params-validation/eventbridge-rules.json");

        EndpointTestSuite tests = parseTestSuite("params-validation/eventbridge-tests-param-not-defined.json");

        List<ValidationError> errors = ParametersValidator.validateTestsParameters(tests, ruleset);

        assertEquals(1, errors.size());
        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_IS_NOT_DEFINED);
    }
}
