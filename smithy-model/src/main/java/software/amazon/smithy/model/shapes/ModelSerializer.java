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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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

    private ModelSerializer(Builder builder) {
        metadataFilter = builder.metadataFilter;
        shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        traitFilter = builder.traitFilter;
    }

    public ObjectNode serialize(Model model) {
        ShapeSerializer shapeSerializer = new ShapeSerializer();

        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("smithy", Node.from(Model.MODEL_VERSION))
                .withOptionalMember("metadata", createMetadata(model).map(Node::withDeepSortedKeys));

        // Sort shapes by ID.
        Map<StringNode, Node> shapes = new TreeMap<>();
        for (Shape shape : model.toSet()) {
            // Members are serialized inside of other shapes, so filter them out.
            if (!shape.isMemberShape() && shapeFilter.test(shape)) {
                Node value = shape.accept(shapeSerializer);
                shapes.put(Node.from(shape.getId().toString()), value);
                // Add any necessary apply statements to inherited mixin members that added traits.
                // Apply statements are used here instead of redefining members on structures because
                // apply statements are more resilient to change over time if the shapes targeted by
                // an inherited member changes.
                if (!shapeSerializer.mixinMemberTraits.isEmpty()) {
                    for (MemberShape member : shapeSerializer.mixinMemberTraits) {
                        ObjectNode.Builder applyBuilder = Node.objectNodeBuilder();
                        applyBuilder.withMember("type", "apply");
                        shapes.put(
                            Node.from(member.getId().toString()),
                            serializeTraits(applyBuilder, member.getIntroducedTraits().values()).build()
                        );
                    }
                }
            }
        }

        builder.withMember("shapes", new ObjectNode(shapes, SourceLocation.NONE));

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

    private ObjectNode.Builder serializeTraits(ObjectNode.Builder builder, Collection<Trait> traits) {
        if (!traits.isEmpty()) {
            Map<StringNode, Node> traitsToAdd = new TreeMap<>();
            for (Trait trait : traits) {
                if (traitFilter.test(trait)) {
                    traitsToAdd.put(Node.from(trait.toShapeId().toString()), trait.toNode());
                }
            }
            builder.withMember("traits", new ObjectNode(traitsToAdd, SourceLocation.none()));
        }

        return builder;
    }

    private final class ShapeSerializer extends ShapeVisitor.Default<Node> {

        private final Set<MemberShape> mixinMemberTraits = new TreeSet<>();

        private ObjectNode.Builder createTypedBuilder(Shape shape) {
            return Node.objectNodeBuilder()
                    .withMember("type", Node.from(shape.getType().toString()));
        }

        private ObjectNode.Builder serializeAllTraits(Shape shape, ObjectNode.Builder builder) {
            return serializeTraits(builder, shape.getAllTraits().values());
        }

        @Override
        protected ObjectNode getDefault(Shape shape) {
            return serializeAllTraits(shape, createTypedBuilder(shape)).build();
        }

        @Override
        public Node listShape(ListShape shape) {
            return serializeAllTraits(shape, createTypedBuilder(shape)
                    .withMember("member", shape.getMember().accept(this)))
                    .build();
        }

        @Override
        public Node setShape(SetShape shape) {
            return serializeAllTraits(shape, createTypedBuilder(shape)
                    .withMember("member", shape.getMember().accept(this)))
                    .build();
        }

        @Override
        public Node mapShape(MapShape shape) {
            return serializeAllTraits(shape, createTypedBuilder(shape)
                    .withMember("key", shape.getKey().accept(this))
                    .withMember("value", shape.getValue().accept(this)))
                    .build();
        }

        @Override
        public Node operationShape(OperationShape shape) {
            return serializeAllTraits(shape, createTypedBuilder(shape)
                    .withOptionalMember("input", shape.getInput().map(this::serializeReference))
                    .withOptionalMember("output", shape.getOutput().map(this::serializeReference))
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

            return serializeAllTraits(shape, createTypedBuilder(shape)
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
            ObjectNode.Builder serviceBuilder = createTypedBuilder(shape);

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

            // Serialize traits last, after named structure properties.
            return serializeAllTraits(shape, serviceBuilder).build();
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
            ObjectNode.Builder result = createTypedBuilder(shape);

            if (!shape.getMixins().isEmpty()) {
                List<Node> mixins = new ArrayList<>(shape.getMixins().size());
                for (ShapeId mixin : shape.getMixins()) {
                    mixins.add(serializeReference(mixin));
                }
                result.withMember("mixins", Node.fromNodes(mixins));
            }

            ObjectNode.Builder membersBuilder = ObjectNode.objectNodeBuilder();
            for (MemberShape member : members.values()) {
                if (member.getMixins().isEmpty()) {
                    membersBuilder.withMember(member.getMemberName(), member.accept(this));
                } else if (!member.getIntroducedTraits().isEmpty()) {
                    // There are traits that need to be added to inherited members.
                    mixinMemberTraits.add(member);
                }
            }
            result.withMember("members", membersBuilder.build());

            return serializeTraits(result, shape.getIntroducedTraits().values()).build();
        }

        @Override
        public Node memberShape(MemberShape shape) {
            // Only serialize traits introduced by the member.
            Collection<Trait> introducedTraits = shape.getIntroducedTraits().values();
            return serializeTraits(serializeReferenceBuilder(shape.getTarget()), introducedTraits).build();
        }

        private ObjectNode.Builder serializeReferenceBuilder(ShapeId id) {
            return Node.objectNodeBuilder().withMember("target", id.toString());
        }

        private ObjectNode serializeReference(ShapeId id) {
            return serializeReferenceBuilder(id).build();
        }
    }
}
