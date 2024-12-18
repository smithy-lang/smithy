/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * A singleton that loads Smithy models from the JSON AST versions 1.0 and 2.0.
 */
final class AstModelLoader {

    private static final String METADATA = "metadata";
    private static final String MEMBERS = "members";
    private static final String SHAPES = "shapes";
    private static final String TRAITS = "traits";
    private static final String TYPE = "type";
    private static final String TARGET = "target";
    private static final String ERRORS = "errors";
    private static final String MIXINS = "mixins";

    private static final List<String> TOP_LEVEL_PROPERTIES = ListUtils.of("smithy", SHAPES, METADATA);
    private static final List<String> APPLY_PROPERTIES = ListUtils.of(TYPE, TRAITS);
    private static final List<String> SIMPLE_PROPERTY_NAMES = ListUtils.of(TYPE, TRAITS, MIXINS);
    private static final List<String> NAMED_MEMBER_SHAPE_PROPERTY_NAMES = ListUtils.of(TYPE, MEMBERS, TRAITS, MIXINS);
    private static final List<String> COLLECTION_PROPERTY_NAMES = ListUtils.of(TYPE, "member", TRAITS, MIXINS);
    private static final List<String> MAP_PROPERTY_NAMES = ListUtils.of(TYPE, "key", "value", TRAITS, MIXINS);
    private static final Set<String> MEMBER_PROPERTIES = SetUtils.of(TARGET, TRAITS);
    private static final Set<String> REFERENCE_PROPERTIES = SetUtils.of(TARGET);
    private static final Set<String> OPERATION_PROPERTY_NAMES = SetUtils.of(
            TYPE,
            "input",
            "output",
            ERRORS,
            TRAITS,
            MIXINS);
    private static final Set<String> RESOURCE_PROPERTIES = SetUtils.of(
            TYPE,
            "create",
            "read",
            "update",
            "delete",
            "list",
            "put",
            "identifiers",
            "resources",
            "operations",
            "collectionOperations",
            "properties",
            TRAITS,
            MIXINS);
    private static final Set<String> SERVICE_PROPERTIES = SetUtils.of(
            TYPE,
            "version",
            "operations",
            "resources",
            "rename",
            ERRORS,
            TRAITS,
            MIXINS);

    private final Version modelVersion;
    private final ObjectNode model;
    private Consumer<LoadOperation> operations;

    AstModelLoader(Version modelVersion, ObjectNode model) {
        this.modelVersion = modelVersion;
        this.model = model;
    }

    void parse(Consumer<LoadOperation> consumer) {
        operations = consumer;
        LoaderUtils.checkForAdditionalProperties(model, null, TOP_LEVEL_PROPERTIES).ifPresent(this::emit);
        StringNode versionNode = model.expectStringMember("smithy");
        consumer.accept(new LoadOperation.ModelVersion(modelVersion, versionNode.getSourceLocation()));
        loadMetadata();
        loadShapes();
    }

    private void emit(ValidationEvent event) {
        operations.accept(new LoadOperation.Event(event));
    }

    private void loadMetadata() {
        try {
            model.getObjectMember(METADATA).ifPresent(metadata -> {
                for (Map.Entry<String, Node> entry : metadata.getStringMap().entrySet()) {
                    operations.accept(new LoadOperation.PutMetadata(modelVersion, entry.getKey(), entry.getValue()));
                }
            });
        } catch (SourceException e) {
            emit(ValidationEvent.fromSourceException(e));
        }
    }

    private void loadShapes() {
        model.getObjectMember(SHAPES).ifPresent(shapes -> {
            for (Map.Entry<StringNode, Node> entry : shapes.getMembers().entrySet()) {
                ShapeId id = entry.getKey().expectShapeId();
                ObjectNode definition = entry.getValue().expectObjectNode();
                String type = definition.expectStringMember(TYPE).getValue();
                try {
                    // Note: loadShape() returns null when using apply for traits.
                    LoadOperation.DefineShape defineShape = loadShape(id, type, definition);
                    if (defineShape != null) {
                        operations.accept(defineShape);
                    }
                } catch (SourceException e) {
                    ValidationEvent event = ValidationEvent.fromSourceException(e).toBuilder().shapeId(id).build();
                    emit(event);
                }
            }
        });
    }

    private LoadOperation.DefineShape loadShape(ShapeId id, String type, ObjectNode value) {
        switch (type) {
            case "blob":
                return loadSimpleShape(id, value, BlobShape.builder());
            case "boolean":
                return loadSimpleShape(id, value, BooleanShape.builder());
            case "byte":
                return loadSimpleShape(id, value, ByteShape.builder());
            case "short":
                return loadSimpleShape(id, value, ShortShape.builder());
            case "integer":
                return loadSimpleShape(id, value, IntegerShape.builder());
            case "intEnum":
                return loadNamedMemberShape(id, value, IntEnumShape.builder());
            case "long":
                return loadSimpleShape(id, value, LongShape.builder());
            case "float":
                return loadSimpleShape(id, value, FloatShape.builder());
            case "double":
                return loadSimpleShape(id, value, DoubleShape.builder());
            case "document":
                return loadSimpleShape(id, value, DocumentShape.builder());
            case "bigDecimal":
                return loadSimpleShape(id, value, BigDecimalShape.builder());
            case "bigInteger":
                return loadSimpleShape(id, value, BigIntegerShape.builder());
            case "string":
                return loadSimpleShape(id, value, StringShape.builder());
            case "enum":
                return loadNamedMemberShape(id, value, EnumShape.builder());
            case "timestamp":
                return loadSimpleShape(id, value, TimestampShape.builder());
            case "list":
                return loadCollection(id, value, ListShape.builder());
            case "set":
                return loadCollection(id, value, SetShape.builder());
            case "map":
                return loadMap(id, value);
            case "resource":
                return loadResource(id, value);
            case "service":
                return loadService(id, value);
            case "structure":
                return loadNamedMemberShape(id, value, StructureShape.builder());
            case "union":
                return loadNamedMemberShape(id, value, UnionShape.builder());
            case "operation":
                return loadOperation(id, value);
            case "apply":
                LoaderUtils.checkForAdditionalProperties(value, id, APPLY_PROPERTIES).ifPresent(this::emit);
                value.getObjectMember(TRAITS).ifPresent(traits -> applyTraits(id, traits));
                return null;
            default:
                throw new SourceException("Invalid shape `type`: " + type, value);
        }
    }

    private void applyTraits(ShapeId id, ObjectNode traits) {
        for (Map.Entry<StringNode, Node> traitNode : traits.getMembers().entrySet()) {
            ShapeId traitId = traitNode.getKey().expectShapeId();
            // JSON AST model traits are never considered annotation traits, meaning
            // that a null value provided in the AST is not coerced in the same way
            // as an omitted value in the IDL (e.g., "@foo").
            operations.accept(new LoadOperation.ApplyTrait(modelVersion,
                    traitNode.getKey().getSourceLocation(),
                    id.getNamespace(),
                    id,
                    traitId,
                    traitNode.getValue()));
        }
    }

    private void applyShapeTraits(ShapeId id, ObjectNode node) {
        node.getObjectMember(TRAITS).ifPresent(traits -> applyTraits(id, traits));
    }

    private void loadMember(LoadOperation.DefineShape operation, ShapeId id, ObjectNode targetNode) {
        LoaderUtils.checkForAdditionalProperties(targetNode, id, MEMBER_PROPERTIES).ifPresent(this::emit);
        MemberShape.Builder builder = MemberShape.builder().source(targetNode.getSourceLocation()).id(id);
        ShapeId target = targetNode.expectStringMember(TARGET).expectShapeId();
        builder.target(target);
        applyShapeTraits(id, targetNode);
        operation.addMember(builder);
    }

    private void loadOptionalMember(LoadOperation.DefineShape operation, ShapeId id, ObjectNode node, String member) {
        node.getObjectMember(member).ifPresent(targetNode -> loadMember(operation, id, targetNode));
    }

    private LoadOperation.DefineShape loadCollection(
            ShapeId id,
            ObjectNode node,
            CollectionShape.Builder<?, ?> builder
    ) {
        LoaderUtils.checkForAdditionalProperties(node, id, COLLECTION_PROPERTY_NAMES).ifPresent(this::emit);
        applyShapeTraits(id, node);
        // Add the container before members to ensure sets are rejected before adding unreferenced members.
        builder.id(id).source(node.getSourceLocation());
        LoadOperation.DefineShape operation = createShape(builder);
        loadOptionalMember(operation, id.withMember("member"), node, "member");
        addMixins(operation, node);
        return operation;
    }

    LoadOperation.DefineShape createShape(AbstractShapeBuilder<?, ?> builder) {
        return new LoadOperation.DefineShape(modelVersion, builder);
    }

    private LoadOperation.DefineShape loadMap(ShapeId id, ObjectNode node) {
        LoaderUtils.checkForAdditionalProperties(node, id, MAP_PROPERTY_NAMES).ifPresent(this::emit);
        MapShape.Builder builder = MapShape.builder().id(id).source(node.getSourceLocation());
        LoadOperation.DefineShape operation = createShape(builder);
        loadOptionalMember(operation, id.withMember("key"), node, "key");
        loadOptionalMember(operation, id.withMember("value"), node, "value");
        addMixins(operation, node);
        applyShapeTraits(id, node);
        return operation;
    }

    private LoadOperation.DefineShape loadOperation(ShapeId id, ObjectNode node) {
        LoaderUtils.checkForAdditionalProperties(node, id, OPERATION_PROPERTY_NAMES).ifPresent(this::emit);
        applyShapeTraits(id, node);
        OperationShape.Builder builder = OperationShape.builder()
                .id(id)
                .source(node.getSourceLocation())
                .addErrors(loadOptionalTargetList(id, node, ERRORS));
        loadOptionalTarget(id, node, "input").ifPresent(builder::input);
        loadOptionalTarget(id, node, "output").ifPresent(builder::output);
        LoadOperation.DefineShape operation = createShape(builder);
        addMixins(operation, node);
        return operation;
    }

    private LoadOperation.DefineShape loadResource(ShapeId id, ObjectNode node) {
        LoaderUtils.checkForAdditionalProperties(node, id, RESOURCE_PROPERTIES).ifPresent(this::emit);
        applyShapeTraits(id, node);
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(node.getSourceLocation());
        loadOptionalTarget(id, node, "put").ifPresent(builder::put);
        loadOptionalTarget(id, node, "create").ifPresent(builder::create);
        loadOptionalTarget(id, node, "read").ifPresent(builder::read);
        loadOptionalTarget(id, node, "update").ifPresent(builder::update);
        loadOptionalTarget(id, node, "delete").ifPresent(builder::delete);
        loadOptionalTarget(id, node, "list").ifPresent(builder::list);
        builder.operations(loadOptionalTargetList(id, node, "operations"));
        builder.collectionOperations(loadOptionalTargetList(id, node, "collectionOperations"));
        builder.resources(loadOptionalTargetList(id, node, "resources"));

        // Load identifiers and resolve forward references.
        node.getObjectMember("identifiers").ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                ShapeId target = loadReferenceBody(id, entry.getValue());
                builder.addIdentifier(name, target);
            }
        });

        // Load properties and resolve forward references.
        node.getObjectMember("properties").ifPresent(properties -> {
            if (!modelVersion.supportsResourceProperties()) {
                emit(ValidationEvent.builder()
                        .sourceLocation(properties.getSourceLocation())
                        .id(Validator.MODEL_ERROR)
                        .severity(Severity.ERROR)
                        .message("Resource properties can only be used with Smithy version 2 or later. "
                                + "Attempted to use resource properties with version `" + modelVersion + "`.")
                        .build());
            }
            for (Map.Entry<StringNode, Node> entry : properties.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                ShapeId target = loadReferenceBody(id, entry.getValue());
                builder.addProperty(name, target);
            }
        });

        LoadOperation.DefineShape operation = createShape(builder);
        addMixins(operation, node);
        return operation;
    }

    private LoadOperation.DefineShape loadService(ShapeId id, ObjectNode node) {
        LoaderUtils.checkForAdditionalProperties(node, id, SERVICE_PROPERTIES).ifPresent(this::emit);
        applyShapeTraits(id, node);
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(node.getSourceLocation());
        node.getStringMember("version").map(StringNode::getValue).ifPresent(builder::version);
        builder.operations(loadOptionalTargetList(id, node, "operations"));
        builder.resources(loadOptionalTargetList(id, node, "resources"));
        loadServiceRenameIntoBuilder(builder, node);
        builder.addErrors(loadOptionalTargetList(id, node, ERRORS));
        LoadOperation.DefineShape operation = createShape(builder);
        addMixins(operation, node);
        return operation;
    }

    static void loadServiceRenameIntoBuilder(ServiceShape.Builder builder, ObjectNode node) {
        node.getObjectMember("rename").ifPresent(rename -> {
            for (Map.Entry<StringNode, Node> entry : rename.getMembers().entrySet()) {
                ShapeId fromId = entry.getKey().expectShapeId();
                String toName = entry.getValue().expectStringNode().getValue();
                builder.putRename(fromId, toName);
            }
        });
    }

    private LoadOperation.DefineShape loadSimpleShape(
            ShapeId id,
            ObjectNode node,
            AbstractShapeBuilder<?, ?> builder
    ) {
        LoaderUtils.checkForAdditionalProperties(node, id, SIMPLE_PROPERTY_NAMES).ifPresent(this::emit);
        applyShapeTraits(id, node);
        builder.id(id).source(node.getSourceLocation());
        LoadOperation.DefineShape operation = createShape(builder);
        addMixins(operation, node);
        return operation;
    }

    private LoadOperation.DefineShape loadNamedMemberShape(
            ShapeId id,
            ObjectNode node,
            AbstractShapeBuilder<?, ?> builder
    ) {
        LoaderUtils.checkForAdditionalProperties(node, id, NAMED_MEMBER_SHAPE_PROPERTY_NAMES).ifPresent(this::emit);
        builder.id(id).source(node.getSourceLocation());
        LoadOperation.DefineShape operation = createShape(builder);
        finishLoadingNamedMemberShapeMembers(operation, node);
        return operation;
    }

    private void finishLoadingNamedMemberShapeMembers(LoadOperation.DefineShape operation, ObjectNode node) {
        applyShapeTraits(operation.toShapeId(), node);
        ObjectNode memberObject = node.getObjectMember(MEMBERS).orElse(Node.objectNode());
        for (Map.Entry<String, Node> entry : memberObject.getStringMap().entrySet()) {
            loadMember(operation,
                    operation.toShapeId().withMember(entry.getKey()),
                    entry.getValue().expectObjectNode());
        }
        addMixins(operation, node);
    }

    private void addMixins(LoadOperation.DefineShape operation, ObjectNode node) {
        ArrayNode mixins = node.getArrayMember(MIXINS).orElse(Node.arrayNode());
        for (ObjectNode mixin : mixins.getElementsAs(ObjectNode.class)) {
            ShapeId mixinId = loadReferenceBody(operation.toShapeId(), mixin);
            operation.addDependency(mixinId);
            operation.addModifier(new ApplyMixin(mixinId));
        }
    }

    private Optional<ShapeId> loadOptionalTarget(ShapeId id, ObjectNode node, String member) {
        return node.getObjectMember(member).map(r -> loadReferenceBody(id, r));
    }

    private ShapeId loadReferenceBody(ShapeId id, Node reference) {
        ObjectNode referenceObject = reference.expectObjectNode();
        LoaderUtils.checkForAdditionalProperties(referenceObject, id, REFERENCE_PROPERTIES).ifPresent(this::emit);
        return referenceObject.expectStringMember(TARGET).expectShapeId();
    }

    private List<ShapeId> loadOptionalTargetList(ShapeId id, ObjectNode node, String member) {
        return node.getArrayMember(member).map(array -> {
            List<ShapeId> ids = new ArrayList<>(array.size());
            for (Node element : array.getElements()) {
                ids.add(loadReferenceBody(id, element));
            }
            return ids;
        }).orElseGet(Collections::emptyList);
    }
}
