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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
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
            Map<StringNode, Object> nodes = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : definitions.entrySet()) {
                updateIn(nodes, entry.getKey(), entry.getValue().toNode());
            }
            definitionNode = Node.objectNode(convertMap(nodes));
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

    @SuppressWarnings("unchecked")
    private void updateIn(Map<StringNode, Object> map, String key, Node value) {
        if (!key.startsWith("#/")) {
            LOGGER.warning(() -> "Unable to serialize a node for definition JSON pointer: "
                                 + key + ". Can only serialize pointers that start with '#/'.");
            return;
        }

        // Skip "#/" and split by "/".
        String[] paths = key.substring(2).split("/");
        if (paths.length <= 1) {
            throw new SmithyJsonSchemaException("Invalid definition JSON pointer. Expected more segments: " + key);
        }

        // Iterate up to the second to last path segment to find the parent.
        Map<StringNode, Object> current = map;
        for (int i = 0; i < paths.length - 1; i++) {
            StringNode pathNode = Node.from(paths[i]);
            if (!current.containsKey(pathNode)) {
                Map<StringNode, Object> newEntry = new LinkedHashMap<>();
                current.put(pathNode, newEntry);
                current = newEntry;
            } else if (!(current.get(pathNode) instanceof Map)) {
                // This could happen when two keys collide. We don't support things
                // like opening up one schema and inlining another inside of it.
                throw new SmithyJsonSchemaException("Conflicting JSON pointer definition found at " + key);
            } else {
                current = (Map<StringNode, Object>) current.get(pathNode);
            }
        }

        current.put(Node.from(paths[paths.length - 1]), value);
    }

    @SuppressWarnings("unchecked")
    private Map<StringNode, Node> convertMap(Map<StringNode, Object> map) {
        Map<StringNode, Node> result = new HashMap<>();

        for (Map.Entry<StringNode, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Node) {
                result.put(entry.getKey(), (Node) entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                Map<StringNode, Node> valueResult = convertMap((Map<StringNode, Object>) entry.getValue());
                result.put(entry.getKey(), Node.objectNode(valueResult));
            }
        }

        return result;
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
        pointer = unescapeJsonSchema(pointer);

        if (definitions.containsKey(pointer)) {
            return Optional.ofNullable(definitions.get(pointer));
        } else if (pointer.isEmpty()) {
            return Optional.of(getRootSchema());
        }

        String prefix = "";
        String[] refs = pointer.split("/");

        for (int position = 0; position < refs.length; position++) {
            if (position > 0) {
                prefix += "/" + refs[position];
            } else {
                prefix += refs[position];
            }
            if (definitions.containsKey(prefix)) {
                String[] suffix = Arrays.copyOfRange(refs, position + 1, refs.length);
                return definitions.get(prefix).selectSchema(suffix);
            }
        }

        return Optional.empty();
    }

    private static String unescapeJsonSchema(String pointer) {
        // Unescape "~" special cases.
        String result = pointer.replace("~1", "/").replace("~0", "~");
        // Normalize pointer references to the root document.
        switch (result) {
            case "":
            case "#":
            case "#/":
                return "";
            default:
                return result;
        }
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
