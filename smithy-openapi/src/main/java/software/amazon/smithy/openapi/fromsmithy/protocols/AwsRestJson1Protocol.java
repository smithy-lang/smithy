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

import static software.amazon.smithy.openapi.OpenApiConfig.ErrorStatusConflictHandlingStrategy.ONE_OF;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientDiscoveredEndpointTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.utils.SetUtils;

/**
 * Converts the {@code aws.protocols#restJson1} protocol to OpenAPI.
 */
public final class AwsRestJson1Protocol extends AbstractRestProtocol<RestJson1Trait> {

    private static final Set<String> AWS_REQUEST_HEADERS = SetUtils.of(
            // Used by clients for a purpose similar to the standard user-agent header.
            "X-Amz-User-Agent",
            // Used by clients configured to work with X-Ray.
            "X-Amzn-Trace-Id",
            // Used by clients for adaptive retry behavior.
            "Amz-Sdk-Request", "Amz-Sdk-Invocation-Id"
    );

    private static final Set<String> AWS_RESPONSE_HEADERS = SetUtils.of(
            // Used to identify a given request/response, primarily for debugging.
            "X-Amzn-Requestid",
            // Used to indicate which modeled error a given HTTP error represents.
            "X-Amzn-Errortype"
    );

    @Override
    public Class<RestJson1Trait> getProtocolType() {
        return RestJson1Trait.class;
    }

    @Override
    public Set<String> getProtocolRequestHeaders(Context<RestJson1Trait> context, OperationShape operationShape) {
        // x-amz-api-version if it is an endpoint operation
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(super.getProtocolRequestHeaders(context, operationShape));
        headers.addAll(AWS_REQUEST_HEADERS);
        if (operationShape.hasTrait(ClientDiscoveredEndpointTrait.class)) {
            headers.add("X-Amz-Api-Version");
        }
        return headers;
    }

    @Override
    public Set<String> getProtocolResponseHeaders(Context<RestJson1Trait> context, OperationShape operationShape) {
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(super.getProtocolResponseHeaders(context, operationShape));
        headers.addAll(AWS_RESPONSE_HEADERS);
        return headers;
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

        // If errors with the same status code have been deconflicted,
        // hoist the members from the synthetic target union and convert
        // the error structure to a union, so that a `oneOf` can be used
        // in the output without added nesting.
        if (context.getConfig().getOnErrorStatusConflict() != null
                && context.getConfig().getOnErrorStatusConflict().equals(ONE_OF)
                && targetsSyntheticError(cleanedShape, context)) {
            UnionShape.Builder asUnion = UnionShape.builder().id(cleanedShape.getId());
            UnionShape targetUnion = context.getModel().expectShape(
                    cleanedShape.getAllMembers().values().stream().findFirst().get().getTarget(), UnionShape.class);
            for (MemberShape member : targetUnion.getAllMembers().values()) {
                String name = member.getMemberName();
                asUnion.addMember(member.toBuilder().id(cleanedShape.getId().withMember(name)).build());
            }
            return context.getJsonSchemaConverter().convertShape(asUnion.build()).getRootSchema();
        }
        return context.getJsonSchemaConverter().convertShape(cleanedShape).getRootSchema();
    }

    private boolean targetsSyntheticError(StructureShape shape, Context context) {
        if (shape.hasTrait(HttpErrorTrait.ID)) {
            HttpErrorTrait trait = shape.expectTrait(HttpErrorTrait.class);
            String suffix = trait.getCode() + "Error";
            if (shape.getId().getName().endsWith(suffix)) {
                return hasSingleUnionMember(shape, context.getModel());
            }
        }
        return false;
    }

    private boolean hasSingleUnionMember(StructureShape shape, Model model) {
        long unionCount = shape.getAllMembers().values().stream()
                .map(member -> model.expectShape(member.getTarget()))
                .filter(Shape::isUnionShape)
                .count();
        return unionCount == 1;
    }

    @Override
    Node transformSmithyValueToProtocolValue(Node value) {
        return value;
    }
}
