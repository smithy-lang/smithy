/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
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

/**
 * Loads a Smithy JSON AST model by driving an {@link AstReader} cursor with recursion, emitting
 * {@link LoadOperation}s.
 */
final class JsonAstLoader {

    private static final String METADATA = "metadata";
    private static final String MEMBERS = "members";
    private static final String SHAPES = "shapes";
    private static final String TRAITS = "traits";
    private static final String TYPE = "type";
    private static final String TARGET = "target";
    private static final String ERRORS = "errors";
    private static final String MIXINS = "mixins";
    private static final String SMITHY = "smithy";

    private static final List<String> TOP_LEVEL_PROPERTIES = list(SMITHY, SHAPES, METADATA);
    private static final List<String> APPLY_PROPERTIES = list(TYPE, TRAITS);
    private static final List<String> SIMPLE_PROPERTY_NAMES = list(TYPE, TRAITS, MIXINS);
    private static final List<String> NAMED_MEMBER_SHAPE_PROPERTY_NAMES = list(TYPE, MEMBERS, TRAITS, MIXINS);
    private static final List<String> COLLECTION_PROPERTY_NAMES = list(TYPE, "member", TRAITS, MIXINS);
    private static final List<String> MAP_PROPERTY_NAMES = list(TYPE, "key", "value", TRAITS, MIXINS);
    private static final List<String> MEMBER_PROPERTIES = list(TARGET, TRAITS);
    private static final List<String> REFERENCE_PROPERTIES = list(TARGET);
    private static final List<String> OPERATION_PROPERTY_NAMES = list(TYPE, "input", "output", ERRORS, TRAITS, MIXINS);
    private static final List<String> RESOURCE_PROPERTIES = list(TYPE,
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
    private static final List<String> SERVICE_PROPERTIES =
            list(TYPE, "version", "operations", "resources", "rename", ERRORS, TRAITS, MIXINS);

    // Use to read out of order version.
    private AstReader reader;
    private final Consumer<LoadOperation> operations;
    private Version modelVersion = Version.UNKNOWN;

    private JsonAstLoader(AstReader reader, Consumer<LoadOperation> operations) {
        this.reader = reader;
        this.operations = operations;
    }

    private static List<String> list(String... values) {
        return java.util.Arrays.asList(values);
    }

    /**
     * Loads a model from the reader, emitting load operations.
     *
     * @return true if a Smithy AST model was loaded; false if it was an unrecognized JSON file.
     */
    static boolean load(AstReader reader, Consumer<LoadOperation> operations) {
        return new JsonAstLoader(reader, operations).parse();
    }

    private boolean parse() {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            return false;
        }
        SourceLocation modelLocation = reader.currentLocation();
        reader.startObject();

        Keys topLevelKeys = new Keys();
        boolean isModel = false;

        // The version drives version-specific shape/trait validation, so it must be applied before any
        // shapes are loaded. The AST always serializes `smithy` first; in the case `shapes` arrives earlier,
        // that one value is captured as a Node and replayed after the version is read.
        Node deferredShapes = null;
        String key;
        while ((key = reader.nextKey()) != null) {
            topLevelKeys.add(key);
            switch (key) {
                case SMITHY: {
                    String versionString = reader.expectStringValue("`smithy` member");
                    Version version = Version.fromString(versionString);
                    if (version == null) {
                        throw new ModelSyntaxException("Unsupported Smithy version number: " + versionString,
                                reader.currentLocation());
                    }
                    modelVersion = version;
                    isModel = true;
                    operations.accept(new LoadOperation.ModelVersion(modelVersion, reader.currentLocation()));
                    break;
                }
                case METADATA:
                    loadMetadata();
                    break;
                case SHAPES:
                    if (modelVersion == Version.UNKNOWN) {
                        // No version? Read shapes into a Node and buffer it. We need the version.
                        deferredShapes = reader.readValueAsNode();
                    } else {
                        loadShapes();
                    }
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        if (!isModel) {
            return false;
        }

        if (deferredShapes != null) {
            loadDeferredShapes(deferredShapes);
        }

        checkAdditionalProperties(topLevelKeys.list, null, TOP_LEVEL_PROPERTIES, modelLocation);
        return true;
    }

    // Loads shapes that were captured as a Node because they appeared before the `smithy` version.
    private void loadDeferredShapes(Node shapes) {
        AstReader streamReader = reader;
        reader = new NodeAstReader(shapes);
        try {
            loadShapes();
        } finally {
            reader = streamReader;
        }
    }

    private void loadMetadata() {
        try {
            if (reader.currentType() != AstReader.Type.OBJECT) {
                throw new SourceException("Expected `metadata` to be an object; found "
                        + AstReader.describe(reader.currentType()), reader.currentLocation());
            }
            reader.startObject();
            String key;
            while ((key = reader.nextKey()) != null) {
                operations.accept(new LoadOperation.PutMetadata(modelVersion, key, reader.readValueAsNode()));
            }
        } catch (SourceException e) {
            emit(ValidationEvent.fromSourceException(e));
        }
    }

    private void loadShapes() {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected `shapes` to be an object; found "
                    + AstReader.describe(reader.currentType()), reader.currentLocation());
        }

        int shapesDepth = reader.depth();
        reader.startObject();
        String idText;
        while ((idText = reader.nextKey()) != null) {
            // Resolved outside the per-shape try, so a bad id/type aborts the file with "-" attribution
            // exactly as the node-based loader did.
            ShapeId id = expectShapeId(idText, reader.lastKeyLocation());
            SourceLocation shapeLocation = reader.currentLocation();
            if (reader.currentType() != AstReader.Type.OBJECT) {
                throw new SourceException("Expected object, but found "
                        + AstReader.describe(reader.currentType()) + ".", shapeLocation);
            }

            try {
                loadShapeDefinition(id, shapeLocation);
            } catch (SourceException e) {
                emit(ValidationEvent.fromSourceException(e).toBuilder().shapeId(id).build());
            }

            // Whether the shape loaded cleanly or threw mid-way, unwind to the shapes map so the
            // cursor is positioned to read the next shape.
            reader.skipToDepth(shapesDepth + 1);
        }
    }

    // Reads one shape definition. The AST serializes `type` as the first member, which lets the common
    // case stream with no buffering. If the keys arrive in another order (valid JSON, accepted by the
    // old node-based loader), the definition is materialized as an ObjectNode and reloaded from it so
    // `type` can be found regardless of position.
    private void loadShapeDefinition(ShapeId id, SourceLocation shapeLocation) {
        reader.startObject();
        String firstKey = reader.nextKey();

        if (firstKey == null) {
            throw new SourceException("Missing expected member `type`.", shapeLocation);
        }

        // Happy case, found type first.
        if (TYPE.equals(firstKey)) {
            String type = reader.expectStringValue("`type` member");
            loadShape(id, type, shapeLocation);
            return;
        }

        // Out-of-order keys: materialize the shape definition as a Node (preserving source locations)
        // and reload it through a NodeAstReader, which finds `type` by random access like the old
        // loader. The opening and first key were already consumed above.
        ObjectNode shapeNode = reader.finishObjectAsNode(shapeLocation, firstKey, reader.lastKeyLocation())
                .expectObjectNode();
        String type = shapeNode.expectStringMember(TYPE).getValue();
        AstReader streamReader = reader;
        reader = new NodeAstReader(shapeNode);
        try {
            // loadShape's member loop iterates every key; `type` lands in the default branch and is
            // skipped (it's an allowed property of every shape kind), so it needn't be pre-consumed.
            reader.startObject();
            loadShape(id, type, shapeLocation);
        } finally {
            reader = streamReader;
        }
    }

    // Resolves a ShapeId, converting a syntax error into a located SourceException (as
    // StringNode#expectShapeId does) so it's caught and attributed like the node-based loader.
    private static ShapeId expectShapeId(String text, SourceLocation location) {
        try {
            return ShapeId.from(text);
        } catch (RuntimeException e) {
            throw new SourceException(e.getMessage(), location);
        }
    }

    private void loadShape(ShapeId id, String type, SourceLocation location) {
        switch (type) {
            case "blob":
                loadSimpleShape(id, location, BlobShape.builder());
                break;
            case "boolean":
                loadSimpleShape(id, location, BooleanShape.builder());
                break;
            case "byte":
                loadSimpleShape(id, location, ByteShape.builder());
                break;
            case "short":
                loadSimpleShape(id, location, ShortShape.builder());
                break;
            case "integer":
                loadSimpleShape(id, location, IntegerShape.builder());
                break;
            case "intEnum":
                loadNamedMemberShape(id, location, IntEnumShape.builder());
                break;
            case "long":
                loadSimpleShape(id, location, LongShape.builder());
                break;
            case "float":
                loadSimpleShape(id, location, FloatShape.builder());
                break;
            case "double":
                loadSimpleShape(id, location, DoubleShape.builder());
                break;
            case "document":
                loadSimpleShape(id, location, DocumentShape.builder());
                break;
            case "bigDecimal":
                loadSimpleShape(id, location, BigDecimalShape.builder());
                break;
            case "bigInteger":
                loadSimpleShape(id, location, BigIntegerShape.builder());
                break;
            case "string":
                loadSimpleShape(id, location, StringShape.builder());
                break;
            case "enum":
                loadNamedMemberShape(id, location, EnumShape.builder());
                break;
            case "timestamp":
                loadSimpleShape(id, location, TimestampShape.builder());
                break;
            case "list":
                loadCollection(id, location, ListShape.builder());
                break;
            case "set":
                loadCollection(id, location, SetShape.builder());
                break;
            case "map":
                loadMap(id, location);
                break;
            case "resource":
                loadResource(id, location);
                break;
            case "service":
                loadService(id, location);
                break;
            case "structure":
                loadNamedMemberShape(id, location, StructureShape.builder());
                break;
            case "union":
                loadNamedMemberShape(id, location, UnionShape.builder());
                break;
            case "operation":
                loadOperation(id, location);
                break;
            case "apply":
                loadApply(id, location);
                break;
            default:
                throw new SourceException("Invalid shape `type`: " + type, location);
        }
    }

    // ===== Shape kinds. Each consumes the remaining members of the (already type-read) shape object. =====

    private void loadApply(ShapeId id, SourceLocation location) {
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            if (TRAITS.equals(key)) {
                applyTraitsFromCursor(id);
            } else {
                reader.skipValue();
            }
        }
        checkAdditionalProperties(keys.list, id, APPLY_PROPERTIES, location);
    }

    private void loadSimpleShape(ShapeId id, SourceLocation location, AbstractShapeBuilder<?, ?> builder) {
        builder.id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, SIMPLE_PROPERTY_NAMES, location);
        operations.accept(operation);
    }

    private void loadNamedMemberShape(ShapeId id, SourceLocation location, AbstractShapeBuilder<?, ?> builder) {
        builder.id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                case MEMBERS:
                    loadNamedMembers(operation, id);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, NAMED_MEMBER_SHAPE_PROPERTY_NAMES, location);
        operations.accept(operation);
    }

    private void loadCollection(ShapeId id, SourceLocation location, CollectionShape.Builder<?, ?> builder) {
        builder.id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                case "member":
                    loadMember(operation, id.withMember("member"));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, COLLECTION_PROPERTY_NAMES, location);
        operations.accept(operation);
    }

    private void loadMap(ShapeId id, SourceLocation location) {
        MapShape.Builder builder = MapShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                case "key":
                    loadMember(operation, id.withMember("key"));
                    break;
                case "value":
                    loadMember(operation, id.withMember("value"));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, MAP_PROPERTY_NAMES, location);
        operations.accept(operation);
    }

    private void loadOperation(ShapeId id, SourceLocation location) {
        OperationShape.Builder builder = OperationShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                case "input":
                    builder.input(readReference(id, "input"));
                    break;
                case "output":
                    builder.output(readReference(id, "output"));
                    break;
                case ERRORS:
                    builder.addErrors(readReferenceList(id));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, OPERATION_PROPERTY_NAMES, location);
        operations.accept(operation);
    }

    private void loadResource(ShapeId id, SourceLocation location) {
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                case "put":
                    builder.put(readReference(id, "put"));
                    break;
                case "create":
                    builder.create(readReference(id, "create"));
                    break;
                case "read":
                    builder.read(readReference(id, "read"));
                    break;
                case "update":
                    builder.update(readReference(id, "update"));
                    break;
                case "delete":
                    builder.delete(readReference(id, "delete"));
                    break;
                case "list":
                    builder.list(readReference(id, "list"));
                    break;
                case "operations":
                    builder.operations(readReferenceList(id));
                    break;
                case "collectionOperations":
                    builder.collectionOperations(readReferenceList(id));
                    break;
                case "resources":
                    builder.resources(readReferenceList(id));
                    break;
                case "identifiers":
                    readIdentifiers(builder, id);
                    break;
                case "properties":
                    readProperties(builder, id);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, RESOURCE_PROPERTIES, location);
        operations.accept(operation);
    }

    private void loadService(ShapeId id, SourceLocation location) {
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        Keys keys = new Keys(TYPE);
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TRAITS:
                    applyTraitsFromCursor(id);
                    break;
                case MIXINS:
                    addMixins(operation);
                    break;
                case "version":
                    builder.version(reader.expectStringValue("`version`"));
                    break;
                case "operations":
                    builder.operations(readReferenceList(id));
                    break;
                case "resources":
                    builder.resources(readReferenceList(id));
                    break;
                case ERRORS:
                    builder.addErrors(readReferenceList(id));
                    break;
                case "rename":
                    readServiceRename(builder);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, id, SERVICE_PROPERTIES, location);
        operations.accept(operation);
    }

    // Applies the traits in the object the cursor is positioned on (the value of a "traits" key).
    private void applyTraitsFromCursor(ShapeId id) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected `traits` to be an object, but found "
                    + AstReader.describe(reader.currentType()), reader.currentLocation());
        }
        reader.startObject();
        String traitName;
        while ((traitName = reader.nextKey()) != null) {
            SourceLocation traitLocation = reader.lastKeyLocation();
            // JSON AST model traits are never considered annotation traits.
            ShapeId traitId = expectShapeId(traitName, traitLocation);
            Node value = reader.readValueAsNode();
            operations.accept(new LoadOperation.ApplyTrait(
                    modelVersion,
                    traitLocation,
                    id.getNamespace(),
                    id,
                    traitId,
                    value));
        }
    }

    private void loadNamedMembers(LoadOperation.DefineShape operation, ShapeId shapeId) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected `members` to be an object, but found "
                    + AstReader.describe(reader.currentType()), reader.currentLocation());
        }
        reader.startObject();
        String memberName;
        while ((memberName = reader.nextKey()) != null) {
            loadMember(operation, withMember(shapeId, memberName, reader.lastKeyLocation()));
        }
    }

    private static ShapeId withMember(ShapeId shapeId, String memberName, SourceLocation location) {
        try {
            return shapeId.withMember(memberName);
        } catch (RuntimeException e) {
            throw new SourceException(e.getMessage(), location);
        }
    }

    // Reads the member object the cursor is positioned on into a MemberShape.Builder.
    private void loadMember(LoadOperation.DefineShape operation, ShapeId memberId) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected object, but found " + AstReader.describe(reader.currentType()) + ".",
                    reader.currentLocation());
        }
        SourceLocation memberLocation = reader.currentLocation();
        MemberShape.Builder builder = MemberShape.builder().source(memberLocation).id(memberId);
        Keys keys = new Keys();
        ShapeId target = null;
        boolean sawTarget = false;
        reader.startObject();
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            switch (key) {
                case TARGET:
                    target = expectShapeId(reader.expectStringValue("`target` member"), reader.currentLocation());
                    sawTarget = true;
                    break;
                case TRAITS:
                    applyTraitsFromCursor(memberId);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        checkAdditionalProperties(keys.list, memberId, MEMBER_PROPERTIES, memberLocation);
        if (!sawTarget) {
            throw new SourceException("Missing expected member `target`.", memberLocation);
        }
        builder.target(target);
        operation.addMember(builder);
    }

    private void addMixins(LoadOperation.DefineShape operation) {
        if (reader.currentType() != AstReader.Type.ARRAY) {
            throw new SourceException("Expected an array, but found " + AstReader.describe(reader.currentType()) + ".",
                    reader.currentLocation());
        }
        reader.startArray();
        while (reader.nextElement()) {
            ShapeId mixinId = readReferenceBody(operation.toShapeId());
            operation.addDependency(mixinId);
            operation.addModifier(new ApplyMixin(mixinId));
        }
    }

    // Reads a single shape reference object {"target": "..."} the cursor is positioned on.
    private ShapeId readReference(ShapeId id, String member) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected `" + member + "` to be an object; found "
                    + AstReader.describe(reader.currentType()), reader.currentLocation());
        }
        return readReferenceBody(id);
    }

    private ShapeId readReferenceBody(ShapeId id) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected object, but found " + AstReader.describe(reader.currentType()) + ".",
                    reader.currentLocation());
        }
        SourceLocation location = reader.currentLocation();
        Keys keys = new Keys();
        ShapeId target = null;
        boolean sawTarget = false;
        reader.startObject();
        String key;
        while ((key = reader.nextKey()) != null) {
            keys.add(key);
            if (TARGET.equals(key)) {
                target = expectShapeId(reader.expectStringValue("`target` member"), reader.currentLocation());
                sawTarget = true;
            } else {
                reader.skipValue();
            }
        }
        checkAdditionalProperties(keys.list, id, REFERENCE_PROPERTIES, location);
        if (!sawTarget) {
            throw new SourceException("Missing expected member `target`.", location);
        }
        return target;
    }

    private List<ShapeId> readReferenceList(ShapeId id) {
        if (reader.currentType() != AstReader.Type.ARRAY) {
            throw new SourceException("Expected an array, but found " + AstReader.describe(reader.currentType()) + ".",
                    reader.currentLocation());
        }
        List<ShapeId> ids = new ArrayList<>();
        reader.startArray();
        while (reader.nextElement()) {
            ids.add(readReferenceBody(id));
        }
        return ids;
    }

    private void readIdentifiers(ResourceShape.Builder builder, ShapeId id) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected an object, but found " + AstReader.describe(reader.currentType()) + ".",
                    reader.currentLocation());
        }
        reader.startObject();
        String name;
        while ((name = reader.nextKey()) != null) {
            builder.addIdentifier(name, readReferenceBody(id));
        }
    }

    private void readProperties(ResourceShape.Builder builder, ShapeId id) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected an object, but found " + AstReader.describe(reader.currentType()) + ".",
                    reader.currentLocation());
        }
        SourceLocation propertiesLocation = reader.currentLocation();
        if (!modelVersion.supportsResourceProperties()) {
            emit(ValidationEvent.builder()
                    .sourceLocation(propertiesLocation)
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .message("Resource properties can only be used with Smithy version 2 or later. "
                            + "Attempted to use resource properties with version `" + modelVersion + "`.")
                    .build());
        }
        reader.startObject();
        String name;
        while ((name = reader.nextKey()) != null) {
            builder.addProperty(name, readReferenceBody(id));
        }
    }

    private void readServiceRename(ServiceShape.Builder builder) {
        if (reader.currentType() != AstReader.Type.OBJECT) {
            throw new SourceException("Expected `rename` to be an object, but found "
                    + AstReader.describe(reader.currentType()), reader.currentLocation());
        }
        reader.startObject();
        String fromText;
        while ((fromText = reader.nextKey()) != null) {
            ShapeId fromId = expectShapeId(fromText, reader.lastKeyLocation());
            String toName = reader.expectStringValue("rename value");
            builder.putRename(fromId, toName);
        }
    }

    private LoadOperation.DefineShape createShape(AbstractShapeBuilder<?, ?> builder) {
        return new LoadOperation.DefineShape(modelVersion, builder);
    }

    private void emit(ValidationEvent event) {
        operations.accept(new LoadOperation.Event(event));
    }

    private void checkAdditionalProperties(
            List<String> keys,
            ShapeId shape,
            Collection<String> allowed,
            SourceLocation location
    ) {
        List<String> additional = null;
        for (String key : keys) {
            if (!allowed.contains(key)) {
                if (additional == null) {
                    additional = new ArrayList<>();
                }
                additional.add(key);
            }
        }
        if (additional != null) {
            String message = String.format(
                    "Expected an object with possible properties of %s, but found additional properties: %s",
                    software.amazon.smithy.model.validation.ValidationUtils.tickedList(allowed),
                    software.amazon.smithy.model.validation.ValidationUtils.tickedList(additional));
            emit(ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.WARNING)
                    .message(message)
                    .sourceLocation(location)
                    .shapeId(shape)
                    .build());
        }
    }

    // Accumulates the member keys seen in an object for the additional-properties check.
    private static final class Keys {
        final List<String> list = new ArrayList<>();

        Keys(String... initial) {
            Collections.addAll(list, initial);
        }

        void add(String key) {
            list.add(key);
        }
    }
}
