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

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import software.amazon.smithy.model.traits.BooleanTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.StringUtils;

/**
 * Serializes a {@link Model} into a set of Smithy IDL files.
 */
public final class SmithyIdlModelSerializer {
    private final Predicate<String> metadataFilter;
    private final Predicate<Shape> shapeFilter;
    private final Predicate<Trait> traitFilter;
    private final Function<Shape, Path> shapePlacer;
    private final Path basePath;

    private SmithyIdlModelSerializer(Builder builder) {
        metadataFilter = builder.metadataFilter;
        shapeFilter = builder.shapeFilter.and(FunctionalUtils.not(Prelude::isPreludeShape));
        traitFilter = builder.traitFilter;
        basePath = builder.basePath;
        if (basePath != null) {
            Function<Shape, Path> shapePlacer = builder.shapePlacer;
            this.shapePlacer = shape -> this.basePath.resolve(shapePlacer.apply(shape));
        } else {
            this.shapePlacer = builder.shapePlacer;
        }
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

        ShapeSerializer shapeSerializer = new ShapeSerializer(codeWriter, nodeSerializer, traitFilter, fullModel);
        shapes.stream()
                .filter(FunctionalUtils.not(Shape::isMemberShape))
                .sorted(new ShapeComparator())
                .forEach(shape -> shape.accept(shapeSerializer));

        return serializeHeader(fullModel, namespace) + codeWriter.toString();
    }

    private String serializeHeader(Model fullModel, String namespace) {
        SmithyCodeWriter codeWriter = new SmithyCodeWriter(null, fullModel);
        NodeSerializer nodeSerializer = new NodeSerializer(codeWriter, fullModel);

        codeWriter.write("$$version: \"$L\"", Model.MODEL_VERSION).write("");

        // Write the full metadata into every output. When loaded back together the conflicts will be ignored,
        // but if they're separated out then each file will still have all the context.
        fullModel.getMetadata().entrySet().stream()
                .filter(entry -> metadataFilter.test(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    codeWriter.trimTrailingSpaces(false)
                            .writeInline("metadata $M = ", entry.getKey())
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
     * Comparator used to sort shapes.
     */
    private static final class ShapeComparator implements Comparator<Shape>, Serializable {
        private static final Map<ShapeType, Integer> PRIORITY = MapUtils.of(
                ShapeType.SERVICE, 0,
                ShapeType.RESOURCE, 1,
                ShapeType.OPERATION, 2,
                ShapeType.STRUCTURE, 3,
                ShapeType.UNION, 4,
                ShapeType.LIST, 5,
                ShapeType.SET, 6,
                ShapeType.MAP, 7
        );


        @Override
        public int compare(Shape s1, Shape s2) {
            // Traits go first
            if (s1.hasTrait(TraitDefinition.class) || s2.hasTrait(TraitDefinition.class)) {
                if (!s1.hasTrait(TraitDefinition.class)) {
                    return 1;
                }
                if (!s2.hasTrait(TraitDefinition.class)) {
                    return -1;
                }
                // The other sorting rules don't matter for traits.
                return s1.compareTo(s2);
            }
            // If the shapes are the same type, just compare their shape ids.
            if (s1.getType().equals(s2.getType())) {
                return s1.compareTo(s2);
            }
            // If one shape is prioritized, compare by priority.
            if (PRIORITY.containsKey(s1.getType()) || PRIORITY.containsKey(s2.getType())) {
                // If only one shape is prioritized, that shape is "greater".
                if (!PRIORITY.containsKey(s1.getType())) {
                    return 1;
                }
                if (!PRIORITY.containsKey(s2.getType())) {
                    return -1;
                }
                return PRIORITY.get(s1.getType()) - PRIORITY.get(s2.getType());
            }
            return s1.compareTo(s2);
        }
    }

    /**
     * Builder used to create {@link SmithyIdlModelSerializer}.
     */
    public static final class Builder implements SmithyBuilder<SmithyIdlModelSerializer> {
        private Predicate<String> metadataFilter = pair -> true;
        private Predicate<Shape> shapeFilter = shape -> true;
        private Predicate<Trait> traitFilter = trait -> true;
        private Function<Shape, Path> shapePlacer = SmithyIdlModelSerializer::placeShapesByNamespace;
        private Path basePath = null;

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

        ShapeSerializer(
                SmithyCodeWriter codeWriter,
                NodeSerializer nodeSerializer,
                Predicate<Trait> traitFilter,
                Model model
        ) {
            this.codeWriter = codeWriter;
            this.nodeSerializer = nodeSerializer;
            this.traitFilter = traitFilter;
            this.model = model;
        }

        @Override
        protected Void getDefault(Shape shape) {
            serializeTraits(shape);
            codeWriter.write("$L $L", shape.getType(), shape.getId().getName()).write("");
            return null;
        }

        private void shapeWithMembers(Shape shape, List<MemberShape> members) {
            serializeTraits(shape);
            if (members.isEmpty()) {
                // If there are no members then we don't want to introduce an unnecessary newline by opening a block.
                codeWriter.write("$L $L {}", shape.getType(), shape.getId().getName()).write("");
                return;
            }

            codeWriter.openBlock("$L $L {", shape.getType(), shape.getId().getName());
            for (MemberShape member : members) {
                serializeTraits(member);
                codeWriter.write("$L: $I,", member.getMemberName(), member.getTarget());
            }
            codeWriter.closeBlock("}").write("");
        }

        private void serializeTraits(Shape shape) {
            // The documentation trait always needs to be serialized first since it uses special syntax.
            shape.getTrait(DocumentationTrait.class).filter(traitFilter).ifPresent(this::serializeDocumentationTrait);
            shape.getAllTraits().values().stream()
                    .filter(trait -> !(trait instanceof DocumentationTrait))
                    .filter(traitFilter)
                    .sorted(Comparator.comparing(Trait::toShapeId))
                    .forEach(this::serializeTrait);
        }

        private void serializeDocumentationTrait(DocumentationTrait trait) {
            // The documentation trait has a special syntax, which we always want to use.
            codeWriter.setNewlinePrefix("/// ")
                    .write(trait.getValue().replace("$", "$$"))
                    .setNewlinePrefix("");
        }

        private void serializeTrait(Trait trait) {
            Node node = trait.toNode();
            Shape shape = model.expectShape(trait.toShapeId());

            if (trait instanceof BooleanTrait || isEmptyStructure(node, shape)) {
                // Traits that inherit from BooleanTrait specifically can omit a value.
                // Traits that are simply boolean shapes which don't implement BooleanTrait cannot.
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
        public Void listShape(ListShape shape) {
            shapeWithMembers(shape, Collections.singletonList(shape.getMember()));
            return null;
        }

        @Override
        public Void setShape(SetShape shape) {
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
            shapeWithMembers(shape, new ArrayList<>(shape.getAllMembers().values()));
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            shapeWithMembers(shape, new ArrayList<>(shape.getAllMembers().values()));
            return null;
        }

        @Override
        public Void serviceShape(ServiceShape shape) {
            serializeTraits(shape);
            codeWriter.openBlock("service $L {", shape.getId().getName())
                    .write("version: $S,", shape.getVersion());
            codeWriter.writeOptionalIdList("operations", shape.getOperations());
            codeWriter.writeOptionalIdList("resources", shape.getResources());
            codeWriter.closeBlock("}").write("");
            return null;
        }

        @Override
        public Void resourceShape(ResourceShape shape) {
            serializeTraits(shape);
            codeWriter.openBlock("resource $L {", shape.getId().getName());
            if (!shape.getIdentifiers().isEmpty()) {
                codeWriter.openBlock("identifiers: {");
                shape.getIdentifiers().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> codeWriter.write(
                                "$L: $I,", entry.getKey(), entry.getValue()));
                codeWriter.closeBlock("},");
            }

            shape.getPut().ifPresent(shapeId -> codeWriter.write("put: $I,", shapeId));
            shape.getCreate().ifPresent(shapeId -> codeWriter.write("create: $I,", shapeId));
            shape.getRead().ifPresent(shapeId -> codeWriter.write("read: $I,", shapeId));
            shape.getUpdate().ifPresent(shapeId -> codeWriter.write("update: $I,", shapeId));
            shape.getDelete().ifPresent(shapeId -> codeWriter.write("delete: $I,", shapeId));
            shape.getList().ifPresent(shapeId -> codeWriter.write("list: $I,", shapeId));
            codeWriter.writeOptionalIdList("operations", shape.getOperations());
            codeWriter.writeOptionalIdList("collectionOperations", shape.getCollectionOperations());
            codeWriter.writeOptionalIdList("resources", shape.getResources());

            codeWriter.closeBlock("}");
            codeWriter.write("");
            return null;
        }

        @Override
        public Void operationShape(OperationShape shape) {
            serializeTraits(shape);
            if (isEmptyOperation(shape)) {
                codeWriter.write("operation $L {}", shape.getId().getName()).write("");
                return null;
            }

            codeWriter.openBlock("operation $L {", shape.getId().getName());
            shape.getInput().ifPresent(shapeId -> codeWriter.write("input: $I,", shapeId));
            shape.getOutput().ifPresent(shapeId -> codeWriter.write("output: $I,", shapeId));
            codeWriter.writeOptionalIdList("errors", shape.getErrors());
            codeWriter.closeBlock("}");
            codeWriter.write("");
            return null;
        }

        private boolean isEmptyOperation(OperationShape shape) {
            return !(shape.getInput().isPresent() || shape.getOutput().isPresent() || !shape.getErrors().isEmpty());
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
                shape = model.expectShape(shape.expectMemberShape().getTarget());
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
                codeWriter.writeInline(",");
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
                    member = shape.expectMapShape().getValue();
                } else if (shape instanceof NamedMembersShape) {
                    member = members.get(name.getValue());
                } else {
                    // At this point the shape is either null or a document shape.
                    member = shape;
                }

                codeWriter.writeInline("\n$M: ", name.getValue());
                serialize(value, member);
                codeWriter.writeInline(",");
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
        private static final Pattern UNQUOTED_STRING = Pattern.compile("[a-zA-Z_][\\w$.#]*");
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
            putFormatter('M', this::optionallyQuoteString);
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
            shapeIds.stream().sorted().forEach(shapeId -> write("$I,", shapeId));
            closeBlock("],");

            return this;
        }

        /**
         * Formatter that quotes (and escapes) a string unless it's a valid unquoted string.
         */
        private String optionallyQuoteString(Object key, String indent) {
            String formatted = CodeWriter.formatLiteral(key);
            if (UNQUOTED_STRING.matcher(formatted).matches()) {
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
