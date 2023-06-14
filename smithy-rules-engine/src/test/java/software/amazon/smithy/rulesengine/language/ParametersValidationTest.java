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
