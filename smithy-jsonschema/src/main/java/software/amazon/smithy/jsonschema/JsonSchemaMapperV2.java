/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.shapes.Shape;

public interface JsonSchemaMapperV2 extends JsonSchemaMapper {
    default Schema.Builder updateSchema(Shape shape, Schema.Builder schemaBuilder, JsonSchemaConfig config) {
        return schemaBuilder;
    }

    /**
     * Updates a schema builder.
     *
     * @param context Context of this schema mapping.
     * @param schemaBuilder Schema builder to update.
     * @return Returns an updated schema builder.
     */
    Schema.Builder updateSchema(JsonSchemaMapperContext<? extends Shape> context, Schema.Builder schemaBuilder);
}
