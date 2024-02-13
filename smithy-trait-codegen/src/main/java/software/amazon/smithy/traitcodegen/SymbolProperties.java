/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SymbolProperties {
    // Provides the correct method to map value from a member to a builder value
    // This should be a string template accepting the member name as the only argument
    public static final String MEMBER_MAPPER = "member-mapper";

    // Provides the lambda method call to map the shape to a node
    public static final String TO_NODE_MAPPER = "to-node-mapper";

    // Other symbols to import when using the node mapper for a symbol
    public static final String NODE_MAPPING_IMPORTS = "node-mapper-imports";

    public static final String FROM_NODE_MAPPER = "from-node-mapper";

    // Provides an initializer for the builder ref
    public static final String BUILDER_REF_INITIALIZER = "builder-ref-initializer";

    public static final String BASE_SYMBOL = "base-symbol";

    private SymbolProperties() {
        // No constructor for constants class
    }
}
