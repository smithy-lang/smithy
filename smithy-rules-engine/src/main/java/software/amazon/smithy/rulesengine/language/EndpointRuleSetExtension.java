/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.validators.AuthSchemeValidator;
import software.amazon.smithy.utils.ListUtils;

/**
 * An interface to provide components to endpoints rule-sets.
 */
public interface EndpointRuleSetExtension {
    /**
     * Provides built-in parameters to the rules engine.
     *
     * @return A list of built-in parameters this extension provides.
     */
    default List<Parameter> getBuiltIns() {
        return ListUtils.of();
    }

    /**
     * Provides library functions to the rules engine.
     *
     * @return A list of library functions this extension provides.
     */
    default List<FunctionDefinition> getLibraryFunctions() {
        return ListUtils.of();
    }

    /**
     * Provides authentication scheme validators to the rules engine.
     *
     * @return A list of authentication scheme validators this extension provides.
     */
    default List<AuthSchemeValidator> getAuthSchemeValidators() {
        return ListUtils.of();
    }
}
