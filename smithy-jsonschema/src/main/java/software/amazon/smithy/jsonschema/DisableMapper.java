/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.shapes.Shape;

/**
 * Removes keywords from a Schema builder that have been disabled using
 * {@link JsonSchemaConfig#setDisableFeatures}.
 */
final class DisableMapper implements JsonSchemaMapper {
    @Override
    public byte getOrder() {
        return 120;
    }

    @Override
    public Schema.Builder updateSchema(Shape shape, Schema.Builder schema, JsonSchemaConfig config) {
        for (String feature : config.getDisableFeatures()) {
            schema.disableProperty(feature);
        }

        return schema;
    }
}
