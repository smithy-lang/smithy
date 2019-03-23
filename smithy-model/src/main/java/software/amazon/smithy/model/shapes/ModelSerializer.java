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

package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Serializes a {@link Model} to an {@link ObjectNode}.
 *
 * <p>The serialized value sorts all map key-value pairs so that they contain
 * a consistent key ordering, reducing noise in diffs based on
 * serialized model.
 *
 * <p>After serializing to an ObjectNode, the node can then be serialized
 * to formats like JSON, YAML, Ion, etc.
 */
public final class ModelSerializer {
    private static final String SMITHY_VERSION = "1.0";

    private final Predicate<String> metadataFilter;
    private final Predicate<TraitDefinition> traitDefinitionFilter;
    private final Predicate<Shape> shapeFilter;
    private final Predicate<Trait> traitFilter;
    private final ShapeSerializer shapeSerializer = new ShapeSerializer();

    private ModelSerializer(Builder builder) {
        metadataFilter = builder.metadataFilter;
        traitDefinitionFilter = builder.traitDefinitionFilter
                .and(def -> !Prelude.isPreludeTraitDefinition(def.getFullyQualifiedName()));
        shapeFilter = builder.shapeFilter.and(Predicate.not(Prelude::isPreludeShape));
        traitFilter = builder.traitFilter;
    }

    public ObjectNode serialize(Model model) {
        return Node.objectNode()
                .withMember("smithy", Node.from(SMITHY_VERSION))
                .withOptionalMember("metadata", createMetadata(model).map(Node::withDeepSortedKeys))
                .merge(createNamespaces(model).entrySet().stream()
                        .collect(ObjectNode.collectStringKeys(
                                Map.Entry::getKey,
                                entry -> createNamespaceNode(entry.getValue()))));
    }

    private Optional<Node> createMetadata(Model model) {
        // Grab metadata, filter by key using the predicate.
        Map<StringNode, Node> metadata = model.getMetadata().entrySet().stream()
                .filter(entry -> metadataFilter.test(entry.getKey()))
                .collect(Collectors.toMap(entry -> Node.from(entry.getKey()), Map.Entry::getValue));
        return metadata.isEmpty() ? Optional.empty() : Optional.of(new ObjectNode(metadata, SourceLocation.NONE));
    }

    private ObjectNode createNamespaceNode(Namespace namespace) {
        var builder = Node.objectNodeBuilder();

        if (!namespace.shapes.isEmpty()) {
            builder.withMember("shapes", namespace.shapes.stream()
                    // Members are serialized inside of other shapes, so filter them out.
                    .filter(Predicate.not(Shape::isMemberShape))
                    .map(shape -> new Pair<>(shape, shape.accept(shapeSerializer)))
                    .sorted(Comparator.comparing(pair -> pair.getLeft().getId().getName()))
                    .collect(ObjectNode.collectStringKeys(pair -> pair.getLeft().getId().getName(), Pair::getRight)));
        }

        if (!namespace.traitDefinitions.isEmpty()) {
            builder.withMember("traitDefs", namespace.traitDefinitions.stream()
                    .sorted(Comparator.comparing(TraitDefinition::getName))
                    .collect(ObjectNode.collectStringKeys(TraitDefinition::getName, TraitDefinition::toNode)));
        }

        return builder.build();
    }

    /**
     * Groups shapes and trait definitions into namespaces.
     *
     * @param model Model to compute namespaces from.
     * @return Returns the grouped namespaces.
     */
    private TreeMap<String, Namespace> createNamespaces(Model model) {
        Map<String, List<Shape>> shapes = model.getShapeIndex().shapes()
                .filter(shapeFilter)
                .collect(Collectors.groupingBy(s -> s.getId().getNamespace()));
        Map<String, List<TraitDefinition>> definitions = model.getTraitDefinitions().stream()
                .filter(traitDefinitionFilter)
                .collect(Collectors.groupingBy(TraitDefinition::getNamespace));
        // Sort the namespaces, and group shapes+traits into Namespaces.
        return Stream.concat(shapes.keySet().stream(), definitions.keySet().stream())
                .sorted()
                .collect(Collectors.toMap(
                        Function.identity(),
                        ns -> new Namespace(shapes.get(ns), definitions.get(ns)),
                        (a, b) -> a, // Impossible to encounter duplicate keys.
                        TreeMap::new));
    }

    private static final class Namespace {
        final List<Shape> shapes;
        final List<TraitDefinition> traitDefinitions;

        Namespace(List<Shape> shapes, List<TraitDefinition> traitDefinitions) {
            this.shapes = shapes != null ? shapes : List.of();
            this.traitDefinitions = traitDefinitions != null ? traitDefinitions : List.of();
        }
    }

    /**
     * @return Returns a builder used to create a {@link ModelSerializer}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create {@link ModelSerializer}.
     */
    public static final class Builder implements SmithyBuilder<ModelSerializer> {
        private Predicate<String> metadataFilter = pair -> true;
        private Predicate<TraitDefinition> traitDefinitionFilter = definition -> true;
        private Predicate<Shape> shapeFilter = shape -> true;
        private Predicate<Trait> traitFilter = trait -> true;

        private Builder() {}

        /**
         * Predicate that determines if a metadata is serialized.
         * @param metadataFilter Predicate that accepts a metadata key.
         * @return Returns the builder.
         */
        public Builder metadataFilter(Predicate<String> metadataFilter) {
            this.metadataFilter = Objects.requireNonNull(metadataFilter);
            return this;
        }

        /**
         * Predicate that determines if a trait definition is serialized.
         * @param traitDefinitionFilter Predicate that accepts a traitDef.
         * @return Returns the builder.
         */
        public Builder traitDefinitionFilter(Predicate<TraitDefinition> traitDefinitionFilter) {
            this.traitDefinitionFilter = Objects.requireNonNull(traitDefinitionFilter);
            return this;
        }

        /**
         * Predicate that determines if a shape and its traits are serialized.
         * @param shapeFilter Predicate that accepts a shape.
         * @return Returns the builder.
         */
        public Builder shapeFilter(Predicate<Shape> shapeFilter) {
            this.shapeFilter = Objects.requireNonNull(shapeFilter);
            return this;
        }

        /**
         * Sets a predicate that can be used to filter trait values from
         * appearing in the serialized model.
         *
         * @param traitFilter Predicate that filters out trait definitions.
         * @return Returns the builder.
         */
        public Builder traitFilter(Predicate<Trait> traitFilter) {
            this.traitFilter = traitFilter;
            return this;
        }

        @Override
        public ModelSerializer build() {
            return new ModelSerializer(this);
        }
    }

    private final class ShapeSerializer extends ShapeVisitor.Default<Node> {
        private ObjectNode createTypedNode(Shape shape) {
            return Node.objectNode().withMember("type", Node.from(shape.getType().toString()));
        }

        private ObjectNode withTraits(Shape shape, ObjectNode node) {
            return node.merge(shape.getAllTraits().values().stream()
                    .filter(traitFilter)
                    .sorted(Comparator.comparing(Trait::getName))
                    .collect(ObjectNode.collectStringKeys(Trait::getName, Trait::toNode)));
        }

        @Override
        protected ObjectNode getDefault(Shape shape) {
            return withTraits(shape, createTypedNode(shape));
        }

        @Override
        public Node listShape(ListShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("member", shape.getMember().accept(this)));
        }

        @Override
        public Node setShape(SetShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("member", shape.getMember().accept(this)));
        }

        @Override
        public Node mapShape(MapShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("key", shape.getKey().accept(this))
                    .withMember("value", shape.getValue().accept(this)));
        }

        @Override
        public Node operationShape(OperationShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withOptionalMember("input", shape.getInput().map(id -> Node.from(id.toString())))
                    .withOptionalMember("output", shape.getOutput().map(id -> Node.from(id.toString())))
                    .withOptionalMember("errors", createOptionalIdList(shape.getErrors())));
        }

        @Override
        public Node resourceShape(ResourceShape shape) {
            Optional<Node> identifiers = Optional.empty();
            if (shape.hasIdentifiers()) {
                Stream<Map.Entry<String, ShapeId>> ids = shape.getIdentifiers().entrySet().stream();
                identifiers = Optional.of(ids.collect(ObjectNode.collectStringKeys(
                        Map.Entry::getKey,
                        entry -> Node.from(entry.getValue().toString()))));
            }

            return withTraits(shape, createTypedNode(shape)
                    .withOptionalMember("identifiers", identifiers)
                    .withOptionalMember("create", shape.getCreate().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("read", shape.getRead().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("update", shape.getUpdate().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("delete", shape.getDelete().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("list", shape.getList().map(ShapeId::toString).map(Node::from))
                    .withOptionalMember("operations", createOptionalIdList(shape.getOperations()))
                    .withOptionalMember("resources", createOptionalIdList(shape.getResources())));
        }

        @Override
        public Node serviceShape(ServiceShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("version", Node.from(shape.getVersion()))
                    .withOptionalMember("operations", createOptionalIdList(shape.getOperations()))
                    .withOptionalMember("resources", createOptionalIdList(shape.getResources())));
        }

        private Optional<Node> createOptionalIdList(Collection<ShapeId> list) {
            if (list.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(
                    list.stream().map(ShapeId::toString).sorted().map(Node::from).collect(ArrayNode.collect()));
        }

        @Override
        public Node structureShape(StructureShape shape) {
            return createStructureAndUnion(shape, shape.getAllMembers());
        }

        @Override
        public Node unionShape(UnionShape shape) {
            return createStructureAndUnion(shape, shape.getAllMembers());
        }

        private ObjectNode createStructureAndUnion(Shape shape, Map<String, MemberShape> members) {
            ObjectNode result = createTypedNode(shape);
            result = result.withMember("members", members.entrySet().stream()
                    .map(entry -> new Pair<>(entry.getKey(), entry.getValue().accept(this)))
                    // Sort by key to ensure a consistent member order.
                    .sorted(Comparator.comparing(Pair::getLeft))
                    .collect(ObjectNode.collectStringKeys(Pair::getLeft, Pair::getRight)));
            return withTraits(shape, result);
        }

        @Override
        public Node memberShape(MemberShape shape) {
            String target = shape.getContainer().getNamespace().equals(shape.getTarget().getNamespace())
                    ? shape.getTarget().asRelativeReference()
                    : shape.getTarget().toString();

            return withTraits(shape, Node.objectNode().withMember("target", Node.from(target)));
        }
    }
}
