/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Converts a Smithy model index to a JSON schema document.
 */
public final class JsonSchemaConverter implements ToSmithyBuilder<JsonSchemaConverter> {

    private static final Logger LOGGER = Logger.getLogger(JsonSchemaConverter.class.getName());

    private static final PropertyNamingStrategy DEFAULT_PROPERTY_STRATEGY = PropertyNamingStrategy
            .createDefaultStrategy();

    /** All converters use the built-in mappers. */
    private final List<JsonSchemaMapper> mappers = new ArrayList<>();

    private final Model model;
    private final PropertyNamingStrategy propertyNamingStrategy;
    private JsonSchemaConfig config;
    private final Predicate<Shape> shapePredicate;
    private final RefStrategy refStrategy;
    private final List<JsonSchemaMapper> realizedMappers;
    private final JsonSchemaShapeVisitor visitor;
    private final Shape rootShape;
    private final String rootDefinitionPointer;
    private final int rootDefinitionSegments;

    /** A workaround for including definitions for Unit; it's only included in the schema if a union targets it. */
    private final boolean unitTargetedByUnion;

    private JsonSchemaConverter(Builder builder) {
        mappers.addAll(builder.mappers);
        config = SmithyBuilder.requiredState("config", builder.config);
        propertyNamingStrategy = SmithyBuilder.requiredState("propertyNamingStrategy", builder.propertyNamingStrategy);

        // Flatten mixins out of the model before using the model at all. Mixins are
        // not relevant to JSON Schema documents.
        Model builderModel = SmithyBuilder.requiredState("model", builder.model);
        model = ModelTransformer.create().flattenAndRemoveMixins(builderModel);

        shapePredicate = builder.shapePredicate;

        LOGGER.fine("Building filtered JSON schema shape index");

        if (builder.rootShape == null) {
            rootShape = null;
        } else {
            rootShape = builder.model.getShape(builder.rootShape)
                    .orElseThrow(() -> new SmithyJsonSchemaException(
                            "Invalid root shape (shape not found): " + builder.rootShape));
        }

        LOGGER.fine("Creating JSON ref strategy");
        Model refModel = config.isEnableOutOfServiceReferences()
                ? this.model
                : scopeModelToService(model, config.getService());

        unitTargetedByUnion = refModel.shapes(UnionShape.class)
                .anyMatch(u -> u.members().stream().anyMatch(m -> m.getTarget().equals(UnitTypeTrait.UNIT)));

        refStrategy = RefStrategy.createDefaultStrategy(refModel,
                config,
                propertyNamingStrategy,
                new FilterPreludeUnit(unitTargetedByUnion));

        // Combine custom mappers with the discovered mappers and sort them.
        realizedMappers = new ArrayList<>(mappers);
        realizedMappers.add(new DisableMapper());
        realizedMappers.add(new TimestampMapper());
        realizedMappers.sort(Comparator.comparing(JsonSchemaMapper::getOrder));
        LOGGER.fine(() -> "Adding the following JSON schema mappers: " + realizedMappers.stream()
                .map(Object::getClass)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ")));
        visitor = new JsonSchemaShapeVisitor(this.model, this, realizedMappers);

        // Compute the number of segments in the root definition section.
        rootDefinitionPointer = config.getDefinitionPointer();
        rootDefinitionSegments = countSegments(rootDefinitionPointer);
        LOGGER.fine(() -> "Using the following root JSON schema pointer: " + rootDefinitionPointer
                + " (" + rootDefinitionSegments + " segments)");
    }

    private static Model createUpdatedModel(
            Model model,
            Shape rootShape,
            Predicate<Shape> predicate
    ) {
        ModelTransformer transformer = ModelTransformer.create();

        if (rootShape != null) {
            LOGGER.fine(() -> "Filtering out shapes that are not connected to " + rootShape);
            Set<Shape> connected = new Walker(model).walkShapes(rootShape);
            LOGGER.fine(() -> "Only generating the following JSON schema shapes: " + connected.stream()
                    .map(Shape::getId)
                    .map(ShapeId::toString)
                    .collect(Collectors.joining(", ")));
            model = transformer.filterShapes(model, connected::contains);
        }

        model = transformer.filterShapes(model, predicate);

        // Traits and their shapes are not generated into the OpenAPI schema.
        model = transformer.scrubTraitDefinitions(model);

        return model;
    }

    private static Model scopeModelToService(Model model, ShapeId serviceId) {
        if (serviceId == null) {
            return model;
        }
        Set<Shape> connected = new Walker(model).walkShapes(model.expectShape(serviceId));
        return ModelTransformer.create().filterShapes(model, connected::contains);
    }

    private static int countSegments(String pointer) {
        int totalSegments = 0;
        for (int i = 0; i < pointer.length(); i++) {
            if (pointer.charAt(i) == '/') {
                totalSegments++;
            }
        }

        return totalSegments;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the configuration object.
     *
     * @return Returns the config object.
     */
    public JsonSchemaConfig getConfig() {
        return config;
    }

    /**
     * Set the JSON Schema configuration settings.
     *
     * @param config Config object to set.
     */
    public void setConfig(JsonSchemaConfig config) {
        this.config = config;
    }

    /**
     * Gets the property naming strategy of the converter.
     *
     * @param member Member to convert to a property name.
     * @return Returns the PropertyNamingStrategy.
     */
    public String toPropertyName(MemberShape member) {
        Shape containingShape = model.getShape(member.getContainer())
                .orElseThrow(() -> new SmithyJsonSchemaException("Invalid member: " + member));
        return propertyNamingStrategy.toPropertyName(containingShape, member, config);
    }

    /**
     * Given a shape ID, returns the value used in a $ref to refer to it.
     *
     * <p>The return value is expected to be a JSON pointer.
     *
     * @param id Shape ID to convert to a $ref string.
     * @return Returns the $ref string (e.g., "#/responses/MyShape").
     */
    public String toPointer(ToShapeId id) {
        return refStrategy.toPointer(id.toShapeId());
    }

    /**
     * Checks if the given JSON pointer points to a top-level definition.
     *
     * <p>Note that this expects the pointer to exactly start with the same
     * string that is configured as {@link JsonSchemaConfig#getDefinitionPointer()},
     * or the default value of "#/definitions". If the number of segments
     * in the provided pointer is also equal to the number of segments
     * in the default pointer + 1, then it is considered a top-level pointer.
     *
     * @param pointer Pointer to check.
     * @return Returns true if this is a top-level definition pointer.
     */
    public boolean isTopLevelPointer(String pointer) {
        return pointer.startsWith(rootDefinitionPointer)
                && countSegments(pointer) == rootDefinitionSegments + 1;
    }

    /**
     * Checks if the given shape is inlined into its container when targeted
     * by a member.
     *
     * @param shape Shape to check.
     * @return Returns true if this shape is inlined into its containing shape.
     */
    public boolean isInlined(Shape shape) {
        return refStrategy.isInlined(shape);
    }

    /**
     * Perform the conversion of the entire shape index.
     *
     * @return Returns the created SchemaDocument.
     */
    public SchemaDocument convert() {
        LOGGER.fine("Converting to JSON schema");
        SchemaDocument.Builder builder = SchemaDocument.builder();

        if (rootShape != null && !(rootShape instanceof ServiceShape)) {
            LOGGER.fine(() -> "Setting root schema to " + rootShape);
            builder.rootSchema(rootShape.accept(visitor));
        }

        addExtensions(builder);

        // Create a model that strips out traits and disconnected shapes.
        Model updatedModel = createUpdatedModel(model, rootShape, shapePredicate);

        model.shapes()
                // Only generate shapes that passed through each predicate.
                .filter(shape -> updatedModel.getShape(shape.getId()).isPresent())
                // Don't generate members.
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                // Don't write the root shape to the definitions.
                .filter((shape -> rootShape == null || !shape.getId().equals(rootShape.getId())))
                // Don't convert unsupported shapes.
                .filter(FunctionalUtils.not(this::isUnsupportedShapeType))
                // Ignore prelude shapes.
                .filter(s -> s.getId().equals(UnitTypeTrait.UNIT) && unitTargetedByUnion || !Prelude.isPreludeShape(s))
                // Do not generate inlined shapes in the definitions map.
                .filter(FunctionalUtils.not(refStrategy::isInlined))
                // Create a pair of pointer and shape.
                .map(shape -> Pair.of(toPointer(shape), shape))
                // Only add definitions if they are at the top-level and not inlined.
                .filter(pair -> isTopLevelPointer(pair.getLeft()))
                // Create the pointer to the shape and schema object.
                .map(pair -> {
                    LOGGER.fine(() -> "Converting " + pair.getRight() + " to JSON schema at " + pair.getLeft());
                    return Pair.of(pair.getLeft(), pair.getRight().accept(visitor));
                })
                .forEach(pair -> builder.putDefinition(pair.getLeft(), pair.getRight()));

        LOGGER.fine(() -> "Completed JSON schema document conversion (root shape: " + rootShape + ")");

        return builder.build();
    }

    /**
     * Perform the conversion of a single shape.
     *
     * <p>The root shape of the created document is set to the given shape.
     * No schema extensions are added to the converted schema. This
     * conversion also doesn't take the shape predicate or private
     * controls into account.
     *
     * @param shape Shape to convert.
     * @return Returns the created SchemaDocument.
     */
    public SchemaDocument convertShape(Shape shape) {
        SchemaDocument.Builder builder = SchemaDocument.builder();
        builder.rootSchema(shape.accept(visitor));
        return builder.build();
    }

    // We can't generate service, resource, or operation schemas.
    private boolean isUnsupportedShapeType(Shape shape) {
        return shape.isServiceShape() || shape.isResourceShape() || shape.isOperationShape();
    }

    private void addExtensions(SchemaDocument.Builder builder) {
        ObjectNode extensions = config.getSchemaDocumentExtensions();

        if (!extensions.isEmpty()) {
            LOGGER.fine(() -> "Adding JSON schema extensions: " + Node.prettyPrintJson(extensions));
            builder.extensions(extensions);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .model(model)
                .propertyNamingStrategy(propertyNamingStrategy)
                .config(config)
                .rootShape(rootShape == null ? null : rootShape.getId())
                .shapePredicate(shapePredicate)
                .mappers(mappers);
    }

    public static final class Builder implements SmithyBuilder<JsonSchemaConverter> {

        private Model model;
        private ShapeId rootShape;
        private PropertyNamingStrategy propertyNamingStrategy = DEFAULT_PROPERTY_STRATEGY;
        private JsonSchemaConfig config = new JsonSchemaConfig();
        private Predicate<Shape> shapePredicate = shape -> true;
        private final List<JsonSchemaMapper> mappers = new ArrayList<>();

        private Builder() {}

        @Override
        public JsonSchemaConverter build() {
            return new JsonSchemaConverter(this);
        }

        /**
         * Sets the shape index to convert.
         *
         * @param model Shape index to convert.
         * @return Returns the builder.
         */
        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        /**
         * Only generates shapes connected to the given shape and set the
         * given shape as the root of the created schema document.
         *
         * @param rootShape ID of the shape that is used to limit
         *   the closure of the generated document.
         * @return Returns the builder.
         */
        public Builder rootShape(ToShapeId rootShape) {
            this.rootShape = rootShape == null ? null : rootShape.toShapeId();
            return this;
        }

        /**
         * Sets a predicate used to filter Smithy shapes from being converted
         * to JSON Schema.
         *
         * @param shapePredicate Predicate that returns true if a shape is to be converted.
         * @return Returns the converter.
         */
        public Builder shapePredicate(Predicate<Shape> shapePredicate) {
            this.shapePredicate = Objects.requireNonNull(shapePredicate);
            return this;
        }

        /**
         * Sets the configuration object.
         *
         * @param config Config to use.
         * @return Returns the converter.
         */
        public Builder config(JsonSchemaConfig config) {
            this.config = Objects.requireNonNull(config);
            return this;
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
        public Builder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
            this.propertyNamingStrategy = Objects.requireNonNull(propertyNamingStrategy);
            return this;
        }

        /**
         * Adds a mapper used to update schema builders.
         *
         * @param jsonSchemaMapper Mapper to add.
         * @return Returns the converter.
         */
        public Builder addMapper(JsonSchemaMapper jsonSchemaMapper) {
            mappers.add(Objects.requireNonNull(jsonSchemaMapper));
            return this;
        }

        /**
         * Replaces the mappers of the builder with the given mappers.
         *
         * @param jsonSchemaMappers Mappers to replace with.
         * @return Returns the converter.
         */
        public Builder mappers(List<JsonSchemaMapper> jsonSchemaMappers) {
            mappers.clear();
            mappers.addAll(jsonSchemaMappers);
            return this;
        }
    }

    static final class FilterPreludeUnit implements Predicate<Shape> {
        private final boolean includePreludeUnit;

        FilterPreludeUnit(boolean includePreludeUnit) {
            this.includePreludeUnit = includePreludeUnit;
        }

        @Override
        public boolean test(Shape shape) {
            return includePreludeUnit || !shape.getId().equals(UnitTypeTrait.UNIT);
        }
    }
}
