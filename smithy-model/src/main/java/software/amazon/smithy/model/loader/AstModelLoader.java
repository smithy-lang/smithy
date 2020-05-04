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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
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
import software.amazon.smithy.model.shapes.FloatShape;
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
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * A singleton that loads Smithy models from the JSON AST format 1.0.
 */
enum AstModelLoader {

    INSTANCE;

    private static final String METADATA = "metadata";
    private static final String MEMBERS = "members";
    private static final String SHAPES = "shapes";
    private static final String TRAITS = "traits";
    private static final String TYPE = "type";
    private static final String TARGET = "target";

    private static final List<String> TOP_LEVEL_PROPERTIES = ListUtils.of("smithy", SHAPES, METADATA);
    private static final List<String> APPLY_PROPERTIES = ListUtils.of(TYPE, TRAITS);
    private static final List<String> SIMPLE_PROPERTY_NAMES = ListUtils.of(TYPE, TRAITS);
    private static final List<String> STRUCTURE_AND_UNION_PROPERTY_NAMES = ListUtils.of(TYPE, MEMBERS, TRAITS);
    private static final List<String> COLLECTION_PROPERTY_NAMES = ListUtils.of(TYPE, "member", TRAITS);
    private static final List<String> MAP_PROPERTY_NAMES = ListUtils.of(TYPE, "key", "value", TRAITS);
    private static final Set<String> MEMBER_PROPERTIES = SetUtils.of(TARGET, TRAITS);
    private static final Set<String> REFERENCE_PROPERTIES = SetUtils.of(TARGET);
    private static final Set<String> OPERATION_PROPERTY_NAMES = SetUtils.of(
            TYPE, "input", "output", "errors", TRAITS);
    private static final Set<String> RESOURCE_PROPERTIES = SetUtils.of(
            TYPE, "create", "read", "update", "delete", "list", "put",
            "identifiers", "resources", "operations", "collectionOperations", TRAITS);
    private static final Set<String> SERVICE_PROPERTIES = SetUtils.of(
            TYPE, "version", "operations", "resources", TRAITS);

    void load(ObjectNode model, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(model, null, TOP_LEVEL_PROPERTIES);
        loadMetadata(model, visitor);
        loadShapes(model, visitor);
    }

    private void loadMetadata(ObjectNode model, LoaderVisitor visitor) {
        try {
            model.getObjectMember(METADATA).ifPresent(metadata -> {
                for (Map.Entry<String, Node> entry : metadata.getStringMap().entrySet()) {
                    visitor.onMetadata(entry.getKey(), entry.getValue());
                }
            });
        } catch (SourceException e) {
            visitor.onError(ValidationEvent.fromSourceException(e));
        }
    }

    private void loadShapes(ObjectNode model, LoaderVisitor visitor) {
        model.getObjectMember(SHAPES).ifPresent(shapes -> {
            for (Map.Entry<StringNode, Node> entry : shapes.getMembers().entrySet()) {
                ShapeId id = entry.getKey().expectShapeId();
                ObjectNode definition = entry.getValue().expectObjectNode();
                String type = definition.expectStringMember(TYPE).getValue();
                try {
                    loadShape(id, type, definition, visitor);
                } catch (SourceException e) {
                    ValidationEvent event = ValidationEvent.fromSourceException(e).toBuilder().shapeId(id).build();
                    visitor.onError(event);
                }
            }
        });
    }

    private void loadShape(ShapeId id, String type, ObjectNode value, LoaderVisitor visitor) {
        switch (type) {
            case "blob":
                loadSimpleShape(id, value, BlobShape.builder(), visitor);
                break;
            case "boolean":
                loadSimpleShape(id, value, BooleanShape.builder(), visitor);
                break;
            case "byte":
                loadSimpleShape(id, value, ByteShape.builder(), visitor);
                break;
            case "short":
                loadSimpleShape(id, value, ShortShape.builder(), visitor);
                break;
            case "integer":
                loadSimpleShape(id, value, IntegerShape.builder(), visitor);
                break;
            case "long":
                loadSimpleShape(id, value, LongShape.builder(), visitor);
                break;
            case "float":
                loadSimpleShape(id, value, FloatShape.builder(), visitor);
                break;
            case "double":
                loadSimpleShape(id, value, DoubleShape.builder(), visitor);
                break;
            case "document":
                loadSimpleShape(id, value, DocumentShape.builder(), visitor);
                break;
            case "bigDecimal":
                loadSimpleShape(id, value, BigDecimalShape.builder(), visitor);
                break;
            case "bigInteger":
                loadSimpleShape(id, value, BigIntegerShape.builder(), visitor);
                break;
            case "string":
                loadSimpleShape(id, value, StringShape.builder(), visitor);
                break;
            case "timestamp":
                loadSimpleShape(id, value, TimestampShape.builder(), visitor);
                break;
            case "list":
                loadCollection(id, value, ListShape.builder(), visitor);
                break;
            case "set":
                loadCollection(id, value, SetShape.builder(), visitor);
                break;
            case "map":
                loadMap(id, value, visitor);
                break;
            case "resource":
                loadResource(id, value, visitor);
                break;
            case "service":
                loadService(id, value, visitor);
                break;
            case "structure":
                loadStructure(id, value, visitor);
                break;
            case "union":
                loadUnion(id, value, visitor);
                break;
            case "operation":
                loadOperation(id, value, visitor);
                break;
            case "apply":
                visitor.checkForAdditionalProperties(value, id, APPLY_PROPERTIES);
                applyTraits(id, value.expectObjectMember(TRAITS), visitor);
                break;
            default:
                throw new SourceException("Invalid shape `type`: " + type, value);
        }
    }

    private void applyTraits(ShapeId id, ObjectNode traits, LoaderVisitor visitor) {
        for (Map.Entry<StringNode, Node> traitNode : traits.getMembers().entrySet()) {
            ShapeId traitId = traitNode.getKey().expectShapeId();
            // JSON AST model traits are never considered annotation traits, meaning
            // that a null value provided in the AST is not coerced in the same way
            // as an omitted value in the IDL (e.g., "@foo").
            visitor.onTrait(id, traitId, traitNode.getValue());
        }
    }

    private void applyShapeTraits(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        node.getObjectMember(TRAITS).ifPresent(traits -> applyTraits(id, traits, visitor));
    }

    private void loadMember(LoaderVisitor visitor, ShapeId id, ObjectNode targetNode) {
        visitor.checkForAdditionalProperties(targetNode, id, MEMBER_PROPERTIES);
        MemberShape.Builder builder = MemberShape.builder().source(targetNode.getSourceLocation()).id(id);
        ShapeId target = targetNode.expectStringMember(TARGET).expectShapeId();
        builder.target(target);
        applyShapeTraits(id, targetNode, visitor);
        visitor.onShape(builder);
    }

    private void loadCollection(
            ShapeId id,
            ObjectNode node,
            CollectionShape.Builder builder,
            LoaderVisitor visitor
    ) {
        visitor.checkForAdditionalProperties(node, id, COLLECTION_PROPERTY_NAMES);
        applyShapeTraits(id, node, visitor);
        loadMember(visitor, id.withMember("member"), node.expectObjectMember("member"));
        visitor.onShape(builder.id(id).source(node.getSourceLocation()));
    }

    private void loadMap(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, MAP_PROPERTY_NAMES);
        loadMember(visitor, id.withMember("key"), node.expectObjectMember("key"));
        loadMember(visitor, id.withMember("value"), node.expectObjectMember("value"));
        applyShapeTraits(id, node, visitor);
        visitor.onShape(MapShape.builder().id(id).source(node.getSourceLocation()));
    }

    private void loadOperation(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, OPERATION_PROPERTY_NAMES);
        applyShapeTraits(id, node, visitor);
        OperationShape.Builder builder = OperationShape.builder()
                .id(id)
                .source(node.getSourceLocation())
                .addErrors(loadOptionalTargetList(visitor, id, node, "errors"));

        loadOptionalTarget(visitor, id, node, "input").ifPresent(builder::input);
        loadOptionalTarget(visitor, id, node, "output").ifPresent(builder::output);
        visitor.onShape(builder);
    }

    private void loadResource(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, RESOURCE_PROPERTIES);
        applyShapeTraits(id, node, visitor);
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(node.getSourceLocation());
        loadOptionalTarget(visitor, id, node, "put").ifPresent(builder::put);
        loadOptionalTarget(visitor, id, node, "create").ifPresent(builder::create);
        loadOptionalTarget(visitor, id, node, "read").ifPresent(builder::read);
        loadOptionalTarget(visitor, id, node, "update").ifPresent(builder::update);
        loadOptionalTarget(visitor, id, node, "delete").ifPresent(builder::delete);
        loadOptionalTarget(visitor, id, node, "list").ifPresent(builder::list);
        builder.operations(loadOptionalTargetList(visitor, id, node, "operations"));
        builder.collectionOperations(loadOptionalTargetList(visitor, id, node, "collectionOperations"));
        builder.resources(loadOptionalTargetList(visitor, id, node, "resources"));

        // Load identifiers and resolve forward references.
        node.getObjectMember("identifiers").ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                ShapeId target = loadReferenceBody(visitor, id, entry.getValue());
                builder.addIdentifier(name, target);
            }
        });

        visitor.onShape(builder);
    }

    private void loadService(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, SERVICE_PROPERTIES);
        applyShapeTraits(id, node, visitor);
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(node.getSourceLocation());
        builder.version(node.expectStringMember("version").getValue());
        builder.operations(loadOptionalTargetList(visitor, id, node, "operations"));
        builder.resources(loadOptionalTargetList(visitor, id, node, "resources"));
        visitor.onShape(builder);
    }

    private void loadSimpleShape(
            ShapeId id, ObjectNode node, AbstractShapeBuilder builder, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, SIMPLE_PROPERTY_NAMES);
        applyShapeTraits(id, node, visitor);
        visitor.onShape(builder.id(id).source(node.getSourceLocation()));
    }

    private void loadStructure(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, STRUCTURE_AND_UNION_PROPERTY_NAMES);
        visitor.onShape(StructureShape.builder().id(id).source(node.getSourceLocation()));
        finishLoadingStructOrUnionMembers(id, node, visitor);
    }

    private void loadUnion(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        visitor.checkForAdditionalProperties(node, id, STRUCTURE_AND_UNION_PROPERTY_NAMES);
        visitor.onShape(UnionShape.builder().id(id).source(node.getSourceLocation()));
        finishLoadingStructOrUnionMembers(id, node, visitor);
    }

    private void finishLoadingStructOrUnionMembers(ShapeId id, ObjectNode node, LoaderVisitor visitor) {
        applyShapeTraits(id, node, visitor);
        ObjectNode memberObject = node.getObjectMember(MEMBERS).orElse(Node.objectNode());
        for (Map.Entry<String, Node> entry : memberObject.getStringMap().entrySet()) {
            loadMember(visitor, id.withMember(entry.getKey()), entry.getValue().expectObjectNode());
        }
    }

    private Optional<ShapeId> loadOptionalTarget(
            LoaderVisitor visitor, ShapeId id, ObjectNode node, String member) {
        return node.getObjectMember(member).map(r -> loadReferenceBody(visitor, id, r));
    }

    private ShapeId loadReferenceBody(LoaderVisitor visitor, ShapeId id, Node reference) {
        ObjectNode referenceObject = reference.expectObjectNode();
        visitor.checkForAdditionalProperties(referenceObject, id, REFERENCE_PROPERTIES);
        return referenceObject.expectStringMember(TARGET).expectShapeId();
    }

    private List<ShapeId> loadOptionalTargetList(LoaderVisitor visitor, ShapeId id, ObjectNode node, String member) {
        return node.getArrayMember(member).map(array -> {
            List<ShapeId> ids = new ArrayList<>(array.size());
            for (Node element : array.getElements()) {
                ids.add(loadReferenceBody(visitor, id, element));
            }
            return ids;
        }).orElseGet(Collections::emptyList);
    }
}
