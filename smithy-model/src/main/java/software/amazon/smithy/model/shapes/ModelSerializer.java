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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;

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
    private final Predicate<String> metadataFilter;
    private final Predicate<Shape> shapeFilter;
    private final Predicate<Trait> traitFilter;
    private final ShapeSerializer shapeSerializer = new ShapeSerializer();

    private ModelSerializer(Builder builder) {
        metadataFilter = builder.metadataFilter;
        shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        traitFilter = builder.traitFilter;
    }

    public ObjectNode serialize(Model model) {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("smithy", Node.from(Model.MODEL_VERSION))
                .withOptionalMember("metadata", createMetadata(model).map(Node::withDeepSortedKeys));

        ObjectNode.Builder shapesBuilder = Node.objectNodeBuilder();
        model.shapes()
                // Members are serialized inside of other shapes, so filter them out.
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                .filter(shapeFilter)
                .map(shape -> Pair.of(shape, shape.accept(shapeSerializer)))
                .sorted(Comparator.comparing(pair -> pair.getLeft().getId().getName()))
                .forEach(pair -> shapesBuilder.withMember(pair.getLeft().getId().toString(), pair.getRight()));

        builder.withMember("shapes", shapesBuilder.build());
        return builder.build();
    }

    private Optional<Node> createMetadata(Model model) {
        // Grab metadata, filter by key using the predicate.
        Map<StringNode, Node> metadata = model.getMetadata().entrySet().stream()
                .filter(entry -> metadataFilter.test(entry.getKey()))
                .collect(Collectors.toMap(entry -> Node.from(entry.getKey()), Map.Entry::getValue));
        return metadata.isEmpty() ? Optional.empty() : Optional.of(new ObjectNode(metadata, SourceLocation.NONE));
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
        private Predicate<String> metadataFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Shape> shapeFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Trait> traitFilter = FunctionalUtils.alwaysTrue();

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
         * <p>Note that this does not filter out trait definitions. It only filters
         * out instances of traits from being serialized on shapes.
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
            if (shape.getAllTraits().isEmpty()) {
                return node;
            }

            ObjectNode.Builder builder = Node.objectNodeBuilder();
            shape.getAllTraits().values().stream()
                    .filter(traitFilter)
                    .sorted(Comparator.comparing(Trait::toShapeId))
                    .forEach(trait -> builder.withMember(trait.toShapeId().toString(), trait.toNode()));

            return node.withMember("traits", builder.build());
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
                    .withOptionalMember("input", shape.getInput().map(this::serializeReference))
                    .withOptionalMember("output", shape.getOutput().map(this::serializeReference))
                    .withOptionalMember("errors", createOptionalIdList(shape.getErrors())));
        }

        @Override
        public Node resourceShape(ResourceShape shape) {
            Optional<Node> identifiers = Optional.empty();
            if (shape.hasIdentifiers()) {
                Stream<Map.Entry<String, ShapeId>> ids = shape.getIdentifiers().entrySet().stream();
                identifiers = Optional.of(ids.collect(ObjectNode.collectStringKeys(
                        Map.Entry::getKey,
                        entry -> serializeReference(entry.getValue()))));
            }

            return withTraits(shape, createTypedNode(shape)
                    .withOptionalMember("identifiers", identifiers)
                    .withOptionalMember("put", shape.getPut().map(this::serializeReference))
                    .withOptionalMember("create", shape.getCreate().map(this::serializeReference))
                    .withOptionalMember("read", shape.getRead().map(this::serializeReference))
                    .withOptionalMember("update", shape.getUpdate().map(this::serializeReference))
                    .withOptionalMember("delete", shape.getDelete().map(this::serializeReference))
                    .withOptionalMember("list", shape.getList().map(this::serializeReference))
                    .withOptionalMember("operations", createOptionalIdList(shape.getOperations()))
                    .withOptionalMember("collectionOperations", createOptionalIdList(shape.getCollectionOperations()))
                    .withOptionalMember("resources", createOptionalIdList(shape.getResources())));
        }

        @Override
        public Node serviceShape(ServiceShape shape) {
            ObjectNode result = withTraits(shape, createTypedNode(shape)
                    .withMember("version", Node.from(shape.getVersion()))
                    .withOptionalMember("operations", createOptionalIdList(shape.getOperations()))
                    .withOptionalMember("resources", createOptionalIdList(shape.getResources())));

            if (!shape.getRename().isEmpty()) {
                ObjectNode.Builder builder = Node.objectNodeBuilder();
                for (Map.Entry<ShapeId, String> entry : shape.getRename().entrySet()) {
                    builder.withMember(entry.getKey().toString(), entry.getValue());
                }
                result = result.withMember("rename", builder.build());
            }

            return result;
        }

        private Optional<Node> createOptionalIdList(Collection<ShapeId> list) {
            if (list.isEmpty()) {
                return Optional.empty();
            }

            Node result = list.stream()
                    .sorted()
                    .map(this::serializeReference)
                    .collect(ArrayNode.collect());
            return Optional.of(result);
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
                    .map(entry -> Pair.of(entry.getKey(), entry.getValue().accept(this)))
                    .collect(ObjectNode.collectStringKeys(Pair::getLeft, Pair::getRight)));
            return withTraits(shape, result);
        }

        @Override
        public Node memberShape(MemberShape shape) {
            return withTraits(shape, serializeReference(shape.getTarget()));
        }

        private ObjectNode serializeReference(ShapeId id) {
            return Node.objectNode().withMember("target", id.toString());
        }
    }
}
