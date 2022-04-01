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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * A singleton that loads Smithy models from the JSON AST versions 1.0 and 2.0.
 */
enum AstModelLoader {

    INSTANCE;

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
    private static final List<String> SIMPLE_PROPERTY_NAMES = ListUtils.of(TYPE, TRAITS);
    private static final List<String> NAMED_MEMBER_SHAPE_PROPERTY_NAMES = ListUtils.of(TYPE, MEMBERS, TRAITS, MIXINS);
    private static final List<String> COLLECTION_PROPERTY_NAMES = ListUtils.of(TYPE, "member", TRAITS);
    private static final List<String> MAP_PROPERTY_NAMES = ListUtils.of(TYPE, "key", "value", TRAITS);
    private static final Set<String> MEMBER_PROPERTIES = SetUtils.of(TARGET, TRAITS);
    private static final Set<String> REFERENCE_PROPERTIES = SetUtils.of(TARGET);
    private static final Set<String> OPERATION_PROPERTY_NAMES = SetUtils.of(
            TYPE, "input", "output", ERRORS, TRAITS);
    private static final Set<String> RESOURCE_PROPERTIES = SetUtils.of(
            TYPE, "create", "read", "update", "delete", "list", "put",
            "identifiers", "resources", "operations", "collectionOperations", "properties", TRAITS);
    private static final Set<String> SERVICE_PROPERTIES = SetUtils.of(
            TYPE, "version", "operations", "resources", "rename", ERRORS, TRAITS);

    ModelFile load(Version modelVersion, TraitFactory traitFactory, ObjectNode model) {
        FullyResolvedModelFile modelFile = new FullyResolvedModelFile(model.getSourceLocation().getFilename(),
                                                                      traitFactory);
        modelFile.setVersion(modelVersion);
        LoaderUtils.checkForAdditionalProperties(model, null, TOP_LEVEL_PROPERTIES, modelFile.events());
        loadMetadata(model, modelFile);
        loadShapes(model, modelFile);
        return modelFile;
    }

    private void loadMetadata(ObjectNode model, FullyResolvedModelFile modelFile) {
        try {
            model.getObjectMember(METADATA).ifPresent(metadata -> {
                for (Map.Entry<String, Node> entry : metadata.getStringMap().entrySet()) {
                    modelFile.putMetadata(entry.getKey(), entry.getValue());
                }
            });
        } catch (SourceException e) {
            modelFile.events().add(ValidationEvent.fromSourceException(e));
        }
    }

    private void loadShapes(ObjectNode model, FullyResolvedModelFile modelFile) {
        model.getObjectMember(SHAPES).ifPresent(shapes -> {
            for (Map.Entry<StringNode, Node> entry : shapes.getMembers().entrySet()) {
                ShapeId id = entry.getKey().expectShapeId();
                ObjectNode definition = entry.getValue().expectObjectNode();
                String type = definition.expectStringMember(TYPE).getValue();
                try {
                    loadShape(id, type, definition, modelFile);
                } catch (SourceException e) {
                    ValidationEvent event = ValidationEvent.fromSourceException(e).toBuilder().shapeId(id).build();
                    modelFile.events().add(event);
                }
            }
        });
    }

    private void loadShape(ShapeId id, String type, ObjectNode value, FullyResolvedModelFile modelFile) {
        switch (type) {
            case "blob":
                loadSimpleShape(id, value, BlobShape.builder(), modelFile);
                break;
            case "boolean":
                loadSimpleShape(id, value, BooleanShape.builder(), modelFile);
                break;
            case "byte":
                loadSimpleShape(id, value, ByteShape.builder(), modelFile);
                break;
            case "short":
                loadSimpleShape(id, value, ShortShape.builder(), modelFile);
                break;
            case "integer":
                loadSimpleShape(id, value, IntegerShape.builder(), modelFile);
                break;
            case "intEnum":
                loadNamedMemberShape(id, value, IntEnumShape.builder(), modelFile);
                break;
            case "long":
                loadSimpleShape(id, value, LongShape.builder(), modelFile);
                break;
            case "float":
                loadSimpleShape(id, value, FloatShape.builder(), modelFile);
                break;
            case "double":
                loadSimpleShape(id, value, DoubleShape.builder(), modelFile);
                break;
            case "document":
                loadSimpleShape(id, value, DocumentShape.builder(), modelFile);
                break;
            case "bigDecimal":
                loadSimpleShape(id, value, BigDecimalShape.builder(), modelFile);
                break;
            case "bigInteger":
                loadSimpleShape(id, value, BigIntegerShape.builder(), modelFile);
                break;
            case "string":
                loadSimpleShape(id, value, StringShape.builder(), modelFile);
                break;
            case "enum":
                loadNamedMemberShape(id, value, EnumShape.builder(), modelFile);
                break;
            case "timestamp":
                loadSimpleShape(id, value, TimestampShape.builder(), modelFile);
                break;
            case "list":
                loadCollection(id, value, ListShape.builder(), modelFile);
                break;
            case "set":
                loadCollection(id, value, SetShape.builder(), modelFile);
                break;
            case "map":
                loadMap(id, value, modelFile);
                break;
            case "resource":
                loadResource(id, value, modelFile);
                break;
            case "service":
                loadService(id, value, modelFile);
                break;
            case "structure":
                loadNamedMemberShape(id, value, StructureShape.builder(), modelFile);
                break;
            case "union":
                loadNamedMemberShape(id, value, UnionShape.builder(), modelFile);
                break;
            case "operation":
                loadOperation(id, value, modelFile);
                break;
            case "apply":
                LoaderUtils.checkForAdditionalProperties(value, id, APPLY_PROPERTIES, modelFile.events());
                applyTraits(id, value.expectObjectMember(TRAITS), modelFile);
                break;
            default:
                throw new SourceException("Invalid shape `type`: " + type, value);
        }
    }

    private void applyTraits(ShapeId id, ObjectNode traits, FullyResolvedModelFile modelFile) {
        for (Map.Entry<StringNode, Node> traitNode : traits.getMembers().entrySet()) {
            ShapeId traitId = traitNode.getKey().expectShapeId();
            // JSON AST model traits are never considered annotation traits, meaning
            // that a null value provided in the AST is not coerced in the same way
            // as an omitted value in the IDL (e.g., "@foo").
            modelFile.onTrait(id, traitId, traitNode.getValue());
        }
    }

    private void applyShapeTraits(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        node.getObjectMember(TRAITS).ifPresent(traits -> applyTraits(id, traits, modelFile));
    }

    private void loadMember(FullyResolvedModelFile modelFile, ShapeId id, ObjectNode targetNode) {
        LoaderUtils.checkForAdditionalProperties(targetNode, id, MEMBER_PROPERTIES, modelFile.events());
        MemberShape.Builder builder = MemberShape.builder().source(targetNode.getSourceLocation()).id(id);
        ShapeId target = targetNode.expectStringMember(TARGET).expectShapeId();
        builder.target(target);
        applyShapeTraits(id, targetNode, modelFile);
        modelFile.onShape(builder);
    }

    private void loadOptionalMember(FullyResolvedModelFile modelFile, ShapeId id, ObjectNode node, String member) {
        node.getObjectMember(member).ifPresent(targetNode -> loadMember(modelFile, id, targetNode));
    }

    private void loadCollection(
            ShapeId id,
            ObjectNode node,
            CollectionShape.Builder<?, ?> builder,
            FullyResolvedModelFile modelFile
    ) {
        LoaderUtils.checkForAdditionalProperties(node, id, COLLECTION_PROPERTY_NAMES, modelFile.events());
        applyShapeTraits(id, node, modelFile);
        loadOptionalMember(modelFile, id.withMember("member"), node, "member");
        modelFile.onShape(builder.id(id).source(node.getSourceLocation()));
        addMixins(id, node, modelFile);
    }

    private void loadMap(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        LoaderUtils.checkForAdditionalProperties(node, id, MAP_PROPERTY_NAMES, modelFile.events());
        loadOptionalMember(modelFile, id.withMember("key"), node, "key");
        loadOptionalMember(modelFile, id.withMember("value"), node, "value");
        applyShapeTraits(id, node, modelFile);
        modelFile.onShape(MapShape.builder().id(id).source(node.getSourceLocation()));
        addMixins(id, node, modelFile);
    }

    private void loadOperation(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        LoaderUtils.checkForAdditionalProperties(node, id, OPERATION_PROPERTY_NAMES, modelFile.events());
        applyShapeTraits(id, node, modelFile);
        OperationShape.Builder builder = OperationShape.builder()
                .id(id)
                .source(node.getSourceLocation())
                .addErrors(loadOptionalTargetList(modelFile, id, node, ERRORS));
        loadOptionalTarget(modelFile, id, node, "input").ifPresent(builder::input);
        loadOptionalTarget(modelFile, id, node, "output").ifPresent(builder::output);
        modelFile.onShape(builder);
        addMixins(id, node, modelFile);
    }

    private void loadResource(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        LoaderUtils.checkForAdditionalProperties(node, id, RESOURCE_PROPERTIES, modelFile.events());
        applyShapeTraits(id, node, modelFile);
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(node.getSourceLocation());
        loadOptionalTarget(modelFile, id, node, "put").ifPresent(builder::put);
        loadOptionalTarget(modelFile, id, node, "create").ifPresent(builder::create);
        loadOptionalTarget(modelFile, id, node, "read").ifPresent(builder::read);
        loadOptionalTarget(modelFile, id, node, "update").ifPresent(builder::update);
        loadOptionalTarget(modelFile, id, node, "delete").ifPresent(builder::delete);
        loadOptionalTarget(modelFile, id, node, "list").ifPresent(builder::list);
        builder.operations(loadOptionalTargetList(modelFile, id, node, "operations"));
        builder.collectionOperations(loadOptionalTargetList(modelFile, id, node, "collectionOperations"));
        builder.resources(loadOptionalTargetList(modelFile, id, node, "resources"));

        // Load identifiers and resolve forward references.
        node.getObjectMember("identifiers").ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                ShapeId target = loadReferenceBody(modelFile, id, entry.getValue());
                builder.addIdentifier(name, target);
            }
        });

        // Load properties and resolve forward references.
        node.getObjectMember("properties").ifPresent(properties -> {
            for (Map.Entry<StringNode, Node> entry : properties.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                ShapeId target = loadReferenceBody(modelFile, id, entry.getValue());
                builder.addProperty(name, target);
            }
        });

        modelFile.onShape(builder);
        addMixins(id, node, modelFile);
    }

    private void loadService(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        LoaderUtils.checkForAdditionalProperties(node, id, SERVICE_PROPERTIES, modelFile.events());
        applyShapeTraits(id, node, modelFile);
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(node.getSourceLocation());
        node.getStringMember("version").map(StringNode::getValue).ifPresent(builder::version);
        builder.operations(loadOptionalTargetList(modelFile, id, node, "operations"));
        builder.resources(loadOptionalTargetList(modelFile, id, node, "resources"));
        loadServiceRenameIntoBuilder(builder, node);
        builder.addErrors(loadOptionalTargetList(modelFile, id, node, ERRORS));
        modelFile.onShape(builder);
        addMixins(id, node, modelFile);
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

    private void loadSimpleShape(
            ShapeId id, ObjectNode node, AbstractShapeBuilder<?, ?> builder, FullyResolvedModelFile modelFile) {
        LoaderUtils.checkForAdditionalProperties(node, id, SIMPLE_PROPERTY_NAMES, modelFile.events());
        applyShapeTraits(id, node, modelFile);
        modelFile.onShape(builder.id(id).source(node.getSourceLocation()));
        addMixins(id, node, modelFile);
    }

    private void loadNamedMemberShape(
            ShapeId id,
            ObjectNode node,
            AbstractShapeBuilder<?, ?> builder,
            FullyResolvedModelFile modelFile
    ) {
        LoaderUtils.checkForAdditionalProperties(node, id, NAMED_MEMBER_SHAPE_PROPERTY_NAMES, modelFile.events());
        modelFile.onShape(builder.id(id).source(node.getSourceLocation()));
        finishLoadingNamedMemberShapeMembers(id, node, modelFile);
    }

    private void finishLoadingNamedMemberShapeMembers(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        applyShapeTraits(id, node, modelFile);
        ObjectNode memberObject = node.getObjectMember(MEMBERS).orElse(Node.objectNode());
        for (Map.Entry<String, Node> entry : memberObject.getStringMap().entrySet()) {
            loadMember(modelFile, id.withMember(entry.getKey()), entry.getValue().expectObjectNode());
        }
        addMixins(id, node, modelFile);
    }

    private void addMixins(ShapeId id, ObjectNode node, FullyResolvedModelFile modelFile) {
        ArrayNode mixins = node.getArrayMember(MIXINS).orElse(Node.arrayNode());
        for (ObjectNode mixin : mixins.getElementsAs(ObjectNode.class)) {
            modelFile.addPendingMixin(id, loadReferenceBody(modelFile, id, mixin));
        }
    }

    private Optional<ShapeId> loadOptionalTarget(
            FullyResolvedModelFile modelFile, ShapeId id, ObjectNode node, String member) {
        return node.getObjectMember(member).map(r -> loadReferenceBody(modelFile, id, r));
    }

    private ShapeId loadReferenceBody(FullyResolvedModelFile modelFile, ShapeId id, Node reference) {
        ObjectNode referenceObject = reference.expectObjectNode();
        LoaderUtils.checkForAdditionalProperties(referenceObject, id, REFERENCE_PROPERTIES, modelFile.events());
        return referenceObject.expectStringMember(TARGET).expectShapeId();
    }

    private List<ShapeId> loadOptionalTargetList(
            FullyResolvedModelFile modelFile, ShapeId id, ObjectNode node, String member) {
        return node.getArrayMember(member).map(array -> {
            List<ShapeId> ids = new ArrayList<>(array.size());
            for (Node element : array.getElements()) {
                ids.add(loadReferenceBody(modelFile, id, element));
            }
            return ids;
        }).orElseGet(Collections::emptyList);
    }
}
