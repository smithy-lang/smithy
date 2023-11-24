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

package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.shapes.Shape;

/**
 * Updates a schema builder before converting a shape to a schema.
 *
 * {@link JsonSchemaMapper#updateSchema(JsonSchemaMapperContext, Schema.Builder)} is the entry point during JSON Schema
 * conversion, and is the recommended method to implement. If this method is implemented,
 * {@link JsonSchemaMapper#updateSchema(Shape, Schema.Builder, JsonSchemaConfig)} will NOT be called unless written in
 * the implementation.
 */
public interface JsonSchemaMapper {
    /**
     * Gets the sort order of the plugin from -128 to 127.
     *
     * <p>Plugins are applied according to this sort order. Lower values
     * are executed before higher values (for example, -128 comes before 0,
     * comes before 127). Plugins default to 0, which is the middle point
     * between the minimum and maximum order values.
     *
     * @return Returns the sort order, defaulting to 0.
     */
    default byte getOrder() {
        return 0;
    }

    /**
     * Updates a schema builder using information in {@link JsonSchemaMapperContext}.
     *
     * If not implemented, will default to
     * {@link JsonSchemaMapper#updateSchema(Shape, Schema.Builder, JsonSchemaConfig)} for backwards-compatibility.
     *
     * @param context Context with information needed to update the schema.
     * @param schemaBuilder Schema builder to update.
     * @return Returns an updated schema builder.
     */
    default Schema.Builder updateSchema(JsonSchemaMapperContext context, Schema.Builder schemaBuilder) {
        return updateSchema(context.getShape(), schemaBuilder, context.getConfig());
    }

    /**
     * Updates a schema builder, and is not recommended. Use
     * {@link JsonSchemaMapper#updateSchema(JsonSchemaMapperContext, Schema.Builder)} instead.
     *
     * If not implemented, this method will default to a no-op.
     *
     * This method is not deprecated for backwards-compatibility.
     *
     * @param shape Shape used for the conversion.
     * @param schemaBuilder Schema builder to update.
     * @param config JSON Schema config.
     * @return Returns an updated schema builder.
     */
    default Schema.Builder updateSchema(Shape shape, Schema.Builder schemaBuilder, JsonSchemaConfig config) {
        return schemaBuilder;
    }
}
