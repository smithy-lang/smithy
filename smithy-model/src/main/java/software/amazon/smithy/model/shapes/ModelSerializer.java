/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.SetUtils;
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

    // Explicitly filter out these traits. While some of these are automatically removed in the downgradeToV1
    // model transformation, calling them out here explicitly is a defense in depth. This also has to remove all
    // default traits from the output, whereas the downgradeToV1 transform only removes unnecessary default traits
    // that don't correlate to boxing in V1 models.
    private static final Set<ShapeId> V2_TRAITS_TO_FILTER_FROM_V1 = SetUtils.of(
            DefaultTrait.ID,
            AddedDefaultTrait.ID,
            ClientOptionalTrait.ID,
            PropertyTrait.ID,
            NotPropertyTrait.ID);

    private final Predicate<String> metadataFilter;
    private final Predicate<Shape> shapeFilter;
    private final Predicate<Trait> traitFilter;
    private final String version;

    private ModelSerializer(Builder builder) {
        metadataFilter = builder.metadataFilter;
        version = builder.version;

        if (!builder.includePrelude) {
            shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        } else if (version.equals("1.0")) {
            throw new UnsupportedOperationException("Cannot serialize prelude and set model version to 1.0");
        } else {
            shapeFilter = builder.shapeFilter;
        }

        if (version.equals("1.0")) {
            traitFilter = builder.traitFilter.and(trait -> {
                if (trait.toShapeId().equals(BoxTrait.ID)) {
                    // Include the box trait in 1.0 models.
                    return true;
                } else if (V2_TRAITS_TO_FILTER_FROM_V1.contains(trait.toShapeId())) {
                    // Exclude V2 specific traits.
                    return false;
                } else {
                    return !trait.isSynthetic();
                }
            });
        } else {
            // 2.0 models just need to filter out synthetic traits, including box.
            traitFilter = builder.traitFilter.and(FunctionalUtils.not(Trait::isSynthetic));
        }
    }

    public ObjectNode serialize(Model model) {
        ShapeSerializer shapeSerializer = new ShapeSerializer();

        if (version.equals("1.0")) {
            model = ModelTransformer.create().downgradeToV1(model);
        }

        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("smithy", Node.from(version))
                .withOptionalMember("metadata", createMetadata(model).map(Node::withDeepSortedKeys));

        // Sort shapes by ID.
        Map<StringNode, Node> shapes = new TreeMap<>();
        for (Shape shape : model.toSet()) {
            // Members are serialized inside of other shapes, so filter them out.
            if (!shape.isMemberShape() && shapeFilter.test(shape)) {
                Node value = shape.accept(shapeSerializer);
                shapes.put(Node.from(shape.getId().toString()), value);
                // Add any necessary apply statements to inherited mixin members that added traits, but only if there
                // are actually traits to serialize. Apply statements are used here instead of redefining members on
                // structures because apply statements are more resilient to change over time if the shapes targeted by
                // an inherited member changes.
                if (!shapeSerializer.mixinMemberTraits.isEmpty()) {
                    for (MemberShape member : shapeSerializer.mixinMemberTraits) {
                        Map<StringNode, Node> introducedTraits = createIntroducedTraitsMap(
                                member.getIntroducedTraits().values());
                        if (!introducedTraits.isEmpty()) {
                            ObjectNode.Builder applyBuilder = Node.objectNodeBuilder();
                            applyBuilder.withMember("type", "apply");
                            ObjectNode traits = serializeTraits(applyBuilder, introducedTraits).build();
                            shapes.put(Node.from(member.getId().toString()), traits);
                        }
                    }
                }
            }
        }

        builder.withMember("shapes", new ObjectNode(shapes, SourceLocation.NONE));

        return builder.build();
    }

    private Optional<Node> createMetadata(Model model) {
        // Grab metadata, filter by key using the predicate.
        Map<StringNode, Node> metadata = model.getMetadata()
                .entrySet()
                .stream()
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
        private String version = "2.0";

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

        /**
         * Sets the IDL version to serialize. Defaults to 2.0.
         *
         * <p>Version "1.0" serialization cannot be used with {@link #includePrelude}.
         *
         * @param version IDL version to set. Can be "1", "1.0", "2", or "2.0".
         *                "1" and "2" are normalized to "1.0" and "2.0".
         * @return Returns the builder.
         */
        public Builder version(String version) {
            switch (version) {
                case "2":
                case "2.0":
                    this.version = "2.0";
                    break;
                case "1":
                case "1.0":
                    this.version = "1.0";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Smithy model version: " + version);
            }
            return this;
        }

        @Override
        public ModelSerializer build() {
            return new ModelSerializer(this);
        }
    }

    private ObjectNode.Builder serializeTraits(ObjectNode.Builder builder, Collection<Trait> traits) {
        return serializeTraits(builder, createIntroducedTraitsMap(traits));
    }

    private ObjectNode.Builder serializeTraits(ObjectNode.Builder builder, Map<StringNode, Node> traits) {
        if (!traits.isEmpty()) {
            builder.withMember("traits", new ObjectNode(traits, SourceLocation.none()));
        }

        return builder;
    }

    private Map<StringNode, Node> createIntroducedTraitsMap(Collection<Trait> traits) {
        if (traits.isEmpty()) {
            return Collections.emptyMap();
        } else {
            Map<StringNode, Node> traitsToAdd = new TreeMap<>();
            for (Trait trait : traits) {
                if (traitFilter.test(trait)) {
                    traitsToAdd.put(Node.from(trait.toShapeId().toString()), trait.toNode());
                }
            }
            return traitsToAdd;
        }
    }

    private final class ShapeSerializer extends ShapeVisitor.Default<Node> {

        private final Set<MemberShape> mixinMemberTraits = new TreeSet<>();

        private ObjectNode.Builder createTypedBuilder(Shape shape) {
            ObjectNode.Builder builder = Node.objectNodeBuilder()
                    .withMember("type", Node.from(shape.getType().toString()));

            if (!shape.getMixins().isEmpty()) {
                List<Node> mixins = new ArrayList<>(shape.getMixins().size());
                for (ShapeId mixin : shape.getMixins()) {
                    mixins.add(serializeReference(mixin));
                }
                builder.withMember("mixins", Node.fromNodes(mixins));
            }

            return builder;
        }

        private ObjectNode.Builder serializeAllTraits(Shape shape, ObjectNode.Builder builder) {
            return serializeTraits(builder, shape.getIntroducedTraits().values());
        }

        @Override
        protected ObjectNode getDefault(Shape shape) {
            return serializeAllTraits(shape, createTypedBuilder(shape)).build();
        }

        @Override
        public Node enumShape(EnumShape shape) {
            return createNamedMemberShape(shape, shape.getAllMembers());
        }

        @Override
        public Node intEnumShape(IntEnumShape shape) {
            return createNamedMemberShape(shape, shape.getAllMembers());
        }

        @Override
        public Node listShape(ListShape shape) {
            ObjectNode.Builder result = createTypedBuilder(shape);
            // This visitor covers sets and lists. Sets aren't supported in IDL v2, so convert to list.
            result.withMember("type", ShapeType.LIST.toString());
            mixinMember(result, shape.getMember(), "member");
            return serializeAllTraits(shape, result).build();
        }

        private void mixinMember(ObjectNode.Builder builder, MemberShape member, String key) {
            if (member.getMixins().isEmpty()) {
                builder.withMember(key, member.accept(this));
            } else if (!member.getIntroducedTraits().isEmpty()) {
                mixinMemberTraits.add(member);
            }
        }

        @Override
        public Node mapShape(MapShape shape) {
            ObjectNode.Builder result = createTypedBuilder(shape);
            mixinMember(result, shape.getKey(), "key");
            mixinMember(result, shape.getValue(), "value");
            return serializeAllTraits(shape, result).build();
        }

        @Override
        public Node operationShape(OperationShape shape) {
            return serializeAllTraits(shape,
                    createTypedBuilder(shape)
                            .withMember("input", serializeReference(shape.getInputShape()))
                            .withMember("output", serializeReference(shape.getOutputShape()))
                            .withOptionalMember("errors", createOptionalIdList(shape.getIntroducedErrors())))
                    .build();
        }

        @Override
        public Node resourceShape(ResourceShape shape) {
            Optional<Node> identifiers = Optional.empty();
            Optional<Node> properties = Optional.empty();
            if (shape.hasIdentifiers()) {
                Stream<Map.Entry<String, ShapeId>> ids = shape.getIdentifiers().entrySet().stream();
                identifiers = Optional.of(ids.collect(ObjectNode.collectStringKeys(
                        Map.Entry::getKey,
                        entry -> serializeReference(entry.getValue()))));
            }
            if (shape.hasProperties()) {
                Stream<Map.Entry<String, ShapeId>> props = shape.getProperties().entrySet().stream();
                properties = Optional.of(props.collect(ObjectNode.collectStringKeys(
                        Map.Entry::getKey,
                        entry -> serializeReference(entry.getValue()))));
            }

            return serializeAllTraits(shape,
                    createTypedBuilder(shape)
                            .withOptionalMember("identifiers", identifiers)
                            .withOptionalMember("properties", properties)
                            .withOptionalMember("put", shape.getPut().map(this::serializeReference))
                            .withOptionalMember("create", shape.getCreate().map(this::serializeReference))
                            .withOptionalMember("read", shape.getRead().map(this::serializeReference))
                            .withOptionalMember("update", shape.getUpdate().map(this::serializeReference))
                            .withOptionalMember("delete", shape.getDelete().map(this::serializeReference))
                            .withOptionalMember("list", shape.getList().map(this::serializeReference))
                            .withOptionalMember("operations", createOptionalIdList(shape.getIntroducedOperations()))
                            .withOptionalMember("collectionOperations",
                                    createOptionalIdList(shape.getCollectionOperations()))
                            .withOptionalMember("resources", createOptionalIdList(shape.getIntroducedResources())))
                    .build();
        }

        @Override
        public Node serviceShape(ServiceShape shape) {
            ObjectNode.Builder serviceBuilder = createTypedBuilder(shape);

            if (!StringUtils.isBlank(shape.getIntroducedVersion())) {
                serviceBuilder.withMember("version", Node.from(shape.getIntroducedVersion()));
            }

            serviceBuilder.withOptionalMember("operations", createOptionalIdList(shape.getIntroducedOperations()));
            serviceBuilder.withOptionalMember("resources", createOptionalIdList(shape.getIntroducedResources()));
            serviceBuilder.withOptionalMember("errors", createOptionalIdList(shape.getIntroducedErrors()));

            if (!shape.getIntroducedRename().isEmpty()) {
                ObjectNode.Builder renameBuilder = Node.objectNodeBuilder();
                for (Map.Entry<ShapeId, String> entry : shape.getIntroducedRename().entrySet()) {
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
            return createNamedMemberShape(shape, shape.getAllMembers());
        }

        @Override
        public Node unionShape(UnionShape shape) {
            return createNamedMemberShape(shape, shape.getAllMembers());
        }

        private ObjectNode createNamedMemberShape(Shape shape, Map<String, MemberShape> members) {
            ObjectNode.Builder result = createTypedBuilder(shape);

            ObjectNode.Builder membersBuilder = ObjectNode.objectNodeBuilder();
            for (MemberShape member : members.values()) {
                mixinMember(membersBuilder, member, member.getMemberName());
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
