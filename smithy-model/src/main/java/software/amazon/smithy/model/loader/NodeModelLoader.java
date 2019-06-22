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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.DefaultNodeFactory;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeFactory;
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
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
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
 * Adds shapes and definitions from a JSON file to a loader visitor.
 */
final class NodeModelLoader implements ModelLoader {
    private static final String SMITHY = "smithy";
    private static final String METADATA = "metadata";
    private static final String SHAPES = "shapes";
    private static final String TRAITS = "traits";
    private static final String TRAIT_DEFS = "traitDefs";
    private static final List<String> NAMESPACE_PROPERTIES = ListUtils.of("shapes", "traits", "traitDefs");
    private static final List<String> COLLECTION_PROPERTY_NAMES = ListUtils.of("type", "member");
    private static final List<String> MAP_PROPERTY_NAMES = ListUtils.of("type", "key", "value");
    private static final Set<String> MEMBER_PROPERTIES = SetUtils.of("target");
    private static final List<String> OPERATION_PROPERTY_NAMES = ListUtils.of("type", "input", "output", "errors");
    private static final List<String> SIMPLE_PROPERTY_NAMES = ListUtils.of("type");
    private static final List<String> STRUCTURE_PROPERTY_NAMES = ListUtils.of("type", "members");
    private static final List<String> UNION_PROPERTY_NAMES = ListUtils.of("type", "members");
    private static final Set<String> RESERVED_STRUCTURE_WORDS = SetUtils.of("isa");

    private final NodeFactory nodeFactory;

    NodeModelLoader() {
        this(new DefaultNodeFactory());
    }

    NodeModelLoader(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    @Override
    public boolean load(String filename, Supplier<String> contentSupplier, LoaderVisitor visitor) {
        if (test(filename, contentSupplier)) {
            Node node = nodeFactory.createNode(filename, contentSupplier.get());
            visitor.onOpenFile(filename);
            load(visitor, node);
            return true;
        }

        return false;
    }

    private boolean test(String path, Supplier<String> contentSupplier) {
        if (path.endsWith(".json")) {
            return true;
        }

        // Loads the contents of a file if the file has a source location
        // of "N/A", isn't empty, and the first character is "{".
        String contents = contentSupplier.get();
        return path.equals(SourceLocation.NONE.getFilename())
               && !contents.isEmpty()
               && contents.charAt(0) == '{';
    }

    /**
     * Loads a shape from the JSON definition of a shape into the given loader visitor.
     *
     * <p>This method is public because other JSON based formats may want to use it
     * to load shapes with different serialization formats that are similar to JSON.
     *
     * @param id The shape ID to load. The current namespace of traits is assumed to be the same as the shape ID.
     * @param type The type of shape to load.
     * @param value The shape's value to load.
     * @param visitor The visitor to update while loading the shape.
     */
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
            default:
                throw new SourceException("Invalid shape `type`: " + type, value);
        }
    }

    void load(LoaderVisitor visitor, Node node) {
        ObjectNode model = node.expectObjectNode("Smithy documents must be an object. Found {type}.");
        StringNode version = model.expectMember(SMITHY).expectStringNode();
        visitor.onVersion(version.getSourceLocation(), version.expectStringNode().getValue());

        model.getMember(METADATA).ifPresent(value -> {
            ObjectNode metadata = value.expectObjectNode("`metadata` must be an object");
            metadata.getMembers().forEach((k, v) -> visitor.onMetadata(k.getValue(), v));
        });

        model.getMembers().forEach((key, value) -> {
            String keyValue = key.getValue();
            if (!keyValue.equals(SMITHY) && !keyValue.equals(METADATA)) {
                // Additional properties are considered namespaces.
                visitor.onNamespace(key.getValue(), key);
                loadNamespace(visitor, key.getValue(), value.expectObjectNode());
            }
        });
    }

    private void loadNamespace(LoaderVisitor visitor, String namespace, ObjectNode node) {
        ObjectNode members = node.expectObjectNode("Each namespace must be an object. Found {type}.");
        members.warnIfAdditionalProperties(NAMESPACE_PROPERTIES);
        node.getObjectMember(SHAPES).ifPresent(shapes -> loadShapes(visitor, namespace, shapes));
        node.getObjectMember(TRAITS).ifPresent(traits -> loadTraits(visitor, namespace, traits));
        node.getObjectMember(TRAIT_DEFS).ifPresent(traitDefs -> loadTraitDefs(visitor, namespace, traitDefs));
    }

    private void loadShapes(LoaderVisitor visitor, String namespace, ObjectNode members) {
        members.getMembers().forEach((k, v) -> {
            try {
                ShapeId shapeId = ShapeId.fromRelative(namespace, k.getValue());
                ObjectNode shape = v.expectObjectNode(
                        "Each value in the `shapes` property must be an object; found {type}.");
                loadShape(shapeId, shape, visitor);
            } catch (ShapeIdSyntaxException e) {
                visitor.onError(invalidShapeId("shapes", e.getMessage(), k.getSourceLocation()));
            } catch (SourceException e) {
                visitor.onError(ValidationEvent.fromSourceException(e));
            }
        });
    }

    private void loadTraits(LoaderVisitor visitor, String namespace, ObjectNode members) {
        members.getMembers().forEach((k, v) -> {
            try {
                ShapeId shapeId = ShapeId.fromRelative(namespace, k.getValue());
                ObjectNode traitDefinitions = v.expectObjectNode(
                        "Each value in the inner object of `traits` must be an object; found {type}.");
                for (Map.Entry<StringNode, Node> traitNode : traitDefinitions.getMembers().entrySet()) {
                    visitor.onTrait(shapeId, traitNode.getKey().getValue(), traitNode.getValue());
                }
            } catch (ShapeIdSyntaxException e) {
                visitor.onError(invalidShapeId("traits", e.getMessage(), k.getSourceLocation()));
            }
        });
    }

    private ValidationEvent invalidShapeId(String descriptor, String message, SourceLocation sourceLocation) {
        return ValidationEvent.builder()
                .eventId(Validator.MODEL_ERROR)
                .sourceLocation(sourceLocation)
                .severity(Severity.ERROR)
                .message("Each key in the `" + descriptor + "` object must be a valid relative shape ID; " + message)
                .build();
    }

    private void loadTraitDefs(LoaderVisitor visitor, String namespace, ObjectNode members) {
        for (Map.Entry<StringNode, Node> entry : members.getMembers().entrySet()) {
            try {
                LoaderUtils.loadTraitDefinition(namespace, entry.getKey().getValue(), entry.getValue(), visitor, null);
            } catch (SourceException e) {
                visitor.onError(ValidationEvent.fromSourceException(e));
            }
        }
    }

    private void loadShape(ShapeId id, ObjectNode value, LoaderVisitor visitor) throws SourceException {
        String type = value.expectMember("type", "Missing `type` in shape definition")
                .expectStringNode("Shape `type` must be a string. Found {type}.")
                .getValue();
        try {
            loadShape(id, type, value, visitor);
        } catch (SourceException e) {
            ValidationEvent event = ValidationEvent.fromSourceException(e).toBuilder().shapeId(id).build();
            visitor.onError(event);
        }
    }

    private void loadMember(LoaderVisitor visitor, ShapeId shapeId, Node node) {
        MemberShape.Builder builder = MemberShape.builder().source(node.getSourceLocation()).id(shapeId);
        ObjectNode targetNode = node.expectObjectNode("Expected member to be an object; found {type}");
        String target = targetNode
                .expectMember("target", "Missing required member property `target`.")
                .expectStringNode("Expected `target` property of member to be a string; found {type}.")
                .getValue();
        visitor.onShapeTarget(target, builder::target);
        extractTraits(shapeId, targetNode, MEMBER_PROPERTIES, visitor);
        visitor.onShape(builder);
    }

    private void extractTraits(
            ShapeId shapeId,
            ObjectNode shapeNode,
            Collection<String> propertyNames,
            LoaderVisitor visitor
    ) {
        // Extracts traits from the node and adds them to the visitor.
        for (Map.Entry<StringNode, Node> entry : shapeNode.getMembers().entrySet()) {
            String traitName = entry.getKey().getValue();
            if (!propertyNames.contains(traitName)) {
                visitor.onTrait(shapeId, traitName, entry.getValue());
            }
        }
    }

    private void loadCollection(
            ShapeId shapeId,
            ObjectNode shapeNode,
            CollectionShape.Builder builder,
            LoaderVisitor visitor
    ) {
        extractTraits(shapeId, shapeNode, COLLECTION_PROPERTY_NAMES, visitor);
        loadMember(visitor, shapeId.withMember("member"), shapeNode.expectMember(
                "member", "Shape is missing required property `member`."));
        visitor.onShape(builder.id(shapeId).source(shapeNode.getSourceLocation()));
    }

    private void loadMap(ShapeId shapeId, ObjectNode shapeNode, LoaderVisitor visitor) {
        loadMember(visitor, shapeId.withMember("key"), shapeNode.expectMember(
                "key", "Missing required map property `key`."));
        loadMember(visitor, shapeId.withMember("value"), shapeNode.expectMember(
                "value", "Missing required map property `value`."));
        extractTraits(shapeId, shapeNode, MAP_PROPERTY_NAMES, visitor);
        visitor.onShape(MapShape.builder().id(shapeId).source(shapeNode.getSourceLocation()));
    }

    private void loadOperation(ShapeId operationShapeId, ObjectNode node, LoaderVisitor visitor) {
        String namespace = operationShapeId.getNamespace();
        extractTraits(operationShapeId, node, OPERATION_PROPERTY_NAMES, visitor);
        OperationShape.Builder builder = OperationShape.builder()
                .id(operationShapeId)
                .source(node.getSourceLocation())
                .addErrors(LoaderUtils.optionalIdList(node, namespace, "errors"));
        LoaderUtils.optionalId(node, namespace, "input").ifPresent(builder::input);
        LoaderUtils.optionalId(node, namespace, "output").ifPresent(builder::output);
        visitor.onShape(builder);
    }

    private void loadResource(ShapeId shapeId, ObjectNode shapeNode, LoaderVisitor visitor) {
        extractTraits(shapeId, shapeNode, LoaderUtils.RESOURCE_PROPERTY_NAMES, visitor);
        ResourceShape.Builder builder = ResourceShape.builder().id(shapeId).source(shapeNode.getSourceLocation());
        LoaderUtils.loadResourceObject(builder, shapeId, shapeNode, visitor);
        visitor.onShape(builder);
    }

    private void loadService(ShapeId shapeId, ObjectNode shapeNode, LoaderVisitor visitor) {
        extractTraits(shapeId, shapeNode, LoaderUtils.SERVICE_PROPERTY_NAMES, visitor);
        ServiceShape.Builder builder = new ServiceShape.Builder()
                .id(shapeId)
                .source(shapeNode.getSourceLocation());
        LoaderUtils.loadServiceObject(builder, shapeId, shapeNode);
        visitor.onShape(builder);
    }

    /**
     * Loads simple shapes using a supplier for the builder
     * (string, integer, float, etc.).
     */
    private void loadSimpleShape(
            ShapeId shapeId,
            ObjectNode shapeNode,
            AbstractShapeBuilder builder,
            LoaderVisitor visitor
    ) {
        extractTraits(shapeId, shapeNode, SIMPLE_PROPERTY_NAMES, visitor);
        visitor.onShape(builder.id(shapeId).source(shapeNode.getSourceLocation()));
    }

    private void loadStructure(ShapeId shapeId, ObjectNode shapeNode, LoaderVisitor visitor) {
        ObjectNode memberObject = shapeNode
                .getMember("members")
                .orElse(Node.objectNode())
                .expectObjectNode("Expected structure `members` to be an object; found {type}.");

        // Some properties are reserved for potential use in the future.
        for (StringNode property : shapeNode.getMembers().keySet()) {
            if (RESERVED_STRUCTURE_WORDS.contains(property.getValue())) {
                visitor.onError(ValidationEvent.builder()
                        .eventId(Validator.MODEL_ERROR)
                        .severity(Severity.ERROR)
                        .sourceLocation(property.getSourceLocation())
                        .message(String.format("`%s` is a reserved word for a structure shape", property.getValue()))
                        .build());
            }
        }

        extractTraits(shapeId, shapeNode, STRUCTURE_PROPERTY_NAMES, visitor);
        visitor.onShape(StructureShape.builder().id(shapeId).source(shapeNode.getSourceLocation()));
        memberObject.getMembers().forEach((k, v) -> {
            loadMember(visitor, shapeId.withMember(k.getValue()), v);
        });
    }

    private void loadUnion(ShapeId shapeId, ObjectNode shapeNode, LoaderVisitor visitor) {
        ObjectNode membersNode = shapeNode
                .expectMember("members", "Missing required union property `members`.")
                .expectObjectNode("Expected union `members` to be an `object`, got `{type}`.");
        extractTraits(shapeId, shapeNode, UNION_PROPERTY_NAMES, visitor);
        visitor.onShape(UnionShape.builder().id(shapeId).source(shapeNode.getSourceLocation()));
        membersNode.getMembers().forEach((k, v) -> {
            loadMember(visitor, shapeId.withMember(k.getValue()), v);
        });
    }
}
