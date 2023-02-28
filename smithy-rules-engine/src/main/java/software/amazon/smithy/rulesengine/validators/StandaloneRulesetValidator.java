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

import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A meta validator that verifies a rule-set and tests.
 */
@SmithyUnstableApi
public final class StandaloneRulesetValidator {
    private StandaloneRulesetValidator() {}

    public static Stream<ValidationError> validate(EndpointRuleSet ruleset, EndpointTestsTrait testSuite) {
        Stream<ValidationError> base = Stream.concat(
                Stream.concat(
                        BuiltInsValidator.validateBuiltIns(ruleset),
                        new ValidateUriScheme().visitRuleset(ruleset)),
                AuthSchemesValidator.validateRuleset(ruleset));

        if (testSuite != null) {
            base = Stream.concat(base, BuiltInsValidator.validateBuiltIns(testSuite));
        }
        return base;
    }
}
