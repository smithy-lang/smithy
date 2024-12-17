/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Updates builders based on timestamp shapes, timestampFormat traits, and
 * the value of {@link JsonSchemaConfig#getDefaultTimestampFormat()}.
 */
final class TimestampMapper implements JsonSchemaMapper {

    @Override
    public byte getOrder() {
        return -120;
    }

    @Override
    public Schema.Builder updateSchema(Shape shape, Schema.Builder builder, JsonSchemaConfig config) {
        String format = config.detectJsonTimestampFormat(shape).orElse(null);

        if (format == null) {
            return builder;
        }

        switch (format) {
            case TimestampFormatTrait.DATE_TIME:
                return builder.type("string").format(format);
            case TimestampFormatTrait.HTTP_DATE:
                return builder.type("string");
            case TimestampFormatTrait.EPOCH_SECONDS:
                return builder.format(null).type("number");
            default:
                // Handle any future "epoch-*" formats like epoch-millis.
                return format.startsWith("epoch-") ? builder.type("number") : builder.type("string");
        }
    }
}
