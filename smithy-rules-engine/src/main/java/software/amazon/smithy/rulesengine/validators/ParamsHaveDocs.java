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
import software.amazon.smithy.rulesengine.reterminus.EndpointRuleset;


/**
 * Validator to ensure that all parameters have documentation.
 *
 * Note: this validator is not included in the default set of validators and must be enabled conditionally
 */
public final class ParamsHaveDocs {
    private ParamsHaveDocs() {
    }

    public static Stream<ValidationError> ensureParamsHaveDocs(EndpointRuleset ruleset) {
        return ruleset
                .getParameters()
                .toList().stream()
                .filter(param -> !param.getDocumentation().isPresent())
                .map(paramWithoutDocs ->
                        new ValidationError(
                                ValidationErrorType.PARAM_MISSING_DOCS,
                                String.format("Parameter %s did not have documentation",
                                        paramWithoutDocs.getName()), paramWithoutDocs.getSourceLocation())
                );
    }
}
