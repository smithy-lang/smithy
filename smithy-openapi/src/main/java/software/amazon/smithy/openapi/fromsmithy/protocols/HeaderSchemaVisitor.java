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

import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;

/**
 * Creates an appropriate Schema for an input/output header parameters.
 *
 * @param <T> Type of protocol trait to convert.
 */
final class HeaderSchemaVisitor<T extends Trait> extends ShapeVisitor.Default<Schema> {
    private final Context<T> context;
    private final Schema schema;
    private final MemberShape member;

    HeaderSchemaVisitor(Context<T> context, Schema schema, MemberShape member) {
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
                new HeaderSchemaVisitor<>(context, refSchema, collectionMember));
        // Copy the collection schema, remove any $ref, and change the items.
        return schema.toBuilder()
                .ref(null)
                .type("array")
                .items(itemsSchema)
                .build();
    }

    // Header timestamps in Smithy use the HTTP-Date format if a
    // timestamp format is not explicitly set. An inline schema is
    // created if the format was not explicitly set.
    @Override
    public Schema timestampShape(TimestampShape shape) {
        if (member.hasTrait(TimestampFormatTrait.class)) {
            return schema;
        }

        // Uses an HTTP-date format by default.
        Schema original = context.getJsonSchemaConverter().convertShape(member).getRootSchema();
        Schema.Builder copiedBuilder = ModelUtils.convertSchemaToStringBuilder(original);
        return copiedBuilder.format(null).build();
    }

    @Override
    public Schema stringShape(StringShape shape) {
        // String shapes with the mediaType trait must be base64 encoded.
        return shape.hasTrait(MediaTypeTrait.class)
               ? schema.toBuilder().ref(null).type("string").format("byte").build()
               : schema;
    }
}
