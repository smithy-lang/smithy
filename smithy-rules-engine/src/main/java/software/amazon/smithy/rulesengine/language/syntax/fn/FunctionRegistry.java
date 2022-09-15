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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import java.util.HashMap;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.stdlib.IsValidHostLabel;
import software.amazon.smithy.rulesengine.language.stdlib.IsVirtualHostableS3Bucket;
import software.amazon.smithy.rulesengine.language.stdlib.ParseArn;
import software.amazon.smithy.rulesengine.language.stdlib.ParseUrl;
import software.amazon.smithy.rulesengine.language.stdlib.PartitionFn;
import software.amazon.smithy.rulesengine.language.stdlib.Substring;
import software.amazon.smithy.rulesengine.language.stdlib.UriEncode;
import software.amazon.smithy.rulesengine.language.util.LazyValue;
import software.amazon.smithy.utils.SmithyUnstableApi;


/**
 * Collection of registered functions.
 */
@SmithyUnstableApi
public final class FunctionRegistry {
    private static final LazyValue<FunctionRegistry> GLOBAL_REGISTRY =
            LazyValue.<FunctionRegistry>builder().initializer(() -> {
                FunctionRegistry registry = new FunctionRegistry();
                registry.registerFunction(new PartitionFn());
                registry.registerFunction(new IsValidHostLabel());
                registry.registerFunction(new ParseArn());
                registry.registerFunction(new ParseUrl());
                registry.registerFunction(new Substring());
                registry.registerFunction(new UriEncode());
                registry.registerFunction(new IsVirtualHostableS3Bucket());
                return registry;
            }).build();

    private final HashMap<String, FunctionDefinition> registry = new HashMap<>();

    private FunctionRegistry() {
    }

    static FunctionRegistry getGlobalRegistry() {
        return GLOBAL_REGISTRY.value();
    }

    public void registerFunction(FunctionDefinition definition) {
        registry.put(definition.id(), definition);
    }

    public Optional<LibraryFunction> forNode(FnNode node) {
        if (registry.containsKey(node.getId())) {
            return Optional.of(new LibraryFunction(registry.get(node.getId()), node));
        } else {
            return Optional.empty();
        }
    }
}
