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

package software.amazon.smithy.rulesengine.language;

class ParametersValidationTest {
//
//    @Test
//    void testParametersMismatchExtraParamInFirstSet() {
//        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
//        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build());
//        List<ValidationError> errors = RuleSetParameterValidator.validateParametersMatching(params1, params2);
//        assertEquals(1, errors.size());
//        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
//    }
//
//    @Test
//    void testParametersMismatchExtraParamInSecondSet() {
//        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build());
//        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.STRING).build());
//        List<ValidationError> errors = RuleSetParameterValidator.validateParametersMatching(params1, params2);
//        assertEquals(1, errors.size());
//        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
//    }
//
//    @Test
//    void testParametersMismatch() {
//        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param3", Parameter.builder().name("param3").type(ParameterType.BOOLEAN).build());
//        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.STRING).build());
//        List<ValidationError> errors = RuleSetParameterValidator.validateParametersMatching(params1, params2);
//        assertEquals(2, errors.size());
//        assertEquals(errors.get(0).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
//        assertEquals(errors.get(1).validationErrorType(), ValidationErrorType.PARAMETER_MISMATCH);
//    }
//
//    @Test
//    void testParametersAndTypesMismatch() {
//        Map<String, Parameter> params1 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.BOOLEAN).build(), "param3", Parameter.builder().name("param3").type(ParameterType.BOOLEAN).build());
//        Map<String, Parameter> params2 = MapUtils.of("param1", Parameter.builder().name("param1").type(ParameterType.STRING).build(), "param2", Parameter.builder().name("param2").type(ParameterType.BOOLEAN).build());
//        List<ValidationError> errors = RuleSetParameterValidator.validateParametersMatching(params1, params2);
//        assertEquals(3, errors.size());
//    }
}
