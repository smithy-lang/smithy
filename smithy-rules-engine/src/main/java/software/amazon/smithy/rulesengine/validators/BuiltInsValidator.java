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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.rulesengine.language.EndpointRuleset;
import software.amazon.smithy.rulesengine.language.EndpointTestSuite;
import software.amazon.smithy.rulesengine.language.lang.parameters.Builtins;

public final class BuiltInsValidator {
    private BuiltInsValidator() {
    }

    /**
     * Validate that all BuiltIn parameters in this ruleset reference predefined built ins.
     *
     * @param ruleset The ruleset to check
     * @return validation errors
     */
    public static Stream<ValidationError> validateBuiltIns(EndpointRuleset ruleset) {
        return ruleset.getParameters().toList().stream().flatMap(param -> {
            Optional<String> builtIn = param.getBuiltIn();
            if (builtIn.isPresent()) {
                return validateBuiltIn(builtIn.get(), param);
            }
            return Stream.empty();
        });
    }


    /**
     * Validate that all BuiltIn parameters in this testSuite reference predefined built ins.
     *
     * @param testSuite The testSuite to check
     * @return validation errors
     */
    public static Stream<ValidationError> validateBuiltIns(EndpointTestSuite testSuite) {
        return testSuite.getTestCases().stream().flatMap(test -> test.getOperationInputs().stream())
                .flatMap(operationInput -> operationInput.getBuiltinParameters().keySet().stream())
                .flatMap(builtIn -> validateBuiltIn(builtIn.asString(), builtIn));
    }

    private static Stream<ValidationError> validateBuiltIn(String builtInName, FromSourceLocation source) {
        if (!getAllBuiltIns().contains(builtInName)) {
            return Stream.of(
                    invalidBuiltIn(source, builtInName)
            );
        }
        return Stream.empty();
    }

    private static ValidationError invalidBuiltIn(FromSourceLocation param, String builtIn) {
        return new ValidationError(
                ValidationErrorType.INVALID_BUILTIN,
                String.format("%s is not a valid builtIn parameter (%s)", builtIn, getAllBuiltIns()),
                param.getSourceLocation());
    }

    private static Set<String> getAllBuiltIns() {
        return Builtins.ALL_BUILTINS.stream()
            .map(prop -> prop.getBuiltIn().get())
            .collect(Collectors.toSet());
    }
}
