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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Converts a Smithy model index to a JSON schema document.
 */
public final class JsonSchemaConverter {
    private final List<SchemaBuilderMapper> customMappers = new ArrayList<>();
    private final List<SchemaBuilderMapper> discoveredMappers = new ArrayList<>();
    private boolean alreadyDiscoveredMappers;
    private PropertyNamingStrategy propertyNamingStrategy;
    private ObjectNode config = Node.objectNode();
    private RefStrategy refStrategy;
    private Predicate<Shape> shapePredicate = shape -> true;

    private JsonSchemaConverter() {}

    /**
     * Creates a new JsonSchemaConverter.
     *
     * @return Returns the created JsonSchemaConverter.
     */
    public static JsonSchemaConverter create() {
        return new JsonSchemaConverter();
    }

    /**
     * Copies the JsonSchemaConverter to a new converter.
     *
     * @return Returns the copied converter.
     */
    public JsonSchemaConverter copy() {
        JsonSchemaConverter copy = create();
        copy.config = config;
        copy.customMappers.addAll(customMappers);
        copy.discoveredMappers.addAll(discoveredMappers);
        copy.alreadyDiscoveredMappers = alreadyDiscoveredMappers;
        copy.propertyNamingStrategy = propertyNamingStrategy;
        copy.refStrategy = refStrategy;
        copy.shapePredicate = shapePredicate;
        return copy;
    }

    /**
     * Sets a predicate used to filter Smithy shapes from being converted
     * to JSON Schema.
     *
     * @param shapePredicate Predicate that returns true if a shape is to be converted.
     * @return Returns the converter.
     */
    public JsonSchemaConverter shapePredicate(Predicate<Shape> shapePredicate) {
        this.shapePredicate = shapePredicate;
        return this;
    }

    /**
     * Sets the configuration object.
     *
     * @param config Config to use.
     * @return Returns the converter.
     */
    public JsonSchemaConverter config(ObjectNode config) {
        this.config = config;
        return this;
    }

    /**
     * Gets the configuration object.
     *
     * @return Returns the config object.
     */
    public ObjectNode getConfig() {
        return config;
    }

    /**
     * Sets a custom property naming strategy.
     *
     * <p>This method overrides an configuration values specified by
     * the configuration object.
     *
     * @param propertyNamingStrategy Property name strategy to use.
     * @return Returns the converter.
     */
    public JsonSchemaConverter propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
        this.propertyNamingStrategy = propertyNamingStrategy;
        return this;
    }

    /**
     * Gets the property naming strategy of the converter.
     *
     * @return Returns the PropertyNamingStrategy.
     */
    public PropertyNamingStrategy getPropertyNamingStrategy() {
        if (propertyNamingStrategy == null) {
            propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();
        }
        return propertyNamingStrategy;
    }

    /**
     * Sets a custom reference naming strategy.
     *
     * <p>This method overrides an configuration values specified by
     * the settings object.
     *
     * @param refStrategy Reference naming strategy to use.
     * @return Returns the converter.
     */
    public JsonSchemaConverter refStrategy(RefStrategy refStrategy) {
        this.refStrategy = refStrategy;
        return this;
    }

    /**
     * Gets the RefStrategy used by the converter.
     *
     * @return Reference naming strategy to use.
     */
    public RefStrategy getRefStrategy() {
        if (refStrategy == null) {
            refStrategy = RefStrategy.createDefaultStrategy();
        }
        return refStrategy;
    }

    /**
     * Adds a mapper used to update schema builders.
     *
     * @param schemaBuilderMapper Mapper to add.
     * @return Returns the converter.
     */
    public JsonSchemaConverter addSchemaMapper(SchemaBuilderMapper schemaBuilderMapper) {
        customMappers.add(schemaBuilderMapper);
        return this;
    }

    /**
     * Adds SchemaBuilderMapper instances discovered through SPI.
     *
     * <p>If this method is not called, then {@link SchemaBuilderMapper} services
     * will be discovered using the class loader of {@link JsonSchemaConverter}
     * and might only find the built-in service providers.
     *
     * @param classLoader ClassLoader used to discover implementations.
     * @return Returns the converter.
     */
    public JsonSchemaConverter discoverSchemaMappersWith(ClassLoader classLoader) {
        return loadMapperServices(ServiceLoader.load(SchemaBuilderMapper.class, classLoader));
    }

    private JsonSchemaConverter loadMapperServices(Iterable<SchemaBuilderMapper> mappers) {
        alreadyDiscoveredMappers = true;
        for (SchemaBuilderMapper mapper : mappers) {
            discoveredMappers.add(mapper);
        }
        return this;
    }

    /**
     * Perform the conversion of the entire shape Index.
     *
     * @param shapeIndex Shape index to convert.
     * @return Returns the created SchemaDocument.
     */
    public SchemaDocument convert(ShapeIndex shapeIndex) {
        return doConversion(shapeIndex, null);
    }

    /**
     * Perform the conversion of a single shape.
     *
     * <p>The root shape of the created document is set to the given shape,
     * and only shapes connected to the given shape are added as a definition.
     *
     * @param shapeIndex Shape index to convert.
     * @param shape Shape to convert.
     * @return Returns the created SchemaDocument.
     */
    public SchemaDocument convert(ShapeIndex shapeIndex, Shape shape) {
        return doConversion(shapeIndex, shape);
    }

    private SchemaDocument doConversion(ShapeIndex shapeIndex, Shape rootShape) {
        // If no mapper discovery was already invoked, then just use the current class loader.
        if (!alreadyDiscoveredMappers) {
            discoverSchemaMappersWith(getClass().getClassLoader());
        }

        // Combine custom mappers with the discovered mappers and sort them.
        List<SchemaBuilderMapper> resolvedMappers = new ArrayList<>(discoveredMappers);
        resolvedMappers.addAll(customMappers);
        resolvedMappers.sort(Comparator.comparing(SchemaBuilderMapper::getOrder));

        SchemaDocument.Builder builder = SchemaDocument.builder();
        JsonSchemaShapeVisitor visitor = new JsonSchemaShapeVisitor(
                shapeIndex, getConfig(), getRefStrategy(), getPropertyNamingStrategy(), resolvedMappers);

        if (rootShape != null && !(rootShape instanceof ServiceShape)) {
            builder.rootSchema(rootShape.accept(visitor));
        }

        addExtensions(builder);
        Predicate<Shape> predicate = composePredicate(shapeIndex, rootShape);
        shapeIndex.shapes()
                .filter(predicate)
                // Don't include members if their container was excluded.
                .filter(shape -> memberDefinitionPredicate(shapeIndex, shape, predicate))
                // Create the pointer to the shape and schema object.
                .map(shape -> Pair.of(
                        getRefStrategy().toPointer(shape.getId(), getConfig()),
                        shape.accept(visitor)))
                .forEach(pair -> builder.putDefinition(pair.getLeft(), pair.getRight()));

        return builder.build();
    }

    private Predicate<Shape> composePredicate(ShapeIndex shapeIndex, Shape rootShape) {
        // Don't write the root shape to the definitions.
        Predicate<Shape> predicate = (shape -> rootShape == null || !shape.getId().equals(rootShape.getId()));
        // Ignore any shape defined by the prelude.
        predicate = predicate.and(FunctionalUtils.not(Prelude::isPreludeShape));
        // Don't convert unsupported shapes.
        predicate = predicate.and(FunctionalUtils.not(this::isUnsupportedShapeType));
        // Don't convert excluded private shapes.
        predicate = predicate.and(shape -> !isExcludedPrivateShape(shapeIndex, shape));
        // Filter by the custom predicate.
        predicate = predicate.and(shapePredicate);

        // When a root shape is provided, only include shapes that are connected to it.
        // We *could* add a configuration option to not do this later if needed.
        if (rootShape != null) {
            Walker walker = new Walker(shapeIndex);
            Set<Shape> connected = walker.walkShapes(rootShape);
            predicate = predicate.and(connected::contains);
        }

        return predicate;
    }

    // We can't generate service, resource, or operation schemas.
    private boolean isUnsupportedShapeType(Shape shape) {
        return shape.isServiceShape() || shape.isResourceShape() || shape.isOperationShape();
    }

    // Only include members if not using INLINE_MEMBERS.
    private boolean memberDefinitionPredicate(ShapeIndex shapeIndex, Shape shape, Predicate<Shape> predicate) {
        if (!shape.isMemberShape()) {
            return true;
        } else if (getConfig().getBooleanMemberOrDefault(JsonSchemaConstants.INLINE_MEMBERS)) {
            return false;
        }

        // Don't include broken members or members of excluded shapes.
        return shape.asMemberShape()
                .flatMap(member -> shapeIndex.getShape(member.getContainer()))
                .filter(parent -> parent.equals(shape) || predicate.test(shape))
                .isPresent();
    }

    // Don't generate schemas for private shapes or members of private shapes.
    private boolean isExcludedPrivateShape(ShapeIndex shapeIndex, Shape shape) {
        // We can explicitly enable the generation of private shapes if desired.
        if (getConfig().getBooleanMemberOrDefault(JsonSchemaConstants.SMITHY_INCLUDE_PRIVATE_SHAPES)) {
            return false;
        } else if (shape.hasTrait(PrivateTrait.class)) {
            return true;
        }

        // Now check members.
        return shape.asMemberShape()
                .flatMap(member -> shapeIndex.getShape(member.getContainer()))
                .filter(parent -> parent.hasTrait(PrivateTrait.class))
                .isPresent();
    }

    private void addExtensions(SchemaDocument.Builder builder) {
        getConfig().getObjectMember(JsonSchemaConstants.SCHEMA_DOCUMENT_EXTENSIONS).ifPresent(builder::extensions);
    }
}
