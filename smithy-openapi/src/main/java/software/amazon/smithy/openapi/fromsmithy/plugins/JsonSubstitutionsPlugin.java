/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.fromsmithy.plugins;

import software.amazon.smithy.build.JsonSubstitutions;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.model.OpenApi;

/**
 * Substitutes JSON string values using the mapping defined in
 * {@code openapi.substitutions}.
 */
public final class JsonSubstitutionsPlugin implements SmithyOpenApiPlugin {
    @Override
    public byte getOrder() {
        return 120;
    }

    @Override
    public ObjectNode updateNode(Context context, OpenApi openapi, ObjectNode node) {
        return context.getConfig().getObjectMember(OpenApiConstants.SUBSTITUTIONS)
                .map(substitutions -> JsonSubstitutions.create(substitutions).apply(node).expectObjectNode())
                .orElse(node);
    }
}
