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
import java.util.regex.Pattern;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.Context;

/**
 * Provides the conversion from Smithy aws.rest-json-1.0 and
 * aws.rest-json-1.1 to OpenAPI operations.
 */
public final class AwsRestJsonProtocol extends AbstractRestProtocol {
    @Override
    public Pattern getProtocolNamePattern() {
        return Pattern.compile("^aws\\.rest-json(?:-1\\.[0|1])?$");
    }

    @Override
    String getDocumentMediaType(Context context, Shape operationOrError, MessageType message) {
        return context.getConfig().getStringMemberOrDefault(
                OpenApiConstants.AWS_JSON_CONTENT_TYPE, "application/json");
    }

    @Override
    Schema createDocumentSchema(
            Context context,
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
        // Creating this kind of synthetic shape takes advantage of generic
        // things like handling required properties, pattern, length, range,
        // documentation, jsonName, and passes the synthetic JSON schema
        // through any registered mappers.
        ShapeId container = bindings.get(0).getMember().getContainer();
        StructureShape.Builder tempShapeBuilder = StructureShape.builder().id(container);

        for (HttpBinding binding : bindings) {
            tempShapeBuilder.addMember(binding.getMember().toBuilder()
                    .id(container.withMember(binding.getMemberName()))
                    .build());
        }

        StructureShape tempShape = tempShapeBuilder.build();
        ShapeIndex index = context.getModel().getShapeIndex();

        return context.getJsonSchemaConverter().convert(index, tempShape).getRootSchema();
    }
}
