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

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Updates a schema builder before converting a shape to a schema.
 */
public interface SchemaBuilderMapper {
    /**
     * Defines the order a mapper is applied to a builder.
     *
     * <p>Mappers in FIRST applied applied before SECOND, and so on.
     */
    enum GroupOrder { FIRST, SECOND, THIRD }

    /**
     * Updates a schema builder.
     *
     * @param shape Shape used for the conversion.
     * @param schemaBuilder Schema builder to update.
     * @param config JSON Schema config.
     * @return Returns an updated schema builder.
     */
    Schema.Builder updateSchema(Shape shape, Schema.Builder schemaBuilder, ObjectNode config);

    /**
     * Gets the order in which this strategy should be applied to a builder.
     *
     * @return Returns the order group.
     */
    default GroupOrder getOrder() {
        return GroupOrder.SECOND;
    }
}
