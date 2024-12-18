/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.UriEncode;
import software.amazon.smithy.rulesengine.language.syntax.parameters.BuiltIns;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.utils.ListUtils;

/**
 * Core extensions to smithy-rules-engine.
 */
public class CoreExtension implements EndpointRuleSetExtension {
    @Override
    public List<Parameter> getBuiltIns() {
        return ListUtils.of(BuiltIns.SDK_ENDPOINT);
    }

    @Override
    public List<FunctionDefinition> getLibraryFunctions() {
        return ListUtils.of(
                BooleanEquals.getDefinition(),
                GetAttr.getDefinition(),
                IsSet.getDefinition(),
                IsValidHostLabel.getDefinition(),
                Not.getDefinition(),
                ParseUrl.getDefinition(),
                StringEquals.getDefinition(),
                Substring.getDefinition(),
                UriEncode.getDefinition());
    }
}
