/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.build.JsonSubstitutions;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Substitutes JSON string values using the mapping defined in
 * {@code openapi.substitutions}.
 */
@SmithyInternalApi
public final class OpenApiJsonSubstitutions implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(OpenApiJsonSubstitutions.class.getName());

    @Override
    public byte getOrder() {
        return 120;
    }

    @Override
    public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        Map<String, Node> substitutions = context.getConfig().getSubstitutions();

        if (substitutions.isEmpty()) {
            return node;
        }

        LOGGER.warning("Using `substitutions` is discouraged. DO NOT use placeholders in your Smithy model "
                + "for properties that are used by other tools like SDKs or service frameworks; "
                + "placeholders should only ever be used in models for metadata that is specific to "
                + "generating OpenAPI artifacts.\n\n"
                + "Prefer safer alternatives like `jsonAdd`");

        return JsonSubstitutions.create(substitutions).apply(node).expectObjectNode();
    }
}
