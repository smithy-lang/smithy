/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */


package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Context for a JSON schema mapping.
 *
 * @param <T> Type of Smithy {@link Shape} being mapped.
 */
public class JsonSchemaMapperContext<T extends Shape> {
    private final Model model;
    private final T shape;
    private final JsonSchemaConfig config;

    JsonSchemaMapperContext(
            Model model,
            T shape,
            JsonSchemaConfig config
    ) {
        this.model = model;
        this.shape = shape;
        this.config = config;
    }

    /**
     * Gets the Smithy model being converted.
     *
     * @return Returns the Smithy model.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Gets the Smithy shape being mapped.
     *
     * @return Returns the Smithy shape.
     */
    public T getShape() {
        return shape;
    }

    /**
     * Gets the JSON schema configuration object.
     *
     * @return Returns the JSON schema config object.
     */
    public JsonSchemaConfig getConfig() {
        return config;
    }
}
