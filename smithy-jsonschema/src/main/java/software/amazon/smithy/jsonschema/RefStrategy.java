/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines a strategy for converting Shape IDs to JSON schema $ref values.
 *
 * <p>This API is currently package-private, but could be exposed in the
 * future if we *really* need to. Ideally we don't.
 */
interface RefStrategy {

    /**
     * Given a shape ID, returns the value used in a $ref to refer to it.
     *
     * <p>The return value is expected to be a JSON pointer.
     *
     * @param id Shape ID to convert to a $ref string.
     * @return Returns the $ref string (e.g., "#/responses/MyShape").
     */
    String toPointer(ShapeId id);

    /**
     * Returns true if the given shape should be inlined into
     * its container or if the shape should be a ref.
     *
     * @param shape Shape to check.
     * @return Returns true if this shape should be inlined.
     */
    boolean isInlined(Shape shape);

    /**
     * Creates a default strategy for converting shape IDs to $refs.
     *
     * <p>When a "service" is given in the configuration, shape names
     * used in refs are based on the "rename" property of the service.
     *
     * <p>This default strategy will make the created value consist
     * of only alphanumeric characters. When a namespace is included
     * (because "stripNamespaces" is not set), the namespace is added
     * to the beginning of the created name by capitalizing the first
     * letter of each part of the namespace, removing the "."
     * (for example, "smithy.example" becomes "SmithyExample"). Next,
     * the shape name is appended.
     *
     * <p>For example, given the following shape ID "smithy.example#Foo",
     * the following ref is created "#/definitions/SmithyExampleFoo".
     *
     * <p>This implementation honors the value configured in
     * {@link JsonSchemaConfig#getDefinitionPointer()} to create a $ref
     * pointer to a shape.
     *
     * @param model Model being converted.
     * @param config Conversion configuration.
     * @param propertyNamingStrategy Property naming strategy.
     * @param shapePredicate a predicate to use to filter shapes in model when determining conflicts.
     * @return Returns the created strategy.
     */
    static RefStrategy createDefaultStrategy(
            Model model,
            JsonSchemaConfig config,
            PropertyNamingStrategy propertyNamingStrategy,
            Predicate<Shape> shapePredicate
    ) {
        RefStrategy delegate = new DefaultRefStrategy(model, config, propertyNamingStrategy);
        return new DeconflictingStrategy(model, delegate, shapePredicate);
    }
}
