/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.model;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.smithy.aws.cloudformation.schema.CfnException;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Data class representing a CloudFormation Resource Schema.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#resource-type-schema-syntax">Resource Type Schema</a>
 * @see <a href="https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.jsonL252">Resource Type JSON Schema</a>
 */
public final class ResourceSchema implements ToNode, ToSmithyBuilder<ResourceSchema> {
    private final String typeName;
    private final String description;
    private final String sourceUrl;
    private final String documentationUrl;
    private final Map<String, Schema> definitions = new TreeMap<>();
    private final Map<String, Property> properties = new TreeMap<>();
    private final Set<String> required = new TreeSet<>();
    private final Set<String> readOnlyProperties = new TreeSet<>();
    private final Set<String> writeOnlyProperties = new TreeSet<>();
    private final Set<String> primaryIdentifier = new TreeSet<>();
    private final Set<String> createOnlyProperties = new TreeSet<>();
    private final Set<String> deprecatedProperties = new TreeSet<>();
    private final List<List<String>> additionalIdentifiers;
    // Use a custom comparator to keep the Handler outputs in CRUDL order.
    private final Map<String, Handler> handlers = new TreeMap<>(Comparator.comparing(Handler::getHandlerNameOrder));
    private final Map<String, Remote> remotes = new TreeMap<>();
    private final Tagging tagging;
    private final Schema additionalProperties;

    private ResourceSchema(Builder builder) {
        typeName = SmithyBuilder.requiredState("typeName", builder.typeName);
        description = SmithyBuilder.requiredState("description", builder.description);

        if (builder.properties.isEmpty()) {
            throw new CfnException(format("Expected CloudFormation resource %s to have properties, "
                    + "found none", typeName));
        }
        properties.putAll(builder.properties);

        required.addAll(builder.required);
        sourceUrl = builder.sourceUrl;
        documentationUrl = builder.documentationUrl;
        definitions.putAll(builder.definitions);
        readOnlyProperties.addAll(builder.readOnlyProperties);
        writeOnlyProperties.addAll(builder.writeOnlyProperties);
        primaryIdentifier.addAll(builder.primaryIdentifier);
        createOnlyProperties.addAll(builder.createOnlyProperties);
        deprecatedProperties.addAll(builder.deprecatedProperties);
        additionalIdentifiers = ListUtils.copyOf(builder.additionalIdentifiers);
        handlers.putAll(builder.handlers);
        remotes.putAll(builder.remotes);
        tagging = builder.tagging;
        additionalProperties = builder.additionalProperties;
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        ObjectNode.Builder builder = Node.objectNodeBuilder();

        // This ordering is hand maintained to produce a similar output
        // to those of the resource schemas in CloudFormation documentation,
        // as the NodeMapper does not have a mechanism to order members.
        builder.withMember("typeName", typeName);
        builder.withMember("description", description);

        getSourceUrl().ifPresent(sourceUrl -> builder.withMember("sourceUrl", sourceUrl));
        getDocumentationUrl().ifPresent(documentationUrl -> builder.withMember("documentationUrl", documentationUrl));

        if (!definitions.isEmpty()) {
            builder.withMember("definitions", mapper.serialize(definitions));
        }

        builder.withMember("properties", mapper.serialize(properties));

        if (!required.isEmpty()) {
            builder.withMember("required", mapper.serialize(required));
        }
        if (!readOnlyProperties.isEmpty()) {
            builder.withMember("readOnlyProperties", mapper.serialize(readOnlyProperties));
        }
        if (!writeOnlyProperties.isEmpty()) {
            builder.withMember("writeOnlyProperties", mapper.serialize(writeOnlyProperties));
        }
        if (!createOnlyProperties.isEmpty()) {
            builder.withMember("createOnlyProperties", mapper.serialize(createOnlyProperties));
        }
        if (!deprecatedProperties.isEmpty()) {
            builder.withMember("deprecatedProperties", mapper.serialize(deprecatedProperties));
        }
        if (!primaryIdentifier.isEmpty()) {
            builder.withMember("primaryIdentifier", mapper.serialize(primaryIdentifier));
        }
        if (!additionalIdentifiers.isEmpty()) {
            builder.withMember("additionalIdentifiers", mapper.serialize(additionalIdentifiers));
        }
        if (!handlers.isEmpty()) {
            builder.withMember("handlers", mapper.serialize(handlers));
        }
        if (!remotes.isEmpty()) {
            builder.withMember("remotes", mapper.serialize(remotes));
        }
        if (tagging != null) {
            builder.withMember("tagging", mapper.serialize(tagging));
        }
        if (additionalProperties != null) {
            builder.withMember("additionalProperties", mapper.serialize(additionalProperties));
        }

        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .typeName(typeName)
                .description(description)
                .sourceUrl(sourceUrl)
                .documentationUrl(documentationUrl)
                .definitions(definitions)
                .properties(properties)
                .required(required)
                .readOnlyProperties(readOnlyProperties)
                .writeOnlyProperties(writeOnlyProperties)
                .primaryIdentifier(primaryIdentifier)
                .createOnlyProperties(createOnlyProperties)
                .deprecatedProperties(deprecatedProperties)
                .additionalIdentifiers(additionalIdentifiers)
                .handlers(handlers)
                .remotes(remotes)
                .tagging(tagging);
    }

    public static ResourceSchema fromNode(Node node) {
        NodeMapper mapper = new NodeMapper();
        return mapper.deserializeInto(node, ResourceSchema.builder()).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDescription() {
        return description;
    }

    public Optional<String> getSourceUrl() {
        return Optional.ofNullable(sourceUrl);
    }

    public Optional<String> getDocumentationUrl() {
        return Optional.ofNullable(documentationUrl);
    }

    public Map<String, Schema> getDefinitions() {
        return definitions;
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public Set<String> getRequired() {
        return required;
    }

    public Set<String> getReadOnlyProperties() {
        return readOnlyProperties;
    }

    public Set<String> getWriteOnlyProperties() {
        return writeOnlyProperties;
    }

    public Set<String> getPrimaryIdentifier() {
        return primaryIdentifier;
    }

    public Set<String> getCreateOnlyProperties() {
        return createOnlyProperties;
    }

    public Set<String> getDeprecatedProperties() {
        return deprecatedProperties;
    }

    public List<List<String>> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public Map<String, Handler> getHandlers() {
        return handlers;
    }

    public Map<String, Remote> getRemotes() {
        return remotes;
    }

    public Tagging getTagging() {
        return tagging;
    }

    public Schema getAdditionalProperties() {
        return additionalProperties;
    }

    public static final class Builder implements SmithyBuilder<ResourceSchema> {
        private String typeName;
        private String description;
        private String sourceUrl;
        private String documentationUrl;
        private final Map<String, Schema> definitions = new TreeMap<>();
        private final Map<String, Property> properties = new TreeMap<>();
        private final Set<String> required = new TreeSet<>();
        private final Set<String> readOnlyProperties = new TreeSet<>();
        private final Set<String> writeOnlyProperties = new TreeSet<>();
        private final Set<String> primaryIdentifier = new TreeSet<>();
        private final Set<String> createOnlyProperties = new TreeSet<>();
        private final Set<String> deprecatedProperties = new TreeSet<>();
        private final List<List<String>> additionalIdentifiers = new ArrayList<>();
        private final Map<String, Handler> handlers = new TreeMap<>();
        private final Map<String, Remote> remotes = new TreeMap<>();
        private Tagging tagging;
        private Schema additionalProperties;

        private Builder() {}

        @Override
        public ResourceSchema build() {
            return new ResourceSchema(this);
        }

        public Builder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        public Builder definitions(Map<String, Schema> definitions) {
            this.definitions.clear();
            this.definitions.putAll(definitions);
            return this;
        }

        public Builder addDefinition(String name, Schema definition) {
            this.definitions.put(name, definition);
            return this;
        }

        public Builder removeDefinition(String name) {
            this.definitions.remove(name);
            return this;
        }

        public Builder clearDefinitions() {
            this.definitions.clear();
            return this;
        }

        public Builder properties(Map<String, Property> properties) {
            this.properties.clear();
            this.properties.putAll(properties);
            return this;
        }

        public Builder addProperty(String name, Property property) {
            this.properties.put(name, property);
            return this;
        }

        public Builder removeProperty(String name) {
            this.properties.remove(name);
            return this;
        }

        public Builder clearProperties() {
            this.properties.clear();
            return this;
        }

        public Builder required(Collection<String> required) {
            this.required.clear();
            this.required.addAll(required);
            return this;
        }

        public Builder addRequired(String required) {
            this.required.add(required);
            return this;
        }

        public Builder removeRequired(String required) {
            this.required.remove(required);
            return this;
        }

        public Builder clearRequired() {
            this.required.clear();
            return this;
        }

        public Builder addReadOnlyProperty(String propertyRef) {
            this.readOnlyProperties.add(propertyRef);
            return this;
        }

        public Builder readOnlyProperties(Collection<String> readOnlyProperties) {
            this.readOnlyProperties.clear();
            this.readOnlyProperties.addAll(readOnlyProperties);
            return this;
        }

        public Builder clearReadOnlyProperties() {
            this.readOnlyProperties.clear();
            return this;
        }

        public Builder addWriteOnlyProperty(String propertyRef) {
            this.writeOnlyProperties.add(propertyRef);
            return this;
        }

        public Builder writeOnlyProperties(Collection<String> writeOnlyProperties) {
            this.writeOnlyProperties.clear();
            this.writeOnlyProperties.addAll(writeOnlyProperties);
            return this;
        }

        public Builder clearWriteOnlyProperties() {
            this.writeOnlyProperties.clear();
            return this;
        }

        public Builder primaryIdentifier(Collection<String> primaryIdentifier) {
            this.primaryIdentifier.clear();
            this.primaryIdentifier.addAll(primaryIdentifier);
            return this;
        }

        public Builder clearPrimaryIdentifier() {
            this.primaryIdentifier.clear();
            return this;
        }

        public Builder addCreateOnlyProperty(String propertyRef) {
            this.createOnlyProperties.add(propertyRef);
            return this;
        }

        public Builder createOnlyProperties(Collection<String> createOnlyProperties) {
            this.createOnlyProperties.clear();
            this.createOnlyProperties.addAll(createOnlyProperties);
            return this;
        }

        public Builder clearCreateOnlyProperties() {
            this.createOnlyProperties.clear();
            return this;
        }

        public Builder addDeprecatedProperty(String propertyRef) {
            this.deprecatedProperties.add(propertyRef);
            return this;
        }

        public Builder deprecatedProperties(Collection<String> deprecatedProperties) {
            this.deprecatedProperties.clear();
            this.deprecatedProperties.addAll(deprecatedProperties);
            return this;
        }

        public Builder clearDeprecatedProperties() {
            this.deprecatedProperties.clear();
            return this;
        }

        public Builder addAdditionalIdentifier(List<String> additionalIdentifier) {
            this.additionalIdentifiers.add(additionalIdentifier);
            return this;
        }

        public Builder additionalIdentifiers(List<List<String>> additionalIdentifiers) {
            this.additionalIdentifiers.clear();
            this.additionalIdentifiers.addAll(additionalIdentifiers);
            return this;
        }

        public Builder clearAdditionalIdentifiers() {
            this.additionalIdentifiers.clear();
            return this;
        }

        public Builder handlers(Map<String, Handler> handlers) {
            this.handlers.clear();
            this.handlers.putAll(handlers);
            return this;
        }

        public Builder addHandler(String name, Handler handler) {
            this.handlers.put(name, handler);
            return this;
        }

        public Builder removeHandler(String name) {
            this.handlers.remove(name);
            return this;
        }

        public Builder clearHandlers() {
            this.handlers.clear();
            return this;
        }

        public Builder remotes(Map<String, Remote> remotes) {
            this.remotes.clear();
            this.remotes.putAll(remotes);
            return this;
        }

        public Builder tagging(Tagging tagging) {
            this.tagging = tagging;
            return this;
        }

        public Builder addRemote(String name, Remote remote) {
            this.remotes.put(name, remote);
            return this;
        }

        public Builder removeRemote(String name) {
            this.remotes.remove(name);
            return this;
        }

        public Builder clearRemotes() {
            this.remotes.clear();
            return this;
        }

        public Builder additionalProperties(Schema additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }
    }
}
