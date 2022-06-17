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
import software.amazon.smithy.utils.StringUtils;

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
        if (!builder.includePrelude) {
            shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        } else {
            shapeFilter = builder.shapeFilter;
        }
        // Never serialize synthetic traits.
        traitFilter = builder.traitFilter.and(FunctionalUtils.not(Trait::isSynthetic));
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
        private boolean includePrelude = false;
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
         * Enables or disables including the prelude in the serialized model.
         *
         * <p>By default, the prelude is not included.
         *
         * <p>This should nearly always be left at default, as per the spec the prelude is
         * inherently part of every model, and so any Smithy implementation must build in
         * an understanding of the prelude. Disabling this filter can be useful for those
         * implementations to allow them to build their understanding of it from a JSON
         * version of the prelude.
         *
         * @param includePrelude boolean indicating whether the prelude should be included or not.
         * @return Returns the builder.
         */
        public Builder includePrelude(boolean includePrelude) {
            this.includePrelude = includePrelude;
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

        private ObjectNode.Builder createTypedNode(Shape shape) {
            return Node.objectNodeBuilder().withMember("type", Node.from(shape.getType().toString()));
        }

        private ObjectNode.Builder withTraits(Shape shape, ObjectNode.Builder shapeBuilder) {
            if (shape.getAllTraits().isEmpty()) {
                return shapeBuilder;
            }

            ObjectNode.Builder traitBuilder = Node.objectNodeBuilder();
            shape.getAllTraits().values().stream()
                    .filter(traitFilter)
                    .sorted(Comparator.comparing(Trait::toShapeId))
                    .forEach(trait -> traitBuilder.withMember(trait.toShapeId().toString(), trait.toNode()));

            return shapeBuilder.withMember("traits", traitBuilder.build());
        }

        @Override
        protected ObjectNode getDefault(Shape shape) {
            return withTraits(shape, createTypedNode(shape)).build();
        }

        @Override
        public Node listShape(ListShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("member", shape.getMember().accept(this)))
                    .build();
        }

        @Override
        public Node mapShape(MapShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("key", shape.getKey().accept(this))
                    .withMember("value", shape.getValue().accept(this)))
                    .build();
        }

        @Override
        public Node operationShape(OperationShape shape) {
            return withTraits(shape, createTypedNode(shape)
                    .withMember("input", serializeReference(shape.getInputShape()))
                    .withMember("output", serializeReference(shape.getOutputShape()))
                    .withOptionalMember("errors", createOptionalIdList(shape.getErrors())))
                    .build();
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
                    .withOptionalMember("resources", createOptionalIdList(shape.getResources())))
                    .build();
        }

        @Override
        public Node serviceShape(ServiceShape shape) {
            ObjectNode.Builder serviceBuilder = withTraits(shape, createTypedNode(shape));

            if (!StringUtils.isBlank(shape.getVersion())) {
                serviceBuilder.withMember("version", Node.from(shape.getVersion()));
            }

            serviceBuilder.withOptionalMember("operations", createOptionalIdList(shape.getOperations()));
            serviceBuilder.withOptionalMember("resources", createOptionalIdList(shape.getResources()));
            serviceBuilder.withOptionalMember("errors", createOptionalIdList(shape.getErrors()));

            if (!shape.getRename().isEmpty()) {
                ObjectNode.Builder renameBuilder = Node.objectNodeBuilder();
                for (Map.Entry<ShapeId, String> entry : shape.getRename().entrySet()) {
                    renameBuilder.withMember(entry.getKey().toString(), entry.getValue());
                }
                serviceBuilder.withMember("rename", renameBuilder.build());
            }

            return serviceBuilder.build();
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
            ObjectNode.Builder builder = createTypedNode(shape);

            ObjectNode.Builder memberBuilder = ObjectNode.objectNodeBuilder();
            for (MemberShape member : members.values()) {
                Node memberValue = member.accept(this);
                memberBuilder.withMember(member.getMemberName(), memberValue);
            }

            builder.withMember("members", memberBuilder.build());
            withTraits(shape, builder);

            return builder.build();
        }

        @Override
        public Node memberShape(MemberShape shape) {
            ObjectNode.Builder builder = serializeReference(shape.getTarget()).toBuilder();
            withTraits(shape, builder);
            return builder.build();
        }

        private ObjectNode serializeReference(ShapeId id) {
            return Node.objectNode().withMember("target", id.toString());
        }
    }
}
