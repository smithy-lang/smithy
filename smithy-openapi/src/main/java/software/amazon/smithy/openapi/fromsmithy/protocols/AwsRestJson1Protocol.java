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

package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;

/**
 * Converts the {@code aws.protocols#restJson1} protocol to OpenAPI.
 */
public final class AwsRestJson1Protocol extends AbstractRestProtocol<RestJson1Trait> {
    @Override
    public Class<RestJson1Trait> getProtocolType() {
        return RestJson1Trait.class;
    }

    @Override
    public void updateDefaultSettings(Model model, OpenApiConfig config) {
        config.setUseJsonName(true);
        config.setDefaultTimestampFormat(TimestampFormatTrait.Format.EPOCH_SECONDS);
    }

    @Override
    String getDocumentMediaType(Context context, Shape operationOrError, MessageType message) {
        return context.getConfig().getJsonContentType();
    }

    @Override
    Schema createDocumentSchema(
            Context<RestJson1Trait> context,
            Shape operationOrError,
            List<HttpBinding> bindings,
            MessageType message
    ) {
        if (bindings.isEmpty()) {
            return Schema.builder().type("object").build();
        }

        // We create a synthetic structure shape that is passed through the
        // JSON schema converter. This shape only contains members that make
        // up the "document" members of the input/output/error shape.
        ShapeId container = bindings.get(0).getMember().getContainer();
        StructureShape containerShape = context.getModel().expectShape(container, StructureShape.class);

        // Path parameters of requests are handled in "parameters" and headers are
        // handled in headers, so this method must ensure that only members that
        // are sent in the document payload are present in the structure when it is
        // converted to OpenAPI. This ensures that any path parameters are removed
        // before converting the structure to a synthesized JSON schema object.
        // Doing this sanitation after converting the shape to JSON schema might
        // result in things like "required" properties pointing to members that
        // don't exist.
        Set<String> documentMemberNames = bindings.stream()
                .map(HttpBinding::getMemberName)
                .collect(Collectors.toSet());

        // Remove non-document members.
        StructureShape.Builder containerShapeBuilder = containerShape.toBuilder();
        for (String memberName : containerShape.getAllMembers().keySet()) {
            if (!documentMemberNames.contains(memberName)) {
                containerShapeBuilder.removeMember(memberName);
            }
        }

        StructureShape cleanedShape = containerShapeBuilder.build();
        return context.getJsonSchemaConverter().convertShape(cleanedShape).getRootSchema();
    }
}
