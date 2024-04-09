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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a single JSON schema.
 *
 * <p>This supports a subset of the "draft-handrews-json-schema-validation-01"
 * version of JSON Schema. The following properties are not supported:
 *
 * <ul>
 *     <li>dependencies</li>
 *     <li>if</li>
 *     <li>then</li>
 *     <li>else</li>
 *     <li>examples</li>
 * </ul>
 *
 * <p>Custom properties can be added to the scheme using the
 * {@link Builder#putExtension} method. Custom properties are merged with
 * the schema document when serializing it to a {@link Node} object. Any
 * extension properties that conflict with built-in properties overwrite the
 * built-in property.
 *
 * @see <a href="https://json-schema.org/latest/json-schema-validation.html">JSON Schema Validation</a>
 */
public final class Schema implements ToNode, ToSmithyBuilder<Schema> {
    private static final Logger LOGGER = Logger.getLogger(Schema.class.getName());

    private final String ref;
    private final String type;
    private final Collection<String> enumValues;
    private final Collection<Integer> intEnumValues;
    private final Node constValue;
    private final Node defaultValue;

    private final Number multipleOf;
    private final Number maximum;
    private final Number exclusiveMaximum;
    private final Number minimum;
    private final Number exclusiveMinimum;

    private final Long maxLength;
    private final Long minLength;
    private final String pattern;

    private final Schema items;
    private final Integer maxItems;
    private final Integer minItems;
    private final boolean uniqueItems;

    private final Integer maxProperties;
    private final Integer minProperties;
    private final Collection<String> required;
    private final Map<String, Schema> properties;
    private final Schema additionalProperties;
    private final Schema propertyNames;
    private final Map<String, Schema> patternProperties;

    private final List<Schema> allOf;
    private final List<Schema> anyOf;
    private final List<Schema> oneOf;
    private final Schema not;

    private final String title;
    private final String description;
    private final String format;
    private final boolean readOnly;
    private final boolean writeOnly;
    private final String comment;
    private final Node examples;

    private final String contentEncoding;
    private final String contentMediaType;

    private final Map<String, ToNode> extensions;

    private Node asNode;

    private Schema(Builder builder) {
        ref = builder.ref;
        type = builder.type;
        enumValues = Collections.unmodifiableCollection(builder.enumValues);
        intEnumValues = Collections.unmodifiableCollection(builder.intEnumValues);
        constValue = builder.constValue;
        defaultValue = builder.defaultValue;

        multipleOf = builder.multipleOf;
        maximum = builder.maximum;
        exclusiveMaximum = builder.exclusiveMaximum;
        minimum = builder.minimum;
        exclusiveMinimum = builder.exclusiveMinimum;

        maxLength = builder.maxLength;
        minLength = builder.minLength;
        pattern = builder.pattern;

        items = builder.items;
        maxItems = builder.maxItems;
        minItems = builder.minItems;
        uniqueItems = builder.uniqueItems;

        properties = builder.properties;
        additionalProperties = builder.additionalProperties;
        required = ListUtils.copyOf(builder.required);
        maxProperties = builder.maxProperties;
        minProperties = builder.minProperties;
        propertyNames = builder.propertyNames;
        patternProperties = builder.patternProperties;

        allOf = ListUtils.copyOf(builder.allOf);
        oneOf = ListUtils.copyOf(builder.oneOf);
        anyOf = ListUtils.copyOf(builder.anyOf);
        not = builder.not;

        title = builder.title;
        description = builder.description;
        format = builder.format;
        readOnly = builder.readOnly;
        writeOnly = builder.writeOnly;
        comment = builder.comment;
        examples = builder.examples;

        contentEncoding = builder.contentEncoding;
        contentMediaType = builder.contentMediaType;

        extensions = MapUtils.copyOf(builder.extensions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getRef() {
        return Optional.ofNullable(ref);
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<Collection<String>> getEnumValues() {
        return Optional.ofNullable(enumValues);
    }

    public Optional<Collection<Integer>> getIntEnumValues() {
        return Optional.ofNullable(intEnumValues);
    }

    public Optional<Node> getConstValue() {
        return Optional.ofNullable(constValue);
    }

    public Optional<Node> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public Optional<Number> getMultipleOf() {
        return Optional.ofNullable(multipleOf);
    }

    public Optional<Number> getMaximum() {
        return Optional.ofNullable(maximum);
    }

    public Optional<Number> getExclusiveMaximum() {
        return Optional.ofNullable(exclusiveMaximum);
    }

    public Optional<Number> getMinimum() {
        return Optional.ofNullable(minimum);
    }

    public Optional<Number> getExclusiveMinimum() {
        return Optional.ofNullable(exclusiveMinimum);
    }

    public Optional<Long> getMaxLength() {
        return Optional.ofNullable(maxLength);
    }

    public Optional<Long> getMinLength() {
        return Optional.ofNullable(minLength);
    }

    public Optional<String> getPattern() {
        return Optional.ofNullable(pattern);
    }

    public Optional<Schema> getItems() {
        return Optional.ofNullable(items);
    }

    public Optional<Integer> getMaxItems() {
        return Optional.ofNullable(maxItems);
    }

    public Optional<Integer> getMinItems() {
        return Optional.ofNullable(minItems);
    }

    public boolean getUniqueItems() {
        return uniqueItems;
    }

    public Optional<Integer> getMaxProperties() {
        return Optional.ofNullable(maxProperties);
    }

    public Optional<Integer> getMinProperties() {
        return Optional.ofNullable(minProperties);
    }

    public Collection<String> getRequired() {
        return required;
    }

    public Map<String, Schema> getProperties() {
        return properties;
    }

    public Optional<Schema> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    public Optional<Schema> getAdditionalProperties() {
        return Optional.ofNullable(additionalProperties);
    }

    public Optional<Schema> getPropertyNames() {
        return Optional.ofNullable(propertyNames);
    }

    public Map<String, Schema> getPatternProperties() {
        return patternProperties;
    }

    public List<Schema> getAllOf() {
        return allOf;
    }

    public List<Schema> getAnyOf() {
        return anyOf;
    }

    public List<Schema> getOneOf() {
        return oneOf;
    }

    public Optional<Schema> getNot() {
        return Optional.ofNullable(not);
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getFormat() {
        return Optional.ofNullable(format);
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    public boolean getWriteOnly() {
        return writeOnly;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public Optional<Node> getExamples() {
        return Optional.ofNullable(examples);
    }

    public Optional<String> getContentEncoding() {
        return Optional.ofNullable(contentEncoding);
    }

    public Optional<String> getContentMediaType() {
        return Optional.ofNullable(contentMediaType);
    }

    public Optional<ToNode> getExtension(String key) {
        return Optional.ofNullable(extensions.get(key));
    }

    public Map<String, ToNode> getAllExtensions() {
        return extensions;
    }

    @Override
    public Node toNode() {
        if (asNode != null) {
            return asNode;
        }

        ObjectNode.Builder result = Node.objectNodeBuilder()
                .withOptionalMember("type", getType().map(Node::from))
                .withOptionalMember("$ref", getRef().map(Node::from))
                .withOptionalMember("const", getConstValue())
                .withOptionalMember("default", getDefaultValue())

                .withOptionalMember("multipleOf", getMultipleOf().map(Node::from))
                .withOptionalMember("maximum", getMaximum().map(Node::from))
                .withOptionalMember("exclusiveMaximum", getExclusiveMaximum().map(Node::from))
                .withOptionalMember("minimum", getMinimum().map(Node::from))
                .withOptionalMember("exclusiveMinimum", getExclusiveMinimum().map(Node::from))

                .withOptionalMember("items", getItems().map(ToNode::toNode))
                .withOptionalMember("maxItems", getMaxItems().map(Node::from))
                .withOptionalMember("minItems", getMinItems().map(Node::from))
                .withOptionalMember("uniqueItems", uniqueItems ? Optional.of(Node.from(true)) : Optional.empty())

                .withOptionalMember("maxLength", getMaxLength().map(Node::from))
                .withOptionalMember("minLength", getMinLength().map(Node::from))
                .withOptionalMember("pattern", getPattern().map(Node::from))

                .withOptionalMember("additionalProperties", getAdditionalProperties().map(Schema::toNode))
                .withOptionalMember("propertyNames", getPropertyNames().map(Schema::toNode))
                .withOptionalMember("maxProperties", getMaxProperties().map(Node::from))
                .withOptionalMember("minProperties", getMinProperties().map(Node::from))

                .withOptionalMember("not", getNot().map(Schema::toNode))

                .withOptionalMember("comment", getComment().map(Node::from))
                .withOptionalMember("examples", getExamples())
                .withOptionalMember("title", getTitle().map(Node::from))
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("format", getFormat().map(Node::from))

                .withOptionalMember("contentEncoding", getContentEncoding().map(Node::from))
                .withOptionalMember("contentMediaType", getContentMediaType().map(Node::from));

        if (!properties.isEmpty()) {
            result.withMember("properties", properties.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, e -> e.getValue().toNode())));
        }

        if (!patternProperties.isEmpty()) {
            result.withMember("patternProperties", patternProperties.entrySet().stream()
                    .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, e -> e.getValue().toNode())));
        }

        if (!required.isEmpty()) {
            result.withMember("required", required.stream().sorted().map(Node::from).collect(ArrayNode.collect()));
        }

        if (!enumValues.isEmpty() || !intEnumValues.isEmpty()) {
            ArrayNode.Builder builder = ArrayNode.builder();
            if (getIntEnumValues().isPresent()) {
                for (Integer i : getIntEnumValues().get()) {
                    builder.withValue(i);
                }
            }

            if (getEnumValues().isPresent()) {
                for (String s : getEnumValues().get()) {
                    builder.withValue(s);
                }
            }
            result.withOptionalMember("enum", builder.build().asArrayNode());
        }

        if (!allOf.isEmpty()) {
            result.withMember("allOf", allOf.stream().collect(ArrayNode.collect()));
        }

        if (!anyOf.isEmpty()) {
            result.withMember("anyOf", anyOf.stream().collect(ArrayNode.collect()));
        }

        if (!oneOf.isEmpty()) {
            result.withMember("oneOf", oneOf.stream().collect(ArrayNode.collect()));
        }

        if (readOnly) {
            result.withMember("readOnly", Node.from(true));
        }

        if (writeOnly) {
            result.withMember("writeOnly", Node.from(true));
        }

        for (Map.Entry<String, ToNode> entry : extensions.entrySet()) {
            result.withMember(entry.getKey(), entry.getValue().toNode());
        }

        asNode = result.build();
        return asNode;
    }

    /**
     * Selects a nested schema using a variadic list of property names
     * to descend into.
     *
     * <p>For example, this method can be used to get the items schema nested
     * inside of an array inside of an object:
     *
     * <pre>{@code
     * Schema itemsSchema = schema.selectSchema("properties", "foo", "items").get();
     * }</pre>
     *
     * @param segments The properties names to retrieve.
     * @return Returns the selected Schema.
     */
    public Optional<Schema> selectSchema(String... segments) {
        if (segments.length == 0) {
            return Optional.of(this);
        }

        String name = segments[0];
        switch (name) {
            case "properties":
                // Grab the property name if present, and skip 2 segments.
                return segments.length == 1
                       ? Optional.empty()
                       : getRecursiveSchema(getProperty(segments[1]), segments, 2);
            case "allOf":
                return getSchemaFromArray(allOf, segments);
            case "anyOf":
                return getSchemaFromArray(anyOf, segments);
            case "oneOf":
                return getSchemaFromArray(oneOf, segments);
            case "propertyNames":
                return getRecursiveSchema(getPropertyNames(), segments, 1);
            case "items":
                return getRecursiveSchema(getItems(), segments, 1);
            case "additionalProperties":
                return getAdditionalProperties();
            case "not":
                return getRecursiveSchema(getNot(), segments, 1);
            default:
                LOGGER.warning(() -> "Unsupported JSONPointer Schema segment: " + name);
                return Optional.empty();
        }
    }

    private Optional<Schema> getRecursiveSchema(Optional<Schema> schema, String[] segments, int skipOffset) {
        return schema.flatMap(s -> {
            String[] remainingSegments = Arrays.copyOfRange(segments, skipOffset, segments.length);
            return s.selectSchema(remainingSegments);
        });
    }

    private Optional<Schema> getSchemaFromArray(List<Schema> schemaArray, String[] segments) {
        if (segments.length == 1) {
            return Optional.empty();
        }

        try {
            int position = segments[1].equals("-") ? schemaArray.size() - 1 : Integer.parseInt(segments[1]);
            return position > -1 && position < schemaArray.size()
                   ? getRecursiveSchema(Optional.of(schemaArray.get(position)), segments, 2)
                   : Optional.empty();
        } catch (NumberFormatException e) {
            throw new SmithyJsonSchemaException("Invalid JSON pointer number: " + e.getMessage());
        }
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder()
                .ref(ref)
                .type(type)
                .enumValues(enumValues)
                .intEnumValues(intEnumValues)
                .constValue(constValue)
                .defaultValue(defaultValue)

                .multipleOf(multipleOf)
                .maximum(maximum)
                .exclusiveMaximum(exclusiveMaximum)
                .minimum(minimum)
                .exclusiveMinimum(exclusiveMinimum)

                .maxLength(maxLength)
                .minLength(minLength)
                .pattern(pattern)

                .items(items)
                .maxItems(maxItems)
                .minItems(minItems)
                .uniqueItems(uniqueItems)

                .required(required)
                .additionalProperties(additionalProperties)
                .maxProperties(maxProperties)
                .minProperties(minProperties)
                .propertyNames(propertyNames)

                .allOf(allOf)
                .anyOf(anyOf)
                .oneOf(oneOf)
                .not(not)

                .title(title)
                .description(description)
                .format(format)
                .readOnly(readOnly)
                .writeOnly(writeOnly)
                .comment(comment)
                .examples(examples)

                .contentEncoding(contentEncoding)
                .contentMediaType(contentMediaType);
        properties.forEach(builder::putProperty);
        patternProperties.forEach(builder::putPatternProperty);
        extensions.forEach(builder::putExtension);
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Schema)) {
            return false;
        } else {
            return toNode().equals(((Schema) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, type, properties, items);
    }

    /**
     * Abstract class used to build Schema components.
     */
    public static final class Builder implements SmithyBuilder<Schema> {
        private String ref;
        private String type;
        private Collection<String> enumValues = ListUtils.of();
        private Collection<Integer> intEnumValues = ListUtils.of();
        private Node constValue;
        private Node defaultValue;

        private Number multipleOf;
        private Number maximum;
        private Number exclusiveMaximum;
        private Number minimum;
        private Number exclusiveMinimum;

        private Long maxLength;
        private Long minLength;
        private String pattern;

        private Schema items;
        private Integer maxItems;
        private Integer minItems;
        private boolean uniqueItems;

        private Integer maxProperties;
        private Integer minProperties;
        private Collection<String> required = new ArrayList<>();
        private Map<String, Schema> properties = new LinkedHashMap<>();
        private Schema additionalProperties;
        private Schema propertyNames;
        private Map<String, Schema> patternProperties = new LinkedHashMap<>();

        private List<Schema> allOf = ListUtils.of();
        private List<Schema> anyOf = ListUtils.of();
        private List<Schema> oneOf = ListUtils.of();
        private Schema not;

        private String title;
        private String description;
        private String format;
        private boolean readOnly;
        private boolean writeOnly;
        private String comment;
        private Node examples;

        private String contentEncoding;
        private String contentMediaType;

        private final Map<String, ToNode> extensions = new HashMap<>();

        private Builder() {}

        @Override
        public Schema build() {
            return new Schema(this);
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder defaultValue(Node defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder enumValues(Collection<String> enumValues) {
            this.enumValues = enumValues == null ? ListUtils.of() : enumValues;
            return this;
        }

        public Builder intEnumValues(Collection<Integer> intEnumValues) {
            this.intEnumValues = intEnumValues == null ? ListUtils.of() : intEnumValues;
            return this;
        }

        public Builder constValue(Node constValue) {
            this.constValue = constValue;
            return this;
        }

        public Builder multipleOf(Number multipleOf) {
            this.multipleOf = multipleOf;
            return this;
        }

        public Builder maximum(Number maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder exclusiveMaximum(Number exclusiveMaximum) {
            this.exclusiveMaximum = exclusiveMaximum;
            return this;
        }

        public Builder minimum(Number minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder exclusiveMinimum(Number exclusiveMinimum) {
            this.exclusiveMinimum = exclusiveMinimum;
            return this;
        }

        public Builder maxLength(Long maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder minLength(Long minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder items(Schema items) {
            this.items = items;
            return this;
        }

        public Builder maxItems(Integer maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public Builder minItems(Integer minItems) {
            this.minItems = minItems;
            return this;
        }

        public Builder uniqueItems(boolean uniqueItems) {
            this.uniqueItems = uniqueItems;
            return this;
        }

        public Builder maxProperties(Integer maxProperties) {
            this.maxProperties = maxProperties;
            return this;
        }

        public Builder minProperties(Integer minProperties) {
            this.minProperties = minProperties;
            return this;
        }

        public Builder required(Collection<String> required) {
            if (required == null) {
                this.required.clear();
            } else {
                this.required = new ArrayList<>(required);
            }
            return this;
        }

        public Builder properties(Map<String, Schema> properties) {
            this.properties.clear();

            if (properties != null) {
                properties.forEach(this::putProperty);
            }

            return this;
        }

        public Builder putProperty(String key, Schema value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder removeProperty(String key) {
            properties.remove(key);
            required.remove(key);
            return this;
        }

        public Builder additionalProperties(Schema additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public Builder propertyNames(Schema propertyNames) {
            this.propertyNames = propertyNames;
            return this;
        }

        public Builder patternProperties(Map<String, Schema> patternProperties) {
            this.patternProperties.clear();

            if (patternProperties != null) {
                patternProperties.forEach(this::putPatternProperty);
            }

            return this;
        }

        public Builder putPatternProperty(String key, Schema value) {
            this.patternProperties.put(key, value);
            return this;
        }

        public Builder removePatternProperty(String key) {
            patternProperties.remove(key);
            return this;
        }

        public Builder allOf(List<Schema> allOf) {
            this.allOf = allOf == null ? ListUtils.of() : allOf;
            return this;
        }

        public Builder anyOf(List<Schema> anyOf) {
            this.anyOf = anyOf == null ? ListUtils.of() : anyOf;
            return this;
        }

        public Builder oneOf(List<Schema> oneOf) {
            this.oneOf = oneOf == null ? ListUtils.of() : oneOf;
            return this;
        }

        public Builder not(Schema not) {
            this.not = not;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Optional<String> getFormat() {
            return Optional.ofNullable(format);
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder writeOnly(boolean writeOnly) {
            this.writeOnly = writeOnly;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder contentEncoding(String contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        public Builder contentMediaType(String contentMediaType) {
            this.contentMediaType = contentMediaType;
            return this;
        }

        public Builder examples(Node examples) {
            this.examples = examples;
            return this;
        }

        public Builder extensions(Map<String, Node> extensions) {
            this.extensions.clear();
            this.extensions.putAll(extensions);
            return this;
        }

        public Builder putExtension(String key, ToNode value) {
            extensions.put(key, value);
            return this;
        }

        public Builder removeExtension(String key) {
            extensions.remove(key);
            return this;
        }

        /**
         * Disables a specific JSON schema property by name.
         *
         * @param propertyName Property name to remove (e.g., "propertyNames").
         * @return Returns the builder.
         */
        public Builder disableProperty(String propertyName) {
            switch (propertyName) {
                case "const":
                    return this.constValue(null);
                case "default":
                    return this.defaultValue(null);
                case "enum":
                    return this.enumValues(null).intEnumValues(null);
                case "multipleOf":
                    return this.multipleOf(null);
                case "maximum":
                    return this.maximum(null);
                case "exclusiveMaximum":
                    return this.exclusiveMaximum(null);
                case "minimum":
                    return this.minimum(null);
                case "exclusiveMinimum":
                    return this.exclusiveMinimum(null);
                case "maxLength":
                    return this.maxLength(null);
                case "minLength":
                    return this.minLength(null);
                case "pattern":
                    return this.pattern(null);
                case "items":
                    return this.items(null);
                case "maxItems":
                    return this.maxItems(null);
                case "minItems":
                    return this.minItems(null);
                case "uniqueItems":
                    return this.uniqueItems(false);
                case "properties":
                    return this.properties(null);
                case "additionalProperties":
                    return this.additionalProperties(null);
                case "required":
                    return this.required(null);
                case "maxProperties":
                    return this.maxProperties(null);
                case "minProperties":
                    return this.minProperties(null);
                case "propertyNames":
                    return this.propertyNames(null);
                case "allOf":
                    return this.allOf(null);
                case "anyOf":
                    return this.anyOf(null);
                case "oneOf":
                    return this.oneOf(null);
                case "not":
                    return this.not(null);
                case "title":
                    return this.title(null);
                case "description":
                    return this.description(null);
                case "format":
                    return this.format(null);
                case "readOnly":
                    return this.readOnly(false);
                case "writeOnly":
                    return this.writeOnly(false);
                case "comment":
                    return this.comment(null);
                case "contentEncoding":
                    return this.contentEncoding(null);
                case "contentMediaType":
                    return this.contentMediaType(null);
                case "examples":
                    return this.examples(null);
                default:
                    LOGGER.warning("Unknown JSON Schema config 'disable' property: " + propertyName);
                    return this;
            }
        }
    }
}
