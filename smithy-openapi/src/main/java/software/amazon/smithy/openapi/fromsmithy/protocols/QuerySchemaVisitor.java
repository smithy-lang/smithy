/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;

/**
 * Creates an appropriate Schema for an input query string parameter.
 *
 * @param <T> Type of protocol to convert.
 */
final class QuerySchemaVisitor<T extends Trait> extends ShapeVisitor.Default<Schema> {
    private final Context<T> context;
    private final Schema schema;
    private final MemberShape member;

    QuerySchemaVisitor(Context<T> context, Schema schema, MemberShape member) {
        this.context = context;
        this.schema = schema;
        this.member = member;
    }

    @Override
    protected Schema getDefault(Shape shape) {
        return schema;
    }

    // Rewrite collections in case the members contain timestamps, blobs, etc.
    @Override
    public Schema listShape(ListShape shape) {
        MemberShape collectionMember = shape.getMember();
        Shape collectionTarget = context.getModel().expectShape(collectionMember.getTarget());
        // Recursively change the items schema and its targets as needed.
        Schema refSchema = context.inlineOrReferenceSchema(collectionMember);
        Schema itemsSchema = collectionTarget.accept(
                new QuerySchemaVisitor<>(context, refSchema, collectionMember));
        // Copy the collection schema, remove any $ref, and change the items.
        return schema.toBuilder()
                .ref(null)
                .type("array")
                .items(itemsSchema)
                .build();
    }

    // Query string timestamps in Smithy are date-time strings by default
    // unless overridden by the timestampFormat trait. This code grabs the
    // referenced shape and creates an inline schema that explicitly defines
    // the necessary styles.
    @Override
    public Schema timestampShape(TimestampShape shape) {
        // Use the referenced shape as-is since it defines an explicit format.
        if (member.hasTrait(TimestampFormatTrait.class)) {
            return schema;
        }

        // Synthesize a new inline shape that defines an explicit format.
        Schema originalSchema = context.getJsonSchemaConverter().convertShape(member).getRootSchema();
        return ModelUtils.convertSchemaToStringBuilder(originalSchema)
                .format("date-time")
                .build();
    }
}
