/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a JSON Schema document.
 *
 * @see <a href="https://json-schema.org/latest/json-schema-core.html">JSON Schema specification</a>
 */
public final class SchemaDocument implements ToNode, ToSmithyBuilder<SchemaDocument> {
    private static final Logger LOGGER = Logger.getLogger(SchemaDocument.class.getName());

    private final String idKeyword;
    private final String schemaKeyword;
    private final Schema rootSchema;
    private final Map<String, Schema> definitions;
    private final ObjectNode extensions;

    private SchemaDocument(Builder builder) {
        idKeyword = builder.idKeyword;
        schemaKeyword = builder.schemaKeyword;
        rootSchema = builder.rootSchema != null ? builder.rootSchema : Schema.builder().build();
        definitions = new LinkedHashMap<>(builder.definitions);
        extensions = builder.extensions;
    }

    /**
     * Returns a builder used to create a {@link SchemaDocument}.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Node toNode() {
        ObjectNode definitionNode = Node.objectNode();

        if (!definitions.isEmpty()) {
            // Merge in each definition using a JSON Patch style "add" operation
            // that creates missing intermediate objects.
            for (Map.Entry<String, Schema> entry : definitions.entrySet()) {
                if (entry.getKey().startsWith("http")) {
                    LOGGER.info(() -> "Skipping the serialization of a remote schema reference: " + entry.getKey());
                } else {
                    NodePointer pointer = parseCheckedPointer(entry.getKey());
                    definitionNode = pointer
                            .addWithIntermediateValues(definitionNode, entry.getValue().toNode())
                            .expectObjectNode();
                }
            }
        }

        return Node.objectNodeBuilder()
                .withOptionalMember("$id", getIdKeyword().map(Node::from))
                .withOptionalMember("$schema", getSchemaKeyword().map(Node::from))
                .merge(rootSchema.toNode().expectObjectNode())
                .merge(extensions)
                .merge(definitionNode)
                .build()
                .withDeepSortedKeys(new SchemaComparator());
    }

    private NodePointer parseCheckedPointer(String pointer) {
        try {
            return NodePointer.parse(pointer);
        } catch (IllegalArgumentException e) {
            throw new SmithyJsonSchemaException("Invalid definition JSON pointer: " + e.getMessage(), e);
        }
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .idKeyword(idKeyword)
                .schemaKeyword(schemaKeyword)
                .rootSchema(rootSchema)
                .extensions(extensions);
        definitions.forEach(builder::putDefinition);
        return builder;
    }

    /**
     * Gets the root schema definition.
     *
     * @return Returns the root schema.
     * @see <a href="https://json-schema.org/latest/json-schema-core.html#rfc.section.4.3.3">Root schema</a>
     */
    public Schema getRootSchema() {
        return rootSchema;
    }

    /**
     * Gets the "$id" keyword of the document.
     *
     * @return Returns the optionally defined $id.
     * @see <a href="https://json-schema.org/latest/json-schema-core.html#rfc.section.8.2">$id</a>
     */
    public Optional<String> getIdKeyword() {
        return Optional.ofNullable(idKeyword);
    }

    /**
     * Gets the "$schema" keyword of the document.
     *
     * @return Returns the optionally defined $schema.
     * @see <a href="https://json-schema.org/latest/json-schema-core.html#rfc.section.7">$schema</a>
     */
    public Optional<String> getSchemaKeyword() {
        return Optional.ofNullable(schemaKeyword);
    }

    /**
     * Gets a schema definition from the "definitions" map using a JSON pointer.
     *
     * <p>The "root" schema is returned if {@code pointer} is an empty string.
     *
     * @param pointer JSON Schema pointer to retrieve.
     * @return Returns the optionally found schema definition.
     */
    public Optional<Schema> getDefinition(String pointer) {
        String unescaped = NodePointer.unescape(pointer);

        // Attempt to get the unescaped pointer, as-is.
        if (definitions.containsKey(unescaped)) {
            return Optional.ofNullable(definitions.get(unescaped));
        }

        List<String> pointerParts = NodePointer.parse(pointer).getParts();

        // An empty pointer returns the root schema.
        if (pointerParts.isEmpty()) {
            return Optional.of(getRootSchema());
        }

        // Compute the part of the pointer that points at a literal entry in
        // the map of definitions, and then compute the remaining pointer
        // segments that need to be used when retrieving a nested schema.
        String prefix = pointer.startsWith("#") ? "#" : "";
        for (int position = 0; position < pointerParts.size(); position++) {
            String part = pointerParts.get(position);
            prefix += '/' + part;
            if (definitions.containsKey(prefix)) {
                List<String> remaining = pointerParts.subList(position + 1, pointerParts.size());
                String[] suffix = remaining.toArray(new String[0]);
                return definitions.get(prefix).selectSchema(suffix);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all of the schema definitions defined in the "definitions" map.
     *
     * @return Returns the defined schema definitions.
     * @see <a href="https://json-schema.org/latest/json-schema-validation.html#rfc.section.9">Schema reuse with "definitions"</a>
     */
    public Map<String, Schema> getDefinitions() {
        return definitions;
    }

    /**
     * Gets an extension value by name.
     *
     * @param key Name of the extension to retrieve.
     * @return Returns the extension object.
     */
    public Optional<Node> getExtension(String key) {
        return extensions.getMember(key);
    }

    /**
     * Gets all extensions of the schema document.
     *
     * @return Returns the extensions added to the schema.
     */
    public ObjectNode getExtensions() {
        return extensions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SchemaDocument)) {
            return false;
        }

        SchemaDocument that = (SchemaDocument) o;
        return Objects.equals(idKeyword, that.idKeyword)
                && Objects.equals(schemaKeyword, that.schemaKeyword)
                && rootSchema.equals(that.rootSchema)
                && definitions.equals(that.definitions)
                && extensions.equals(that.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idKeyword, schemaKeyword, rootSchema);
    }

    /**
     * Builds a JSON Schema document.
     */
    public static final class Builder implements SmithyBuilder<SchemaDocument> {
        private String idKeyword;
        private String schemaKeyword;
        private Schema rootSchema;
        private ObjectNode extensions = Node.objectNode();
        private final Map<String, Schema> definitions = new LinkedHashMap<>();

        private Builder() {}

        @Override
        public SchemaDocument build() {
            return new SchemaDocument(this);
        }

        /**
         * Sets the "$id" keyword.
         *
         * @param idKeyword ID keyword URI to set.
         * @return Returns the builder.
         */
        public Builder idKeyword(String idKeyword) {
            this.idKeyword = idKeyword;
            return this;
        }

        /**
         * Sets the "$schema" keyword.
         *
         * @param schemaKeyword Schema keyword URI to set.
         * @return Returns the builder.
         */
        public Builder schemaKeyword(String schemaKeyword) {
            this.schemaKeyword = schemaKeyword;
            return this;
        }

        /**
         * Sets the root schema.
         *
         * @param rootSchema Root schema of the document.
         * @return Returns the builder.
         */
        public Builder rootSchema(Schema rootSchema) {
            this.rootSchema = rootSchema;
            return this;
        }

        /**
         * Adds a scheme definition to the builder.
         *
         * @param name Name of the schema.
         * @param schema Schema to associate to the name.
         * @return Returns the builder.
         */
        public Builder putDefinition(String name, Schema schema) {
            definitions.put(name, schema);
            return this;
        }

        /**
         * Removes a schema definition by name.
         *
         * @param name Name of the schema to remove.
         * @return Returns the builder.
         */
        public Builder removeDefinition(String name) {
            definitions.remove(name);
            return this;
        }

        /**
         * Adds custom key-value pairs to the resulting JSON Schema document.
         *
         * @param extensions Extensions to apply.
         * @return Returns the builder.
         */
        public Builder extensions(ObjectNode extensions) {
            this.extensions = extensions;
            return this;
        }
    }
}
