/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Context for a JSON schema mapping.
 */
public class JsonSchemaMapperContext {
    private final Model model;
    private final Shape shape;
    private final JsonSchemaConfig config;

    JsonSchemaMapperContext(
            Model model,
            Shape shape,
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
    public Shape getShape() {
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
