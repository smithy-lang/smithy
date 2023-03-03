/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * JSON Schema configuration options.
 */
public class JsonSchemaConfig {

    /**
     * Configures how Smithy union shapes are converted to JSON Schema.
     */
    public enum UnionStrategy {
        /**
         * Converts to a schema that uses "oneOf".
         *
         * <p>This is the default setting used if not configured.
         */
        ONE_OF("oneOf"),

        /**
         * Converts to an empty object "{}".
         */
        OBJECT("object"),

        /**
         * Converts to an object with properties just like a structure.
         */
        STRUCTURE("structure");

        private final String stringValue;

        UnionStrategy(String stringValue) {
            this.stringValue = stringValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    /**
     * Configures how Smithy map shapes are converted to JSON Schema.
     */
    public enum MapStrategy {
        /**
         * Converts to a schema that uses a combination of "propertyNames"
         * and "additionalProperties".
         *
         * <p>This is the default setting used if not configured.
         */
        PROPERTY_NAMES("propertyNames"),

        /**
         * Converts to a schema that uses "patternProperties". If a map's key
         * member or its target does not have a {@code pattern} trait, a default
         * indicating one or more of any character (".+") is applied.
         */
        PATTERN_PROPERTIES("patternProperties");

        private final String stringValue;

        MapStrategy(String stringValue) {
            this.stringValue = stringValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    private boolean alphanumericOnlyRefs;
    private boolean useJsonName;
    private TimestampFormatTrait.Format defaultTimestampFormat = TimestampFormatTrait.Format.DATE_TIME;
    private UnionStrategy unionStrategy = UnionStrategy.ONE_OF;
    private MapStrategy mapStrategy = MapStrategy.PROPERTY_NAMES;
    private String definitionPointer;
    private ObjectNode schemaDocumentExtensions = Node.objectNode();
    private ObjectNode extensions = Node.objectNode();
    private Set<String> disableFeatures = new HashSet<>();
    private JsonSchemaVersion jsonSchemaVersion = JsonSchemaVersion.DRAFT07;
    private final ConcurrentHashMap<Class<?>, Object> extensionCache = new ConcurrentHashMap<>();
    private final NodeMapper nodeMapper = new NodeMapper();
    private ShapeId service;
    private boolean supportNonNumericFloats = false;
    private boolean enableOutOfServiceReferences = false;
    private boolean useIntegerType;

    public JsonSchemaConfig() {
        nodeMapper.setWhenMissingSetter(NodeMapper.WhenMissing.IGNORE);
    }

    public boolean getAlphanumericOnlyRefs() {
        return alphanumericOnlyRefs;
    }

    /**
     * Creates shape name pointers that strip out non-alphanumeric characters.
     *
     * <p>This is necessary for compatibility with some vendors like
     * Amazon API Gateway that only allow alphanumeric shape names.
     *
     * @param alphanumericOnlyRefs Set to true to strip non-alphanumeric characters.
     */
    public void setAlphanumericOnlyRefs(boolean alphanumericOnlyRefs) {
        this.alphanumericOnlyRefs = alphanumericOnlyRefs;
    }

    public boolean getUseJsonName() {
        return useJsonName;
    }

    /**
     * Uses the value of the jsonName trait when creating JSON schema
     * properties for structure and union shapes.
     *
     * <p>This property has no effect if a {@link PropertyNamingStrategy} is
     * manually configured on a {@link JsonSchemaConverter}.
     *
     * @param useJsonName Set to true to use jsonName traits when creating refs..
     */
    public void setUseJsonName(boolean useJsonName) {
        this.useJsonName = useJsonName;
    }

    public TimestampFormatTrait.Format getDefaultTimestampFormat() {
        return defaultTimestampFormat;
    }

    /**
     * Sets the assumed timestampFormat trait for timestamps with no
     * timestampFormat trait. The provided value is expected to be a string.
     *
     * <p>Defaults to "date-time" if not set. Can be set to "date-time",
     * "epoch-seconds", or "http-date".
     *
     * @param defaultTimestampFormat The default timestamp format to use when none is set.
     */
    public void setDefaultTimestampFormat(TimestampFormatTrait.Format defaultTimestampFormat) {
        this.defaultTimestampFormat = defaultTimestampFormat;
    }

    public UnionStrategy getUnionStrategy() {
        return unionStrategy;
    }

    /**
     * Configures how Smithy union shapes are converted to JSON Schema.
     *
     * @param unionStrategy The union strategy to use.
     */
    public void setUnionStrategy(UnionStrategy unionStrategy) {
        this.unionStrategy = unionStrategy;
    }

    public MapStrategy getMapStrategy() {
        return mapStrategy;
    }

    /**
     * Configures how Smithy map shapes are converted to JSON Schema.
     *
     * @param mapStrategy The map strategy to use.
     */
    public void setMapStrategy(MapStrategy mapStrategy) {
        this.mapStrategy = mapStrategy;
    }

    public String getDefinitionPointer() {
        return definitionPointer != null ? definitionPointer : jsonSchemaVersion.getDefaultDefinitionPointer();
    }

    /**
     * Configures location of where definitions are written using JSON Pointer.
     *
     * <p>The provided String value MUST start with "#/" and can use nested "/"
     * characters to place schemas in nested object properties. The provided
     * JSON Pointer does not support escaping.
     *
     * <p>Defaults to {@code "#/definitions"} for schema versions less than 2019-09 and {@code "#/$defs"} for schema
     * versions 2019-09 and greater. OpenAPI artifacts will want to use "#/components/schemas".
     *
     * @param definitionPointer The root definition pointer to use.
     */
    public void setDefinitionPointer(String definitionPointer) {
        this.definitionPointer = Objects.requireNonNull(definitionPointer);
    }

    public ObjectNode getSchemaDocumentExtensions() {
        return schemaDocumentExtensions;
    }

    /**
     * Adds custom key-value pairs to the JSON Schema document generated from
     * a Smithy model.
     *
     * @param schemaDocumentExtensions Custom extensions to merge into the created schema.
     */
    public void setSchemaDocumentExtensions(ObjectNode schemaDocumentExtensions) {
        this.schemaDocumentExtensions = Objects.requireNonNull(schemaDocumentExtensions);
    }

    public Set<String> getDisableFeatures() {
        return disableFeatures;
    }

    /**
     * Disables OpenAPI features by their property name (e.g., "allOf").
     *
     * @param disableFeatures Feature names to disable.
     */
    public void setDisableFeatures(Set<String> disableFeatures) {
        this.disableFeatures = disableFeatures;
    }

    public ObjectNode getExtensions() {
        return extensions;
    }

    /**
     * Attempts to deserialize the {@code extensions} into the targeted
     * type using a {@link NodeMapper}.
     *
     * <p>Extraneous properties are ignored and <em>not</em> warned on
     * because many different plugins could be used with different
     * configuration POJOs.
     *
     * <p>The result of calling this method is cached for each type,
     * and the cache is cleared when any mutation is made to
     * extensions.
     *
     * @param as Type to deserialize extensions into.
     * @param <T> Type to deserialize extensions into.
     * @return Returns the deserialized type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtensions(Class<T> as) {
        return (T) extensionCache.computeIfAbsent(as, t -> nodeMapper.deserialize(extensions, t));
    }

    /**
     * Sets an arbitrary map of "extensions" used by plugins that need
     * configuration.
     *
     * @param extensions Extensions to set.
     */
    public void setExtensions(ObjectNode extensions) {
        this.extensions = Objects.requireNonNull(extensions);
        extensionCache.clear();
    }

    /**
     * Add an extension to the "extensions" object node using a POJO.
     *
     * @param extensionContainer POJO to serialize and merge into extensions.
     */
    public void putExtensions(Object extensionContainer) {
        ObjectNode serialized = nodeMapper.serialize(extensionContainer).expectObjectNode();
        setExtensions(extensions.merge(serialized));
    }

    /**
     * Add an extension to the "extensions" object node.
     *
     * @param key Property name to set.
     * @param value Value to assigned.
     */
    public void putExtension(String key, Node value) {
        setExtensions(extensions.withMember(key, value));
    }

    /**
     * Add an extension to the "extensions" object node.
     *
     * @param key Property name to set.
     * @param value Value to assigned.
     */
    public void putExtension(String key, boolean value) {
        putExtension(key, Node.from(value));
    }

    /**
     * Add an extension to the "extensions" object node.
     *
     * @param key Property name to set.
     * @param value Value to assigned.
     */
    public void putExtension(String key, String value) {
        putExtension(key, Node.from(value));
    }

    /**
     * Gets the service shape ID that is used to contextualize the created
     * schemas by using things like the "rename" property of the service.
     *
     * @return the nullable Smithy service shape ID.
     */
    public ShapeId getService() {
        return service;
    }

    /**
     * Sets the service shape ID that is used to contextualize the created
     * schemas by using things like the "rename" property of the service.
     *
     * @param service the Smithy service shape ID.
     */
    public void setService(ShapeId service) {
        this.service = service;
    }

    /**
     * Detects the TimestampFormat of the given shape, falling back to the
     * configured default format specified in {@link #setDefaultTimestampFormat(TimestampFormatTrait.Format)}.
     *
     * @param shape Shape to extract the timestamp format from.
     * @return Returns the optionally detected format.
     */
    public Optional<String> detectJsonTimestampFormat(Shape shape) {
        if (shape.isTimestampShape() || shape.hasTrait(TimestampFormatTrait.class)) {
            return Optional.of(shape.getTrait(TimestampFormatTrait.class)
                    .map(TimestampFormatTrait::getValue)
                    .orElseGet(() -> getDefaultTimestampFormat().toString()));
        }
        return Optional.empty();
    }

    public boolean getSupportNonNumericFloats() {
        return supportNonNumericFloats;
    }

    /**
     * Set to true to add support for NaN, Infinity, and -Infinity in float
     * and double shapes. These values will be serialized as strings. The
     * OpenAPI document will be updated to refer to them as a "oneOf" of number
     * and string.
     *
     * <p>By default, non-numeric values are not supported.
     *
     * @param supportNonNumericFloats True if non-numeric float values should be supported.
     */
    public void setSupportNonNumericFloats(boolean supportNonNumericFloats) {
        this.supportNonNumericFloats = supportNonNumericFloats;
    }

    public boolean isEnableOutOfServiceReferences() {
        return enableOutOfServiceReferences;
    }

    /**
     * Set to true to enable references to shapes outside the service closure.
     *
     * Setting this to true means that all the shapes in the model must not conflict, whereas
     * leaving it at the default, false, means that only the shapes connected to the configured
     * service via {@link #setService(ShapeId)} must not conflict.
     *
     * @param enableOutOfServiceReferences true if out-of-service references should be allowed. default: false
     */
    public void setEnableOutOfServiceReferences(boolean enableOutOfServiceReferences) {
        this.enableOutOfServiceReferences = enableOutOfServiceReferences;
    }


    public boolean getUseIntegerType() {
        return useIntegerType;
    }

    /**
     * Set to true to use the "integer" type when converting {@code byte},
     * {@code short}, {@code integer}, and {@code long} shapes to Json Schema.
     *
     * <p>By default, these shape types are converted to Json Schema with a type
     * of "number".
     *
     * @param useIntegerType True to use "integer".
     */
    public void setUseIntegerType(boolean useIntegerType) {
        this.useIntegerType = useIntegerType;
    }


    /**
     * JSON schema version to use when converting Smithy shapes into Json Schema.
     *
     * <p> Defaults to JSON Schema version {@code draft07} if no schema version is specified
     *
     * @return JSON Schema version that will be used for generated JSON schema
     */
    public JsonSchemaVersion getJsonSchemaVersion() {
        return jsonSchemaVersion;
    }

    /**
     * Set the JSON schema version to use when converting Smithy shapes into Json Schema.
     *
     * @param schemaVersion JSON Schema version to use for generated schema
     */
    public void setJsonSchemaVersion(JsonSchemaVersion schemaVersion) {
        this.jsonSchemaVersion = Objects.requireNonNull(schemaVersion);
    }
}
