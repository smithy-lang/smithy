/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.AbstractCodeWriter;
import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.StringUtils;

/**
 * Serializes a {@link Model} into a set of Smithy IDL files.
 */
public final class SmithyIdlModelSerializer {
    private static final String DEFAULT_INLINE_INPUT_SUFFIX = "Input";
    private static final String DEFAULT_INLINE_OUTPUT_SUFFIX = "Output";

    private final Predicate<String> metadataFilter;
    private final Predicate<Shape> shapeFilter;
    private final Predicate<Trait> traitFilter;
    private final Function<Shape, Path> shapePlacer;
    private final Path basePath;
    private final SmithyIdlComponentOrder componentOrder;
    private final String inlineInputSuffix;
    private final String inlineOutputSuffix;

    /**
     * Trait serialization features.
     */
    private enum TraitFeature {
        /** Inline documentation traits with other traits as opposed to using /// syntax. */
        NO_SPECIAL_DOCS_SYNTAX,

        /** Serializing a member, so special default syntax can be used. */
        MEMBER;

        /**
         * Checks if the current enum value is present in an array of enum values.
         *
         * @param haystack Array of enums to check.
         * @return Returns true if this enum is found in the array.
         */
        boolean hasFeature(TraitFeature[] haystack) {
            for (TraitFeature test : haystack) {
                if (test == this) {
                    return true;
                }
            }
            return false;
        }
    }

    private SmithyIdlModelSerializer(Builder builder) {
        metadataFilter = builder.metadataFilter;
        // If prelude serializing has been enabled, only use the given shape filter.
        if (builder.serializePrelude) {
            shapeFilter = builder.shapeFilter;
        // Default to using the given shape filter and filtering prelude shapes.
        } else {
            shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        }
        // Never serialize synthetic traits.
        traitFilter = builder.traitFilter.and(FunctionalUtils.not(Trait::isSynthetic));
        basePath = builder.basePath;
        if (basePath != null) {
            Function<Shape, Path> shapePlacer = builder.shapePlacer;
            this.shapePlacer = shape -> this.basePath.resolve(shapePlacer.apply(shape));
        } else {
            this.shapePlacer = builder.shapePlacer;
        }
        this.componentOrder = builder.componentOrder;
        this.inlineInputSuffix = builder.inlineInputSuffix;
        this.inlineOutputSuffix = builder.inlineOutputSuffix;
    }

    /**
     * Serializes a {@link Model} into a set of Smithy IDL files.
     *
     * <p>The output is a mapping
     *
     * <p>By default the paths are relative paths where each namespace is given its own file in the form
     * "namespace.smithy". This is configurable via the shape placer, which can place shapes into absolute
     * paths.
     *
     * <p>If the model contains no shapes, or all shapes are filtered out, then a single path "metadata.smithy"
     * will be present. This will contain only any defined metadata.
     *
     * @param model The model to serialize.
     * @return A map of (possibly relative) file paths to Smithy IDL strings.
     */
    public Map<Path, String> serialize(Model model) {
        Map<Path, String> result = model.shapes()
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                .filter(shapeFilter)
                .collect(Collectors.groupingBy(shapePlacer)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> serialize(model, entry.getValue())));
        if (result.isEmpty()) {
            Path path = Paths.get("metadata.smithy");
            if (basePath != null) {
                path = basePath.resolve(path);
            }
            return Collections.singletonMap(path, serializeHeader(model, null));
        }
        return result;
    }

    private String serialize(Model fullModel, Collection<Shape> shapes) {
        Set<String> namespaces = shapes.stream()
                .map(shape -> shape.getId().getNamespace())
                .collect(Collectors.toSet());
        if (namespaces.size() != 1) {
            throw new RuntimeException("All shapes in a single file must share a namespace.");
        }

        // There should only be one namespace at this point, so grab it.
        String namespace = namespaces.iterator().next();
        SmithyCodeWriter codeWriter = new SmithyCodeWriter(namespace, fullModel);
        NodeSerializer nodeSerializer = new NodeSerializer(codeWriter, fullModel);

        Set<ShapeId> inlineableShapes = getInlineableShapes(fullModel, shapes);
        ShapeSerializer shapeSerializer = new ShapeSerializer(
                codeWriter, nodeSerializer, traitFilter, fullModel, inlineableShapes, componentOrder);
        Comparator<Shape> comparator = componentOrder.shapeComparator();
        shapes.stream()
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                .filter(shape -> !inlineableShapes.contains(shape.getId()))
                .sorted(comparator)
                .forEach(shape -> shape.accept(shapeSerializer));

        return serializeHeader(fullModel, namespace) + codeWriter.toString();
    }

    private Set<ShapeId> getInlineableShapes(Model fullModel, Collection<Shape> shapes) {
        Set<ShapeId> inlineableShapes = new HashSet<>();
        for (Shape shape : shapes) {
            if (!shape.isOperationShape()) {
                continue;
            }
            OperationShape operation = shape.asOperationShape().get();
            if (!operation.getInputShape().equals(UnitTypeTrait.UNIT)) {
                Shape inputShape = fullModel.expectShape(operation.getInputShape());
                if (shapes.contains(inputShape) && inputShape.hasTrait(InputTrait.ID)
                        && inputShape.getId().getName().equals(operation.getId().getName() + inlineInputSuffix)) {
                    inlineableShapes.add(operation.getInputShape());
                }
            }
            if (!operation.getOutputShape().equals(UnitTypeTrait.UNIT)) {
                Shape outputShape = fullModel.expectShape(operation.getOutputShape());
                if (shapes.contains(outputShape) && outputShape.hasTrait(OutputTrait.ID)
                        && outputShape.getId().getName().equals(operation.getId().getName() + inlineOutputSuffix)) {
                    inlineableShapes.add(operation.getOutputShape());
                }
            }
        }
        return inlineableShapes;
    }

    private String serializeHeader(Model fullModel, String namespace) {
        SmithyCodeWriter codeWriter = new SmithyCodeWriter(null, fullModel);
        NodeSerializer nodeSerializer = new NodeSerializer(codeWriter, fullModel);

        codeWriter.write("$$version: \"$L\"", Model.MODEL_VERSION);

        if (!inlineInputSuffix.equals(DEFAULT_INLINE_INPUT_SUFFIX)) {
            codeWriter.write("$$operationInputSuffix: $S", inlineInputSuffix);
        }

        if (!inlineOutputSuffix.equals(DEFAULT_INLINE_OUTPUT_SUFFIX)) {
            codeWriter.write("$$operationOutputSuffix: $S", inlineOutputSuffix);
        }

        codeWriter.write("");

        Comparator<Map.Entry<String, Node>> comparator = componentOrder.metadataComparator();

        // Write the full metadata into every output. When loaded back together the conflicts will be ignored,
        // but if they're separated out then each file will still have all the context.
        fullModel.getMetadata().entrySet().stream()
                .filter(entry -> metadataFilter.test(entry.getKey()))
                .sorted(comparator)
                .forEach(entry -> {
                    codeWriter.trimTrailingSpaces(false)
                            .writeInline("metadata $K = ", entry.getKey())
                            .trimTrailingSpaces();
                    nodeSerializer.serialize(entry.getValue());
                    codeWriter.write("");
                });

        if (!fullModel.getMetadata().isEmpty()) {
            codeWriter.write("");
        }

        if (namespace != null) {
            codeWriter.write("namespace $L", namespace)
                    .write("")
                    // We want the extra blank line to separate the header and the model contents.
                    .trimBlankLines(-1);
        }

        return codeWriter.toString();
    }

    /**
     * @return Returns a builder used to create a {@link SmithyIdlModelSerializer}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Sorts shapes into files based on their namespace, where each file is named {namespace}.smithy.
     *
     * @param shape The shape to assign a file to.
     * @return Returns the file the given shape should be placed in.
     */
    public static Path placeShapesByNamespace(Shape shape) {
        return Paths.get(shape.getId().getNamespace() + ".smithy");
    }

    /**
     * Builder used to create {@link SmithyIdlModelSerializer}.
     */
    public static final class Builder implements SmithyBuilder<SmithyIdlModelSerializer> {
        private Predicate<String> metadataFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Shape> shapeFilter = FunctionalUtils.alwaysTrue();
        private Predicate<Trait> traitFilter = FunctionalUtils.alwaysTrue();
        private Function<Shape, Path> shapePlacer = SmithyIdlModelSerializer::placeShapesByNamespace;
        private Path basePath = null;
        private boolean serializePrelude = false;
        private SmithyIdlComponentOrder componentOrder = SmithyIdlComponentOrder.PREFERRED;
        private String inlineInputSuffix = DEFAULT_INLINE_INPUT_SUFFIX;
        private String inlineOutputSuffix = DEFAULT_INLINE_OUTPUT_SUFFIX;

        public Builder() {}

        /**
         * Predicate that determines if a metadata is serialized.
         *
         * @param metadataFilter Predicate that accepts a metadata key.
         * @return Returns the builder.
         */
        public Builder metadataFilter(Predicate<String> metadataFilter) {
            this.metadataFilter = Objects.requireNonNull(metadataFilter);
            return this;
        }

        /**
         * Predicate that determines if a shape and its traits are serialized.
         *
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

        /**
         * Function that determines what output file a shape should go in.
         *
         * <p>The returned paths may be absolute or relative.
         *
         * <p>NOTE: the Smithy IDL only supports one namespace per file.
         *
         * @param shapePlacer Function that accepts a shape and returns file path.
         * @return Returns the builder.
         */
        public Builder shapePlacer(Function<Shape, Path> shapePlacer) {
            this.shapePlacer = Objects.requireNonNull(shapePlacer);
            return this;
        }

        /**
         * A base path to use for any created models.
         *
         * @param basePath The base directory to assign models to.
         * @return Returns the builder.
         */
        public Builder basePath(Path basePath) {
            this.basePath = basePath;
            return this;
        }

        /**
         * Enables serializing shapes in the Smithy prelude.
         * Defaults to false.
         * @return Returns the builder.
         */
        public Builder serializePrelude() {
            this.serializePrelude = true;
            return this;
        }

        /**
         * Defines how components are sorted in the model, changing the default behavior of sorting alphabetically.
         *
         * <p>You can serialize metadata, shapes, and traits in the original order they were defined by setting
         * this to {@link SmithyIdlComponentOrder#SOURCE_LOCATION}.
         *
         * @param componentOrder Change how components are sorted.
         * @return Returns the builder.
         */
        public Builder componentOrder(SmithyIdlComponentOrder componentOrder) {
            this.componentOrder = Objects.requireNonNull(componentOrder);
            return this;
        }

        /**
         * Defines what suffixes are checked on operation input shapes to determine whether
         * inline syntax should be used.
         *
         * <p>This will also set the "operationInputSuffix" control statement.
         *
         * @param inlineInputSuffix The suffix to use for inline operation input.
         * @return Returns the builder.
         */
        public Builder inlineInputSuffix(String inlineInputSuffix) {
            this.inlineInputSuffix = inlineInputSuffix;
            return this;
        }

        /**
         * Defines what suffixes are checked on operation output shapes to determine whether
         * inline syntax should be used.
         *
         * <p>This will also set the "operationOutputSuffix" control statement.
         *
         * @param inlineOutputSuffix The suffix to use for inline operation output.
         * @return Returns the builder.
         */
        public Builder inlineOutputSuffix(String inlineOutputSuffix) {
            this.inlineOutputSuffix = inlineOutputSuffix;
            return this;
        }

        @Override
        public SmithyIdlModelSerializer build() {
            return new SmithyIdlModelSerializer(this);
        }
    }

    /**
     * Serializes shapes in the IDL format.
     */
    private static final class ShapeSerializer extends ShapeVisitor.Default<Void> {
        private final SmithyCodeWriter codeWriter;
        private final NodeSerializer nodeSerializer;
        private final Predicate<Trait> traitFilter;
        private final Model model;
        private final Set<ShapeId> inlineableShapes;
        private final SmithyIdlComponentOrder componentOrder;

        ShapeSerializer(
                SmithyCodeWriter codeWriter,
                NodeSerializer nodeSerializer,
                Predicate<Trait> traitFilter,
                Model model,
                Set<ShapeId> inlineableShapes,
                SmithyIdlComponentOrder componentOrder
        ) {
            this.codeWriter = codeWriter;
            this.nodeSerializer = nodeSerializer;
            this.traitFilter = traitFilter;
            this.model = model;
            this.inlineableShapes = inlineableShapes;
            this.componentOrder = componentOrder;
        }

        @Override
        protected Void getDefault(Shape shape) {
            serializeTraits(shape);
            codeWriter.writeInline("$L $L ", shape.getType(), shape.getId().getName());
            writeMixins(shape);
            codeWriter.write("").write("");
            return null;
        }

        private void shapeWithMembers(Shape shape, Collection<MemberShape> members) {
            shapeWithMembers(shape, members, false);
        }

        private void shapeWithMembers(Shape shape, Collection<MemberShape> members, boolean isEnum) {
            List<MemberShape> nonMixinMembers = new ArrayList<>();
            List<MemberShape> mixinMembers = new ArrayList<>();
            for (MemberShape member : members) {
                if (member.getMixins().isEmpty()) {
                    nonMixinMembers.add(member);
                } else if (!member.getIntroducedTraits().isEmpty()) {
                    mixinMembers.add(member);
                }
            }

            serializeTraits(shape);
            // IDL V2 does not support sets, so convert set to list when serializing.
            String v2Type = shape.getType() == ShapeType.SET ? ShapeType.LIST.toString() : shape.getType().toString();
            codeWriter.writeInline("$L $L ", v2Type, shape.getId().getName());

            writeMixins(shape);
            if (isEnum) {
                writeEnumMembers(nonMixinMembers);
            } else {
                writeShapeMembers(nonMixinMembers);
            }
            codeWriter.write("");
            applyIntroducedTraits(mixinMembers);
        }

        private void writeMixins(Shape shape) {
            if (shape.getMixins().size() == 1) {
                codeWriter.writeInline("with [$I] ", shape.getMixins().iterator().next());
            } else if (shape.getMixins().size() > 1) {
                codeWriter.write("with [").indent();
                for (ShapeId id : shape.getMixins()) {
                    // Trailing spaces are trimmed.
                    codeWriter.write("$I", id);
                }
                codeWriter.dedent().writeInline("] ");
            }
        }

        private void writeShapeMembers(Collection<MemberShape> members) {
            if (members.isEmpty()) {
                // When the are no members to write, put "{}" on the same line.
                codeWriter.writeInline("{}").write("");
            } else {
                codeWriter.openBlock("{", "}", () -> {
                    for (MemberShape member : members) {
                        serializeTraits(member.getAllTraits(), TraitFeature.MEMBER);
                        String assignment = "";
                        if (member.hasTrait(DefaultTrait.class)) {
                            assignment = " = " + Node.printJson(member.expectTrait(DefaultTrait.class).toNode());
                        }
                        codeWriter.write("$L: $I$L", member.getMemberName(), member.getTarget(), assignment);
                    }
                });
            }
        }

        private void writeEnumMembers(Collection<MemberShape> members) {
            if (members.isEmpty()) {
                codeWriter.writeInline("{}").write("");
                return;
            }

            codeWriter.openBlock("{", "}", () -> {
                for (MemberShape member : members) {
                    Map<ShapeId, Trait> traits = new LinkedHashMap<>(member.getAllTraits());
                    Optional<String> stringValue = member.expectTrait(EnumValueTrait.class).getStringValue();
                    boolean hasNormalName = stringValue.isPresent() && member.getMemberName().equals(stringValue.get());
                    String assignment = "";
                    if (!hasNormalName) {
                        assignment = " = " + Node.printJson(member.expectTrait(EnumValueTrait.class).toNode());
                    }
                    traits.remove(EnumValueTrait.ID);
                    serializeTraits(traits);
                    codeWriter.write("$L$L", member.getMemberName(), assignment);
                }
            });
        }

        private void applyIntroducedTraits(Collection<MemberShape> members) {
            for (MemberShape member : members) {
                Map<ShapeId, Trait> introducedTraits = new LinkedHashMap<>(member.getIntroducedTraits());

                // The @enumValue trait is serialized using the `=` IDL syntax, so remove it here.
                introducedTraits.remove(EnumValueTrait.ID);

                // Use short form for a single trait, and block form for multiple traits.
                if (introducedTraits.size() == 1) {
                    codeWriter.writeInline("apply $I ", member.getId());
                    serializeTraits(member.getIntroducedTraits(), TraitFeature.NO_SPECIAL_DOCS_SYNTAX);
                    codeWriter.write("");
                } else if (!introducedTraits.isEmpty()) {
                    codeWriter.openBlock("apply $I {", "}", member.getId(), () -> {
                        // Only serialize local traits, and don't use special documentation syntax here.
                        serializeTraits(member.getIntroducedTraits(), TraitFeature.NO_SPECIAL_DOCS_SYNTAX);
                    }).write("");
                }
            }
        }

        private void serializeTraits(Shape shape) {
            serializeTraits(shape.getIntroducedTraits());
        }

        private void serializeTraits(Map<ShapeId, Trait> traits, TraitFeature... traitFeatures) {
            boolean noSpecialDocsSyntax = TraitFeature.NO_SPECIAL_DOCS_SYNTAX.hasFeature(traitFeatures);
            boolean isMember = TraitFeature.MEMBER.hasFeature(traitFeatures);

            // The documentation trait always needs to be serialized first since it uses special syntax.
            if (!noSpecialDocsSyntax && traits.containsKey(DocumentationTrait.ID)) {
                Trait documentation = traits.get(DocumentationTrait.ID);
                if (traitFilter.test(documentation)) {
                    serializeDocumentation(documentation.toNode().expectStringNode().getValue());
                }
            }

            Comparator<Trait> traitComparator = componentOrder.toShapeIdComparator();

            traits.values().stream()
                    .filter(trait -> noSpecialDocsSyntax || !(trait instanceof DocumentationTrait))
                    // The default and enumValue traits are serialized using the assignment syntactic sugar.
                    .filter(trait -> {
                        if (trait instanceof EnumValueTrait) {
                            return false;
                        } else {
                            // Default traits are serialized normally for non-members, but omitted for members.
                            return !isMember || !(trait instanceof DefaultTrait);
                        }
                    })
                    .filter(traitFilter)
                    .sorted(traitComparator)
                    .forEach(this::serializeTrait);
        }

        private void serializeDocumentation(String documentation) {
            // The documentation trait has a special syntax, which we always want to use.
            codeWriter
                    .pushState()
                    // See https://github.com/smithy-lang/smithy/issues/2115
                    .trimTrailingSpaces(false)
                    .setNewlinePrefix("/// ")
                    .write(documentation.replace("$", "$$"))
                    .popState();
        }

        private void serializeTrait(Trait trait) {
            Node node = trait.toNode();
            // We won't fail if the trait can't be found.
            Shape shape = model.getShape(trait.toShapeId()).orElse(null);

            if (shape != null && (trait instanceof AnnotationTrait || isEmptyStructure(node, shape))) {
                // Traits that inherit from AnnotationTrait specifically can omit a value.
                // Traits that are simply boolean shapes which don't implement AnnotationTrait cannot.
                // Additionally, empty structure traits can omit a value.
                codeWriter.write("@$I", trait.toShapeId());
            } else if (node.isObjectNode()) {
                codeWriter.writeIndent().openBlockInline("@$I(", trait.toShapeId());
                nodeSerializer.serializeKeyValuePairs(node.expectObjectNode(), shape);
                codeWriter.closeBlock(")");
            } else {
                codeWriter.writeIndent().writeInline("@$I(", trait.toShapeId());
                nodeSerializer.serialize(node, shape);
                codeWriter.write(")");
            }
        }

        private boolean isEmptyStructure(Node node, Shape shape) {
            return !shape.isDocumentShape() && node.asObjectNode().map(ObjectNode::isEmpty).orElse(false);
        }

        @Override
        public Void enumShape(EnumShape shape) {
            shapeWithMembers(shape, shape.members(), true);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            shapeWithMembers(shape, shape.members(), true);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            shapeWithMembers(shape, Collections.singletonList(shape.getMember()));
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            shapeWithMembers(shape, ListUtils.of(shape.getKey(), shape.getValue()));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            shapeWithMembers(shape, shape.members());
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            shapeWithMembers(shape, shape.members());
            return null;
        }

        @Override
        public Void serviceShape(ServiceShape shape) {
            serializeTraits(shape);
            codeWriter.writeInline("service $L ", shape.getId().getName());
            writeMixins(shape);
            codeWriter.openBlock("{");

            if (!StringUtils.isBlank(shape.getIntroducedVersion())) {
                codeWriter.write("version: $S", shape.getIntroducedVersion());
            }

            codeWriter.writeOptionalIdList("operations", shape.getIntroducedOperations());
            codeWriter.writeOptionalIdList("resources", shape.getIntroducedResources());
            codeWriter.writeOptionalIdList("errors", shape.getIntroducedErrors());
            if (!shape.getIntroducedRename().isEmpty()) {
                codeWriter.openBlock("rename: {", "}", () -> {
                    for (Map.Entry<ShapeId, String> entry : shape.getIntroducedRename().entrySet()) {
                        codeWriter.write("$S: $S", entry.getKey(), entry.getValue());
                    }
                });
            }
            codeWriter.closeBlock("}").write("");
            return null;
        }

        @Override
        public Void resourceShape(ResourceShape shape) {
            serializeTraits(shape);
            codeWriter.writeInline("resource $L ", shape.getId().getName());
            writeMixins(shape);
            codeWriter.openBlock("{");
            if (!shape.getIdentifiers().isEmpty()) {
                codeWriter.openBlock("identifiers: {");
                shape.getIdentifiers().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> codeWriter.write(
                                "$L: $I", entry.getKey(), entry.getValue()));
                codeWriter.closeBlock("}");
            }

            shape.getPut().ifPresent(shapeId -> codeWriter.write("put: $I", shapeId));
            shape.getCreate().ifPresent(shapeId -> codeWriter.write("create: $I", shapeId));
            shape.getRead().ifPresent(shapeId -> codeWriter.write("read: $I", shapeId));
            shape.getUpdate().ifPresent(shapeId -> codeWriter.write("update: $I", shapeId));
            shape.getDelete().ifPresent(shapeId -> codeWriter.write("delete: $I", shapeId));
            shape.getList().ifPresent(shapeId -> codeWriter.write("list: $I", shapeId));
            codeWriter.writeOptionalIdList("operations", shape.getIntroducedOperations());
            codeWriter.writeOptionalIdList("collectionOperations", shape.getCollectionOperations());
            codeWriter.writeOptionalIdList("resources", shape.getIntroducedResources());
            if (shape.hasProperties()) {
              codeWriter.openBlock("properties: {");
              shape.getProperties().forEach((name, shapeId) -> codeWriter.write("$L: $I", name, shapeId));
              codeWriter.closeBlock("}");
            }
            codeWriter.closeBlock("}");
            codeWriter.write("");
            return null;
        }

        @Override
        public Void operationShape(OperationShape shape) {
            serializeTraits(shape);
            codeWriter.writeInline("operation $L ", shape.getId().getName());
            writeMixins(shape);
            codeWriter.openBlock("{");
            List<MemberShape> mixinMembers = new ArrayList<>();
            mixinMembers.addAll(writeInlineableProperty("input", shape.getInputShape(), InputTrait.ID));
            mixinMembers.addAll(writeInlineableProperty("output", shape.getOutputShape(), OutputTrait.ID));
            codeWriter.writeOptionalIdList("errors", shape.getIntroducedErrors());
            codeWriter.closeBlock("}");
            codeWriter.write("");
            applyIntroducedTraits(mixinMembers);
            return null;
        }

        private Collection<MemberShape> writeInlineableProperty(String key, ShapeId shapeId, ShapeId defaultTrait) {
            if (!inlineableShapes.contains(shapeId)) {
                codeWriter.write("$L: $I", key, shapeId);
                return Collections.emptyList();
            }

            StructureShape structure = model.expectShape(shapeId, StructureShape.class);
            if (hasOnlyDefaultTrait(structure, defaultTrait)) {
                codeWriter.writeInline("$L := ", key);
            } else {
                codeWriter.write("$L := ", key);
                codeWriter.indent();
                Map<ShapeId, Trait> traits = structure.getAllTraits();
                if (defaultTrait != null) {
                    traits = new HashMap<>(traits);
                    traits.remove(defaultTrait);
                }
                serializeTraits(traits);
            }

            List<MemberShape> nonMixinMembers = new ArrayList<>();
            List<MemberShape> mixinMembers = new ArrayList<>();
            for (MemberShape member : structure.members()) {
                if (member.getMixins().isEmpty()) {
                    nonMixinMembers.add(member);
                } else if (!member.getIntroducedTraits().isEmpty()) {
                    mixinMembers.add(member);
                }
            }

            writeMixins(structure);
            writeShapeMembers(nonMixinMembers);

            if (!hasOnlyDefaultTrait(structure, defaultTrait)) {
                codeWriter.dedent();
            }

            return mixinMembers;
        }

        private boolean hasOnlyDefaultTrait(Shape shape, ShapeId defaultTrait) {
            return shape.getAllTraits().size() == 1 && shape.hasTrait(defaultTrait);
        }
    }

    /**
     * Serializes nodes into the Smithy IDL format.
     */
    private static final class NodeSerializer {
        private final SmithyCodeWriter codeWriter;
        private final Model model;

        NodeSerializer(SmithyCodeWriter codeWriter, Model model) {
            this.codeWriter = codeWriter;
            this.model = model;
        }

        /**
         * Serialize a node into the Smithy IDL format.
         *
         * @param node The node to serialize.
         */
        private void serialize(Node node) {
            serialize(node, null);
        }

        /**
         * Serialize a node into the Smithy IDL format.
         *
         * <p>This uses the given shape to influence serialization. For example, a string shape marked with the idRef
         * trait will be serialized as a shape id rather than a string.
         *
         * @param node The node to serialize.
         * @param shape The shape of the node.
         */
        private void serialize(Node node, Shape shape) {
            // ShapeIds are represented differently than strings, so if a shape looks like it's
            // representing a shapeId we need to serialize it without quotes.
            if (isShapeId(shape)) {
                serializeShapeId(node.expectStringNode());
                return;
            }

            if (shape != null && shape.isMemberShape()) {
                shape = model.expectShape(shape.asMemberShape().get().getTarget());
            }

            if (node.isStringNode()) {
                serializeString(node.expectStringNode());
            } else if (node.isNumberNode()) {
                serializeNumber(node.expectNumberNode());
            } else if (node.isBooleanNode()) {
                serializeBoolean(node.expectBooleanNode());
            } else if (node.isNullNode()) {
                serializeNull();
            } else if (node.isArrayNode()) {
                serializeArray(node.expectArrayNode(), shape);
            } else if (node.isObjectNode()) {
                serializeObject(node.expectObjectNode(), shape);
            }
        }

        private boolean isShapeId(Shape shape) {
            if (shape == null) {
                return false;
            }
            return shape.getMemberTrait(model, IdRefTrait.class).isPresent();
        }

        private void serializeString(StringNode node) {
            codeWriter.writeInline("$S", node.getValue());
        }

        private void serializeShapeId(StringNode node) {
            codeWriter.writeInline("$I", node.getValue());
        }

        private void serializeNumber(NumberNode node) {
            codeWriter.writeInline("$L", node.getValue());
        }

        private void serializeBoolean(BooleanNode node) {
            codeWriter.writeInline(String.valueOf(node.getValue()));
        }

        private void serializeNull() {
            codeWriter.writeInline("null");
        }

        private void serializeArray(ArrayNode node, Shape shape) {
            if (node.isEmpty()) {
                codeWriter.writeInline("[]");
                return;
            }

            codeWriter.openBlockInline("[");

            // If it's not a collection shape, it'll be a document shape or null
            Shape member = shape;
            if (shape instanceof CollectionShape) {
                member = ((CollectionShape) shape).getMember();
            }

            for (Node element : node.getElements()) {
                // Elements will be written inline to enable them being written as values.
                // So here we need to ensure that they're written on a new line that's properly indented.
                codeWriter.write("");
                codeWriter.writeIndent();
                serialize(element, member);
            }
            codeWriter.write("");

            // We want to make sure to close without inserting a newline, as this could itself be a list element
            //or an object value.
            codeWriter.closeBlockWithoutNewline("]");
        }

        private void serializeObject(ObjectNode node, Shape shape) {
            if (node.isEmpty()) {
                codeWriter.writeInline("{}");
                return;
            }

            codeWriter.openBlockInline("{");
            serializeKeyValuePairs(node, shape);
            codeWriter.closeBlockWithoutNewline("}");
        }

        /**
         * Serialize an object node without the surrounding brackets.
         *
         * <p>This is mainly useful for serializing trait value nodes.
         *
         * @param node The node to serialize.
         * @param shape The shape of the node.
         */
        private void serializeKeyValuePairs(ObjectNode node, Shape shape) {
            if (node.isEmpty()) {
                return;
            }

            // If we're looking at a structure or union shape, we'll need to get the member shape based on the
            // node key. Here we pre-compute a mapping so we don't have to traverse the member list every time.
            Map<String, MemberShape> members;
            if (shape == null) {
                members = Collections.emptyMap();
            } else {
                members = shape.members().stream()
                        .collect(Collectors.toMap(MemberShape::getMemberName, Function.identity()));
            }

            node.getMembers().forEach((name, value) -> {
                // Try to find the member shape.
                Shape member;
                if (shape != null && shape.isMapShape()) {
                    // For maps the value member will always be the same.
                    member = shape.asMapShape().get().getValue();
                } else if (shape instanceof StructureShape || shape instanceof UnionShape) {
                    member = members.get(name.getValue());
                } else {
                    // At this point the shape is either null or a document shape.
                    member = shape;
                }

                codeWriter.writeInline("\n$K: ", name.getValue());
                serialize(value, member);
            });
            codeWriter.write("");
        }
    }

    /**
     * Extension of {@link CodeWriter} that provides additional convenience methods.
     *
     * <p>Provides a built in $I formatter that formats shape ids, automatically trimming namespace where possible.
     */
    private static final class SmithyCodeWriter extends CodeWriter {
        private static final Pattern UNQUOTED_KEY_STRING = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
        private final String namespace;
        private final Model model;
        private final Set<ShapeId> imports;

        SmithyCodeWriter(String namespace, Model model) {
            super();
            this.namespace = namespace;
            this.model = model;
            this.imports = new HashSet<>();
            trimTrailingSpaces();
            trimBlankLines();
            putFormatter('I', (s, i) -> formatShapeId(s));
            putFormatter('K', this::optionallyQuoteKey);
        }

        /**
         * Opens a block without writing indentation whitespace or inserting a newline.
         */
        private SmithyCodeWriter openBlockInline(String content, Object... args) {
            writeInline(content, args).indent();
            return this;
        }

        /**
         * Closes a block without inserting a newline.
         */
        private SmithyCodeWriter closeBlockWithoutNewline(String content, Object... args) {
            setNewline("");
            closeBlock(content, args);
            setNewline("\n");
            return this;
        }

        /**
         * Writes an empty line that contains only indentation appropriate to the current indentation level.
         *
         * <p> This does not insert a trailing newline.
         */
        private SmithyCodeWriter writeIndent() {
            setNewline("");
            // We explicitly want the trailing spaces, so disable trimming for this call.
            trimTrailingSpaces(false);
            write("");
            trimTrailingSpaces();
            setNewline("\n");
            return this;
        }

        private String formatShapeId(Object value) {
            if (value == null) {
                return "";
            }
            ShapeId shapeId = ShapeId.from(String.valueOf(value));
            if (!shouldWriteNamespace(shapeId)) {
                return shapeId.asRelativeReference();
            }
            return shapeId.toString();
        }

        private boolean shouldWriteNamespace(ShapeId shapeId) {
            if (shapeId.getNamespace().equals(namespace)) {
                return false;
            }
            if (Prelude.isPublicPreludeShape(shapeId)) {
                return conflictsWithLocalNamespace(shapeId);
            }
            if (shouldImport(shapeId)) {
                imports.add(shapeId.withoutMember());
            }
            return !imports.contains(shapeId);
        }

        private boolean conflictsWithLocalNamespace(ShapeId shapeId) {
            return model.getShape(ShapeId.fromParts(namespace, shapeId.getName())).isPresent();
        }

        private boolean shouldImport(ShapeId shapeId) {
            return !conflictsWithLocalNamespace(shapeId)
                    // It's easier to simply never import something that conflicts with the prelude, because
                    // if we did then we'd have to somehow handle rewriting all existing references to the
                    // prelude shape that it conflicts with.
                    && !conflictsWithPreludeNamespace(shapeId)
                    && !conflictsWithImports(shapeId);

        }

        private boolean conflictsWithPreludeNamespace(ShapeId shapeId) {
            return Prelude.isPublicPreludeShape(ShapeId.fromParts(Prelude.NAMESPACE, shapeId.getName()));
        }

        private boolean conflictsWithImports(ShapeId shapeId) {
            return imports.stream().anyMatch(importId -> importId.getName().equals(shapeId.getName()));
        }

        /**
         * Writes a possibly-empty list where each element is a shape id.
         *
         * <p>If the list is empty, nothing is written.
         */
        private SmithyCodeWriter writeOptionalIdList(String textBeforeList, Collection<ShapeId> shapeIds) {
            if (shapeIds.isEmpty()) {
                return this;
            }

            openBlock("$L: [", textBeforeList);
            shapeIds.stream().sorted().forEach(shapeId -> write("$I", shapeId));
            closeBlock("]");

            return this;
        }

        /**
         * Formatter that quotes (and escapes) a string unless it's a valid object key string.
         */
        private String optionallyQuoteKey(Object key, String indent) {
            String formatted = AbstractCodeWriter.formatLiteral(key);
            if (UNQUOTED_KEY_STRING.matcher(formatted).matches()) {
                return formatted;
            }
            return StringUtils.escapeJavaString(formatted, indent);
        }

        @Override
        public String toString() {
            String contents = StringUtils.stripStart(super.toString(), null);
            if (imports.isEmpty()) {
                return contents;
            }
            String importString = imports.stream().sorted()
                    .map(shapeId -> String.format("use %s", shapeId.toString()))
                    .collect(Collectors.joining("\n"));
            return importString + "\n\n" + contents;
        }
    }
}
