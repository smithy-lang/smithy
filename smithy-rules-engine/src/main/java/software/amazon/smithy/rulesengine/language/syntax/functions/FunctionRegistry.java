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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.stdlib.AwsIsVirtualHostableS3Bucket;
import software.amazon.smithy.rulesengine.language.stdlib.AwsPartition;
import software.amazon.smithy.rulesengine.language.stdlib.BooleanEquals;
import software.amazon.smithy.rulesengine.language.stdlib.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.stdlib.ParseArn;
import software.amazon.smithy.rulesengine.language.stdlib.ParseUrl;
import software.amazon.smithy.rulesengine.language.stdlib.StringEquals;
import software.amazon.smithy.rulesengine.language.stdlib.Substring;
import software.amazon.smithy.rulesengine.language.stdlib.UriEncode;
import software.amazon.smithy.utils.SmithyUnstableApi;


/**
 * Collection of registered functions.
 */
@SmithyUnstableApi
public final class FunctionRegistry {
    private static final Map<String, FunctionDefinition> GLOBAL_REGISTRY = new HashMap<>();

    static {
        GLOBAL_REGISTRY.put(AwsIsVirtualHostableS3Bucket.ID, new AwsIsVirtualHostableS3Bucket());
        GLOBAL_REGISTRY.put(AwsPartition.ID, new AwsPartition());
        GLOBAL_REGISTRY.put(BooleanEquals.ID, new BooleanEquals.Definition());
        // GetAttr
        // IsSet
        GLOBAL_REGISTRY.put(IsValidHostLabel.ID, new IsValidHostLabel());
        // Not
        GLOBAL_REGISTRY.put(ParseArn.ID, new ParseArn());
        GLOBAL_REGISTRY.put(ParseUrl.ID, new ParseUrl());
        GLOBAL_REGISTRY.put(StringEquals.ID, new StringEquals.Definition());
        GLOBAL_REGISTRY.put(Substring.ID, new Substring());
        GLOBAL_REGISTRY.put(UriEncode.ID, new UriEncode());
    }

    private FunctionRegistry() {}

    public static void registerFunction(FunctionDefinition definition) {
        GLOBAL_REGISTRY.put(definition.getId(), definition);
    }

    /**
     * Retrieves the {@link LibraryFunction} for the given {@link FunctionNode} if registered.
     *
     * @param node the FunctionNode to retrieve the function for.
     * @return the optional function.
     */
    public static Optional<LibraryFunction> forNode(FunctionNode node) {
        if (GLOBAL_REGISTRY.containsKey(node.getName())) {
            return Optional.of(new LibraryFunction(GLOBAL_REGISTRY.get(node.getName()), node));
        }
        return Optional.empty();
    }
}
