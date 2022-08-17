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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.reterminus.EndpointRuleset;
import software.amazon.smithy.rulesengine.reterminus.EndpointTestSuite;

public final class StandaloneRulesetValidator {
    private StandaloneRulesetValidator() {
    }

    public static Stream<ValidationError> validate(EndpointRuleset ruleset, EndpointTestSuite testSuite) {
        Stream<ValidationError> base = Stream.concat(
                BuiltInsValidator.validateBuiltIns(ruleset),
                new ValidateUriScheme().visitRuleset(ruleset)
        );
        if (testSuite != null) {
            base = Stream.concat(base, BuiltInsValidator.validateBuiltIns(testSuite));
        }
        return base;
    }

    public static void validateOrError(EndpointRuleset ruleset, EndpointTestSuite testSuite) {
        List<ValidationError> errors = validate(ruleset, testSuite).collect(Collectors.toList());
        if (!errors.isEmpty()) {
            throw new RuntimeException(String.format("There were validation errors: %n%s", errors));
        }
    }
}
