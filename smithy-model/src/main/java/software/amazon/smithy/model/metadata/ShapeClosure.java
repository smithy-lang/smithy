/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.metadata;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A named closure of shapes.
 *
 * <p>This is defined in the model as an entry in the {@code shapeClosures} list.
 */
public final class ShapeClosure implements ToNode, ToSmithyBuilder<ShapeClosure>, FromSourceLocation {

    /** The metadata key under which a list of shape closures is stored. */
    public static final String METADATA_KEY = "shapeClosures";

    private final String id;
    private final Set<String> includeNamespaces;
    private final String includeBySelector;
    private final Map<ShapeId, String> rename;
    private final String documentation;
    private final SourceLocation sourceLocation;

    private ShapeClosure(Builder builder) {
        id = SmithyBuilder.requiredState("id", builder.id);
        includeNamespaces = builder.includeNamespaces.copy();
        includeBySelector = builder.includeBySelector;
        rename = builder.rename.copy();
        documentation = builder.documentation;
        sourceLocation = builder.sourceLocation;
    }

    /**
     * Creates a builder for a {@code ShapeClosure}.
     *
     * @return the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@code ShapeClosure} from a single closure node.
     *
     * @param node Object node to parse.
     * @return the parsed closure.
     */
    public static ShapeClosure fromNode(Node node) {
        ObjectNode object = node.expectObjectNode();
        Builder builder = builder().sourceLocation(object);
        object.expectStringMember("id", builder::id)
                .getArrayMember("includeNamespaces", StringNode::getValue, builder::includeNamespaces)
                .getStringMember("includeBySelector", builder::includeBySelector)
                .getObjectMember("rename", rename -> builder.rename(renamesFromNode(rename)));
        return builder.build();
    }

    private static Map<ShapeId, String> renamesFromNode(ObjectNode node) {
        Map<ShapeId, String> result = new LinkedHashMap<>(node.size());
        for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
            result.put(ShapeId.from(entry.getKey().getValue()), entry.getValue().expectStringNode().getValue());
        }
        return result;
    }

    /**
     * Extracts every {@code shapeClosures} entry from a model's metadata.
     *
     * @param model Model to read the {@code shapeClosures} metadata from.
     * @return an ordered map of closure id to closure, empty if none are defined.
     */
    public static Map<String, ShapeClosure> fromModel(Model model) {
        return fromModel(model.getMetadata());
    }

    /**
     * Extracts every well-formed {@code shapeClosures} entry from model metadata.
     *
     * <p>Entries that cannot be parsed (for example, a missing id or a malformed
     * rename key) are skipped; those problems are reported separately by model
     * validation.
     *
     * @param metadata Model metadata to read the {@code shapeClosures} entry from.
     * @return an ordered map of closure id to closure, empty if none are defined.
     */
    public static Map<String, ShapeClosure> fromModel(Map<String, Node> metadata) {
        Map<String, ShapeClosure> closures = new LinkedHashMap<>();
        Node closuresNode = metadata.get(METADATA_KEY);
        if (closuresNode != null && closuresNode.isArrayNode()) {
            for (Node element : closuresNode.expectArrayNode().getElements()) {
                try {
                    ShapeClosure closure = fromNode(element);
                    closures.put(closure.getId(), closure);
                } catch (ExpectationNotMetException | ShapeIdSyntaxException e) {
                    // Malformed entries are reported by metadata and idRef validation.
                }
            }
        }
        return MapUtils.orderedCopyOf(closures);
    }

    /**
     * Gets the identifier used to refer to this closure.
     *
     * @return the closure id.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the namespaces whose shapes are included in the closure.
     *
     * @return the included namespaces, or an empty set if none are defined.
     */
    public Set<String> getIncludeNamespaces() {
        return includeNamespaces;
    }

    /**
     * Gets the selector whose matched shapes are included in the closure.
     *
     * @return the selector expression, or empty if none is defined.
     */
    public Optional<String> getIncludeBySelector() {
        return Optional.ofNullable(includeBySelector);
    }

    /**
     * Gets the shape renames applied within the closure.
     *
     * <p>Each key is the id of a shape in the closure, and each value is the
     * name, without a namespace, to use for that shape within the closure.
     *
     * @return a map of shape id to replacement name, empty if none are defined.
     */
    public Map<ShapeId, String> getRename() {
        return rename;
    }

    /**
     * Gets the documentation for the closure.
     *
     * @return the documentation for the closure.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .sourceLocation(sourceLocation)
                .withMember("id", id);
        if (!includeNamespaces.isEmpty()) {
            builder.withMember("includeNamespaces", Node.fromStrings(includeNamespaces));
        }
        builder.withOptionalMember("includeBySelector", getIncludeBySelector().map(Node::from));
        if (!rename.isEmpty()) {
            ObjectNode.Builder renameBuilder = Node.objectNodeBuilder();
            rename.forEach((from, to) -> renameBuilder.withMember(from.toString(), to));
            builder.withMember("rename", renameBuilder.build());
        }
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .id(id)
                .includeNamespaces(includeNamespaces)
                .includeBySelector(includeBySelector)
                .rename(rename);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ShapeClosure)) {
            return false;
        }
        ShapeClosure that = (ShapeClosure) other;
        return id.equals(that.id)
                && includeNamespaces.equals(that.includeNamespaces)
                && Objects.equals(includeBySelector, that.includeBySelector)
                && rename.equals(that.rename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, includeNamespaces, includeBySelector, rename);
    }

    /**
     * Builds a {@link ShapeClosure}.
     */
    public static final class Builder implements SmithyBuilder<ShapeClosure> {
        private String id;
        private final BuilderRef<Set<String>> includeNamespaces = BuilderRef.forOrderedSet();
        private String includeBySelector;
        private final BuilderRef<Map<ShapeId, String>> rename = BuilderRef.forOrderedMap();
        private String documentation;
        private SourceLocation sourceLocation = SourceLocation.NONE;

        private Builder() {}

        @Override
        public ShapeClosure build() {
            return new ShapeClosure(this);
        }

        public Builder id(String id) {
            this.id = Objects.requireNonNull(id);
            return this;
        }

        public Builder includeNamespaces(Collection<String> includeNamespaces) {
            this.includeNamespaces.clear();
            this.includeNamespaces.get().addAll(includeNamespaces);
            return this;
        }

        public Builder includeBySelector(String includeBySelector) {
            this.includeBySelector = includeBySelector;
            return this;
        }

        public Builder rename(Map<ShapeId, String> rename) {
            this.rename.clear();
            this.rename.get().putAll(rename);
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation.getSourceLocation();
            return this;
        }
    }
}
