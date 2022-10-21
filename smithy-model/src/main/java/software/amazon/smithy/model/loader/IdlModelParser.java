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

import static java.lang.String.format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
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
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.StringUtils;

final class IdlModelParser extends SimpleParser {

    /** Only allow nesting up to 250 arrays/objects in node values. */
    private static final int MAX_NESTING_LEVEL = 250;

    private static final String PUT_KEY = "put";
    private static final String CREATE_KEY = "create";
    private static final String READ_KEY = "read";
    private static final String UPDATE_KEY = "update";
    private static final String DELETE_KEY = "delete";
    private static final String LIST_KEY = "list";
    private static final String RESOURCES_KEY = "resources";
    private static final String OPERATIONS_KEY = "operations";
    private static final String PROPERTIES_KEY = "properties";
    private static final String RENAME_KEY = "rename";
    private static final String COLLECTION_OPERATIONS_KEY = "collectionOperations";
    private static final String IDENTIFIERS_KEY = "identifiers";
    private static final String VERSION_KEY = "version";
    private static final String TYPE_KEY = "type";
    private static final String ERRORS_KEY = "errors";

    static final Collection<String> RESOURCE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, CREATE_KEY, READ_KEY, UPDATE_KEY, DELETE_KEY, LIST_KEY,
            IDENTIFIERS_KEY, RESOURCES_KEY, OPERATIONS_KEY, PUT_KEY, PROPERTIES_KEY, COLLECTION_OPERATIONS_KEY);
    static final List<String> SERVICE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, VERSION_KEY, OPERATIONS_KEY, RESOURCES_KEY, RENAME_KEY, ERRORS_KEY);
    private static final Set<String> SHAPE_TYPES = new HashSet<>();

    static {
        for (ShapeType type : ShapeType.values()) {
            if (type != ShapeType.MEMBER) {
                SHAPE_TYPES.add(type.toString());
            }
        }
    }

    private final String filename;
    private final Map<String, ShapeId> useShapes = new HashMap<>();
    private Consumer<LoadOperation> operations;
    private Version modelVersion = Version.VERSION_1_0;
    private String namespace;
    private TraitEntry pendingDocumentationComment;
    private boolean emittedVersion = false;

    private String operationInputSuffix = "Input";
    private String operationOutputSuffix = "Output";

    // A pending trait that also doesn't yet have a resolved trait shape ID.
    static final class TraitEntry {
        final String traitName;
        final Node value;
        final boolean isAnnotation;

        TraitEntry(String traitName, Node value, boolean isAnnotation) {
            this.traitName = traitName;
            this.value = value;
            this.isAnnotation = isAnnotation;
        }
    }

    IdlModelParser(String filename, String model) {
        super(model, MAX_NESTING_LEVEL);
        this.filename = filename;
    }

    void parse(Consumer<LoadOperation> operationConsumer) {
        operations = operationConsumer;
        ws();
        parseControlSection();

        // Emit a version from the current location if the assumed 1.0 is used.
        if (!emittedVersion) {
            operations.accept(new LoadOperation.ModelVersion(modelVersion, currentLocation()));
        }

        parseMetadataSection();
        parseShapeSection();
    }

    LoadOperation.DefineShape createShape(AbstractShapeBuilder<?, ?> builder) {
        return new LoadOperation.DefineShape(modelVersion, builder);
    }

    void addOperation(LoadOperation operation) {
        operations.accept(operation);
    }

    void emit(ValidationEvent event) {
        addOperation(new LoadOperation.Event(event));
    }

    void addForwardReference(String id, BiConsumer<ShapeId, Function<ShapeId, ShapeType>> consumer) {
        int memberPosition = id.indexOf('$');

        // Check for members by removing the member and checking for the root shape.
        if (memberPosition > 0 && memberPosition < id.length() - 1) {
            addForwardReference(id.substring(0, memberPosition), (resolved, type) -> {
                consumer.accept(resolved.withMember(id.substring(memberPosition + 1)), type);
            });
        } else {
            String resolved = useShapes.containsKey(id) ? useShapes.get(id).toString() : id;
            addOperation(new LoadOperation.ForwardReference(namespace, resolved, consumer));
        }
    }

    void addForwardReference(String id, Consumer<ShapeId> consumer) {
        addForwardReference(id, (resolved, found) -> consumer.accept(resolved));
    }

    String expectNamespace() {
        if (namespace == null) {
            throw new IllegalStateException("No namespace was set before trying to resolve a forward reference");
        }
        return namespace;
    }

    /**
     * Overrides whitespace parsing to handle comments.
     */
    @Override
    public void ws() {
        while (!eof()) {
            switch (peek()) {
                case '/':
                    if (peekDocComment()) {
                        parseDocComment();
                    } else {
                        parseComment();
                    }
                    break;
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                case ',':
                    skip();
                    break;
                default:
                     return;
            }
        }
    }

    // required space
    private void rsp() {
        int cc = column();
        sp();
        if (column() == cc) {
            throw syntax("Expected one or more spaces");
        }
    }

    // Required whitespace.
    private void rws() {
        int line = line();
        int column = column();
        ws();
        if (line() == line && column == column()) {
            throw syntax("Expected one or more whitespace characters");
        }
    }

    @Override
    public void sp() {
        while (isSpaceOrComma(peek())) {
            skip();
        }
    }

    private boolean isSpaceOrComma(char c) {
        return c == ' ' || c == '\t' || c == ',';
    }

    @Override
    public void br() {
        int line = line();
        ws();
        if (line == line() && !eof()) {
            throw syntax("Expected a line break");
        }
    }

    @Override
    public ModelSyntaxException syntax(String message) {
        return syntax(null, message);
    }

    ModelSyntaxException syntax(ShapeId shapeId, String message) {
        return ModelSyntaxException.builder()
                .message(format("Parse error at line %d, column %d near `%s`: %s",
                                line(), column(), peekDebugMessage(), message))
                .sourceLocation(filename, line(), column())
                .shapeId(shapeId)
                .build();
    }

    private void parseControlSection() {
        Set<String> definedKeys = new HashSet<>();
        while (peek() == '$') {
            expect('$');
            String key = IdlNodeParser.parseNodeObjectKey(this);
            sp();
            expect(':');
            sp();

            if (definedKeys.contains(key)) {
                throw syntax(format("Duplicate control statement `%s`", key));
            }
            definedKeys.add(key);

            Node value = IdlNodeParser.parseNode(this);

            switch (key) {
                case "version":
                    onVersion(value);
                    break;
                case "operationInputSuffix":
                    operationInputSuffix = value.expectStringNode().getValue();
                    break;
                case "operationOutputSuffix":
                    operationOutputSuffix = value.expectStringNode().getValue();
                    break;
                default:
                    emit(ValidationEvent.builder()
                                 .id(Validator.MODEL_ERROR)
                                 .sourceLocation(value)
                                 .severity(Severity.WARNING)
                                 .message(format("Unknown control statement `%s` with value `%s",
                                                 key, Node.printJson(value)))
                                 .build());
                    break;
            }

            br();
        }
    }

    private void onVersion(Node value) {
        if (!value.isStringNode()) {
            value.expectStringNode(() -> "The $version control statement must have a string value, but found "
                                         + Node.printJson(value));
        }

        String parsedVersion = value.expectStringNode().getValue();
        Version resolvedVersion = Version.fromString(parsedVersion);

        if (resolvedVersion == null) {
            throw syntax("Unsupported Smithy version number: " + parsedVersion);
        }

        emittedVersion = true;
        modelVersion = resolvedVersion;
        operations.accept(new LoadOperation.ModelVersion(modelVersion, value.getSourceLocation()));
    }

    private void parseMetadataSection() {
        while (peek() == 'm') {
            expect('m');
            expect('e');
            expect('t');
            expect('a');
            expect('d');
            expect('a');
            expect('t');
            expect('a');
            rsp();
            String key = IdlNodeParser.parseNodeObjectKey(this);
            sp();
            expect('=');
            sp();
            operations.accept(new LoadOperation.PutMetadata(modelVersion, key, IdlNodeParser.parseNode(this)));
            br();
        }
    }

    private void parseShapeSection() {
        if (peek() == 'n') {
            expect('n');
            expect('a');
            expect('m');
            expect('e');
            expect('s');
            expect('p');
            expect('a');
            expect('c');
            expect('e');
            rsp();

            // Parse the namespace.
            int start = position();
            ParserUtils.consumeNamespace(this);
            namespace = sliceFrom(start);
            // Clear out any erroneous documentation comments.
            clearPendingDocs();
            br();

            parseUseSection();
            parseShapeStatements();
        } else if (!eof()) {
            if (!ParserUtils.isIdentifierStart(peek())) {
                throw syntax("Expected a namespace definition, but found unexpected syntax");
            } else {
                throw syntax("A namespace must be defined before a use statement or shapes");
            }
        }
    }

    private void parseUseSection() {
        while (peek() == 'u' && peek(1) == 's') {
            expect('u');
            expect('s');
            expect('e');
            rsp();

            int start = position();
            SourceLocation location = currentLocation();
            ParserUtils.consumeNamespace(this);
            expect('#');
            ParserUtils.consumeIdentifier(this);
            String lexeme = sliceFrom(start);
            // Clear out any erroneous documentation comments.
            clearPendingDocs();
            br();

            ShapeId target = ShapeId.from(lexeme);

            // Validate use statements when the model is fully loaded.
            addForwardReference(lexeme, (resolved, typeProvider) -> {
                if (typeProvider.apply(resolved) == null) {
                    ValidationEvent event = ValidationEvent.builder()
                            .id(Validator.MODEL_ERROR)
                            .severity(Severity.WARNING)
                            .sourceLocation(location)
                            .message("Use statement refers to undefined shape: " + lexeme)
                            .build();
                    emit(event);
                }
            });

            useShape(target, location);
        }
    }

    void useShape(ShapeId id, SourceLocation location) {
        if (useShapes.containsKey(id.getName())) {
            ShapeId previous = useShapes.get(id.getName());
            String message = String.format("Cannot use name `%s` because it conflicts with `%s`", id, previous);
            throw new ModelSyntaxException(message, location);
        }

        useShapes.put(id.getName(), id);
    }

    private void parseShapeStatements() {
        while (!eof()) {
            if (peek() == 'a') {
                parseApplyStatement();
            } else {
                boolean docsOnly = pendingDocumentationComment != null;
                List<TraitEntry> traits = parseDocsAndTraits();
                if (parseShapeDefinition(traits, docsOnly)) {
                    parseShape(traits);
                }
            }
        }
    }

    private void clearPendingDocs() {
        pendingDocumentationComment = null;
    }

    private boolean parseShapeDefinition(List<TraitEntry> traits, boolean docsOnly) {
        if (eof()) {
            return !traits.isEmpty() && !docsOnly;
        } else {
            return true;
        }
    }

    private List<TraitEntry> parseDocsAndTraits() {
        // Grab the pending docs, if present, and clear its state.
        TraitEntry docComment = pendingDocumentationComment;
        clearPendingDocs();

        // Parse traits, if any.
        ws();
        List<TraitEntry> traits = IdlTraitParser.parseTraits(this);
        if (docComment != null) {
            traits.add(docComment);
        }
        ws();

        return traits;
    }

    private void parseShape(List<TraitEntry> traits) {
        SourceLocation location = currentLocation();

        // Do a check here to give better parsing error messages.
        String shapeType = ParserUtils.parseIdentifier(this);
        if (!SHAPE_TYPES.contains(shapeType)) {
            switch (shapeType) {
                case "use":
                    throw syntax("A use statement must come before any shape definition");
                case "namespace":
                    throw syntax("Only a single namespace can be declared per/file");
                case "metadata":
                    throw syntax("Metadata statements must appear before a namespace statement");
                default:
                    throw syntax("Unexpected shape type: " + shapeType);
            }
        }

        rsp();
        ShapeId id = parseShapeName();

        switch (shapeType) {
            case "service":
                parseServiceStatement(id, location);
                break;
            case "resource":
                parseResourceStatement(id, location);
                break;
            case "operation":
                parseOperationStatement(id, location);
                break;
            case "structure":
                parseStructuredShape(id, location, StructureShape.builder(), MemberParsing.PARSING_STRUCTURE_MEMBER);
                break;
            case "union":
                parseStructuredShape(id, location, UnionShape.builder(), MemberParsing.PARSING_MEMBER);
                break;
            case "list":
                parseCollection(id, location, ListShape.builder());
                break;
            case "set":
                parseCollection(id, location, SetShape.builder());
                break;
            case "map":
                parseMapStatement(id, location);
                break;
            case "boolean":
                parseSimpleShape(id, location, BooleanShape.builder());
                break;
            case "string":
                parseSimpleShape(id, location, StringShape.builder());
                break;
            case "enum":
                parseEnumShape(id, location, EnumShape.builder());
                break;
            case "blob":
                parseSimpleShape(id, location, BlobShape.builder());
                break;
            case "byte":
                parseSimpleShape(id, location, ByteShape.builder());
                break;
            case "short":
                parseSimpleShape(id, location, ShortShape.builder());
                break;
            case "integer":
                parseSimpleShape(id, location, IntegerShape.builder());
                break;
            case "intEnum":
                parseEnumShape(id, location, IntEnumShape.builder());
                break;
            case "long":
                parseSimpleShape(id, location, LongShape.builder());
                break;
            case "float":
                parseSimpleShape(id, location, FloatShape.builder());
                break;
            case "document":
                parseSimpleShape(id, location, DocumentShape.builder());
                break;
            case "double":
                parseSimpleShape(id, location, DoubleShape.builder());
                break;
            case "bigInteger":
                parseSimpleShape(id, location, BigIntegerShape.builder());
                break;
            case "bigDecimal":
                parseSimpleShape(id, location, BigDecimalShape.builder());
                break;
            case "timestamp":
                parseSimpleShape(id, location, TimestampShape.builder());
                break;
            default:
                // Unreachable.
                throw syntax(id, "Unexpected shape type: " + shapeType);
        }

        addTraits(id, traits);
        clearPendingDocs();
        br();
    }

    private ShapeId parseShapeName() {
        SourceLocation currentLocation = currentLocation();
        String name = ParserUtils.parseIdentifier(this);
        ShapeId id = ShapeId.fromRelative(expectNamespace(), name);

        if (useShapes.containsKey(name)) {
            ShapeId previous = useShapes.get(name);
            String message = String.format("Shape name `%s` conflicts with imported shape `%s`", name, previous);
            throw new ModelSyntaxException(message, currentLocation);
        }

        return id;
    }

    private void parseSimpleShape(ShapeId id, SourceLocation location, AbstractShapeBuilder<?, ?> builder) {
        LoadOperation.DefineShape operation = createShape(builder.source(location).id(id));
        parseMixins(operation);
        operations.accept(operation);
    }

    private void parseEnumShape(ShapeId id, SourceLocation location, AbstractShapeBuilder<?, ?> builder) {
        LoadOperation.DefineShape operation = createShape(builder.id(id).source(location));
        parseMixins(operation);

        ws();
        expect('{');
        clearPendingDocs();
        ws();

        while (!eof() && peek() != '}') {
            List<TraitEntry> memberTraits = parseDocsAndTraits();
            SourceLocation memberLocation = currentLocation();
            String memberName = ParserUtils.parseIdentifier(this);
            MemberShape.Builder memberBuilder = MemberShape.builder()
                    .id(id.withMember(memberName))
                    .source(memberLocation)
                    .target(UnitTypeTrait.UNIT);
            operation.addMember(memberBuilder);
            addTraits(memberBuilder.getId(), memberTraits);

            // Check for optional value assignment.
            sp();
            if (peek() == '=') {
                expect('=');
                sp();
                Node value = IdlNodeParser.parseNode(this);
                memberBuilder.addTrait(new EnumValueTrait.Provider().createTrait(memberBuilder.getId(), value));
                clearPendingDocs();
                br();
            } else {
                ws();
            }
        }

        expect('}');
        clearPendingDocs();
        operations.accept(operation);
    }

    // See parseMap for information on why members are parsed before the
    // list/set is registered with the ModelFile.
    private void parseCollection(ShapeId id, SourceLocation location, CollectionShape.Builder<?, ?> builder) {
        LoadOperation.DefineShape operation = createShape(builder.id(id).source(location));
        parseMixins(operation);
        ws();
        expect('{');
        clearPendingDocs();
        ws();
        parsePossiblyElidedMember(operation, "member");
        ws();
        expect('}');

        clearPendingDocs();
        operations.accept(operation);
    }

    // Parsed list, set, and map members.
    private void parsePossiblyElidedMember(LoadOperation.DefineShape operation, String memberName) {
        boolean isElided = false;
        List<TraitEntry> memberTraits = parseDocsAndTraits();

        if (peek() == '$') {
            isElided = true;
            if (!modelVersion.supportsTargetElision()) {
                throw syntax(operation.toShapeId().withMember(memberName),
                             "Members can only elide targets in IDL version 2 or later");
            }
            expect('$');
        } else if (peek() != memberName.charAt(0)) {
            if (!memberTraits.isEmpty()) {
                throw syntax("Expected member definition to follow traits");
            }
            return;
        }

        MemberShape.Builder memberBuilder = MemberShape.builder()
                .id(operation.toShapeId().withMember(memberName))
                .source(currentLocation());

        for (int i = 0; i < memberName.length(); i++) {
            expect(memberName.charAt(i));
        }

        if (!isElided) {
            sp();
            expect(':');
            sp();
            addForwardReference(ParserUtils.parseShapeId(this), memberBuilder::target);
        }

        operation.addMember(memberBuilder);
        addTraits(memberBuilder.getId(), memberTraits);
        clearPendingDocs();
    }

    private void parseMapStatement(ShapeId id, SourceLocation location) {
        LoadOperation.DefineShape operation = createShape(MapShape.builder().id(id).source(location));
        parseMixins(operation);
        ws();
        expect('{');
        clearPendingDocs();
        ws();
        parsePossiblyElidedMember(operation, "key");
        ws();
        parsePossiblyElidedMember(operation, "value");
        ws();
        expect('}');
        clearPendingDocs();
        operations.accept(operation);
    }

    private void parseStructuredShape(
            ShapeId id,
            SourceLocation location,
            AbstractShapeBuilder<?, ?> builder,
            MemberParsing memberParsing
    ) {
        LoadOperation.DefineShape operation = createShape(builder.id(id).source(location));

        // If it's a structure, parse the optional "from" statement to enable
        // eliding member targets for resource identifiers.
        if (builder.getShapeType() == ShapeType.STRUCTURE) {
            parseForResource(operation);
        }

        // Parse optional "with" statements to add mixins, but only if it's supported by the version.
        parseMixins(operation);
        parseMembers(operation, memberParsing);
        clearPendingDocs();
        operations.accept(operation);
    }

    private void parseMixins(LoadOperation.DefineShape operation) {
        sp();
        if (peek() != 'w') {
            return;
        }

        expect('w');
        expect('i');
        expect('t');
        expect('h');

        if (!modelVersion.supportsMixins()) {
            throw syntax(operation.toShapeId(), "Mixins can only be used with Smithy version 2 or later. "
                    + "Attempted to use mixins with version `" + modelVersion + "`.");
        }

        ws();
        expect('[');
        ws();

        do {
            String target = ParserUtils.parseShapeId(this);
            addForwardReference(target, resolved -> {
                operation.addDependency(resolved);
                operation.addModifier(new ApplyMixin(resolved));
            });
            ws();
        } while (peek() != ']');

        expect(']');
        clearPendingDocs();
    }

    private enum MemberParsing {
        PARSING_STRUCTURE_MEMBER {
            @Override
            boolean supportsAssignment() {
                return true;
            }

            @Override
            Trait createAssignmentTrait(ShapeId id, Node value) {
                return new DefaultTrait(value);
            }
        },
        PARSING_MEMBER {
            @Override
            boolean supportsAssignment() {
                return false;
            }

            @Override
            Trait createAssignmentTrait(ShapeId id, Node value) {
                throw new UnsupportedOperationException();
            }
        };

        abstract boolean supportsAssignment();

        abstract Trait createAssignmentTrait(ShapeId id, Node value);
    }

    private void parseMembers(LoadOperation.DefineShape op, MemberParsing memberParsing) {
        Set<String> definedMembers = new HashSet<>();

        ws();
        expect('{');
        ws();

        while (!eof()) {
            if (peek() == '}') {
                break;
            }

            parseMember(op, definedMembers, memberParsing);

            ws();
        }

        clearPendingDocs();
        expect('}');
    }

    private void parseMember(LoadOperation.DefineShape operation, Set<String> defined, MemberParsing memberParsing) {
        ShapeId parent = operation.toShapeId();

        // Parse optional member traits.
        List<TraitEntry> memberTraits = parseDocsAndTraits();
        SourceLocation memberLocation = currentLocation();

        boolean isTargetElided = peek() == '$';
        if (isTargetElided) {
            expect('$');
        }

        String memberName = ParserUtils.parseIdentifier(this);

        if (defined.contains(memberName)) {
            // This is a duplicate member name.
            throw syntax(parent, "Duplicate member of " + parent + ": '" + memberName + '\'');
        }

        defined.add(memberName);

        ShapeId memberId = parent.withMember(memberName);

        if (isTargetElided && !modelVersion.supportsTargetElision()) {
            throw syntax(memberId, "Members can only elide targets in IDL version 2 or later");
        }

        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(memberLocation);

        // Members whose targets are elided will have those targets resolved later,
        // for example by SetResourceBasedTargets
        if (!isTargetElided) {
            sp();
            expect(':');
            sp();
            addForwardReference(ParserUtils.parseShapeId(this), memberBuilder::target);
        }

        // Skip spaces to check if there is default trait sugar.
        sp();

        if (memberParsing.supportsAssignment() && peek() == '=') {
            if (!modelVersion.isDefaultSupported()) {
                throw syntax("@default assignment is only supported in IDL version 2 or later");
            }
            expect('=');
            sp();
            memberBuilder.addTrait(memberParsing.createAssignmentTrait(memberId, IdlNodeParser.parseNode(this)));
            br();
        } else {
            // Clears out any previously captured documentation
            // comments that may have been found when parsing the member.
            // Default value parsing may safely load comments for the next
            // member, so leave those intact.
            clearPendingDocs();
        }

        // Only add the member once fully parsed.
        operation.addMember(memberBuilder);
        addTraits(memberBuilder.getId(), memberTraits);
    }

    private void parseOperationStatement(ShapeId id, SourceLocation location) {
        OperationShape.Builder builder = OperationShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        parseMixins(operation);
        ws();
        expect('{');
        ws();

        parseProperties(id, propertyName -> {
            switch (propertyName) {
                case "input":
                    TraitEntry inputTrait = new TraitEntry(InputTrait.ID.toString(), Node.objectNode(), true);
                    parseInlineableOperationMember(id, operationInputSuffix, builder::input, inputTrait);
                    break;
                case "output":
                    TraitEntry outputTrait = new TraitEntry(OutputTrait.ID.toString(), Node.objectNode(), true);
                    parseInlineableOperationMember(id, operationOutputSuffix, builder::output, outputTrait);
                    break;
                case "errors":
                    parseIdList(builder::addError);
                    break;
                default:
                    throw syntax(id, String.format("Unknown property %s for %s", propertyName, id));
            }
            rws();
        });

        expect('}');
        clearPendingDocs();
        operations.accept(operation);
    }

    private void parseProperties(ShapeId id, Consumer<String> valueParser) {
        Set<String> defined = new HashSet<>();
        while (!eof() && peek() != '}') {
            String key = ParserUtils.parseIdentifier(this);
            if (defined.contains(key)) {
                throw syntax(id, String.format("Duplicate operation %s property for %s", key, id));
            }
            defined.add(key);

            ws();
            expect(':');
            valueParser.accept(key);
            ws();
        }
    }

    private void parseInlineableOperationMember(
            ShapeId id,
            String suffix,
            Consumer<ShapeId> consumer,
            TraitEntry defaultTrait
    ) {
        if (peek() == '=') {
            if (!modelVersion.supportsInlineOperationIO()) {
                throw syntax(id, "Inlined operation inputs and outputs can only be used with Smithy version 2 or "
                        + "later. Attempted to use inlined IO with version `" + modelVersion + "`.");
            }
            expect('=');
            clearPendingDocs();
            ws();
            consumer.accept(parseInlineStructure(id.getName() + suffix, defaultTrait));
        } else {
            ws();
            addForwardReference(ParserUtils.parseShapeId(this), consumer);
        }
    }

    private ShapeId parseInlineStructure(String name, TraitEntry defaultTrait) {
        List<TraitEntry> traits = parseDocsAndTraits();
        if (defaultTrait != null) {
            traits.add(defaultTrait);
        }
        ShapeId id = ShapeId.fromRelative(expectNamespace(), name);
        SourceLocation location = currentLocation();
        StructureShape.Builder builder = StructureShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        parseMixins(operation);
        parseForResource(operation);
        parseMembers(operation, MemberParsing.PARSING_STRUCTURE_MEMBER);
        addTraits(id, traits);
        clearPendingDocs();
        operations.accept(operation);
        return id;
    }

    private void parseForResource(LoadOperation.DefineShape operation) {
        sp();
        if (peek() != 'f') {
            return;
        }

        expect('f');
        expect('o');
        expect('r');

        if (!modelVersion.supportsTargetElision()) {
            throw syntax(operation.toShapeId(), "Structures can only be bound to resources with Smithy version 2 or "
                                                + "later. Attempted to bind a structure to a resource with version `"
                                                + modelVersion + "`.");
        }

        rsp();

        addForwardReference(ParserUtils.parseShapeId(this), shapeId -> {
            operation.addDependency(shapeId);
            operation.addModifier(new ApplyResourceBasedTargets(shapeId));
        });
    }

    private void parseIdList(Consumer<ShapeId> consumer) {
        increaseNestingLevel();
        ws();
        expect('[');
        ws();

        while (!eof() && peek() != ']') {
            addForwardReference(ParserUtils.parseShapeId(this), consumer);
            ws();
        }

        expect(']');
        decreaseNestingLevel();
    }

    private void parseServiceStatement(ShapeId id, SourceLocation location) {
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        parseMixins(operation);
        ws();
        ObjectNode shapeNode = IdlNodeParser.parseObjectNode(this, id.toString());
        LoaderUtils.checkForAdditionalProperties(shapeNode, id, SERVICE_PROPERTY_NAMES).ifPresent(this::emit);
        shapeNode.getStringMember(VERSION_KEY).map(StringNode::getValue).ifPresent(builder::version);
        optionalIdList(shapeNode, OPERATIONS_KEY, builder::addOperation);
        optionalIdList(shapeNode, RESOURCES_KEY, builder::addResource);
        optionalIdList(shapeNode, ERRORS_KEY, builder::addError);
        AstModelLoader.loadServiceRenameIntoBuilder(builder, shapeNode);
        clearPendingDocs();
        operations.accept(operation);
    }

    private void optionalId(ObjectNode node, String name, Consumer<ShapeId> consumer) {
        if (node.getMember(name).isPresent()) {
            addForwardReference(node.expectStringMember(name).getValue(), consumer);
        }
    }

    private void optionalIdList(ObjectNode node, String name, Consumer<ShapeId> consumer) {
        if (node.getMember(name).isPresent()) {
            ArrayNode value = node.expectArrayMember(name);
            for (StringNode element : value.getElementsAs(StringNode.class)) {
                addForwardReference(element.getValue(), consumer);
            }
        }
    }

    private void parseResourceStatement(ShapeId id, SourceLocation location) {
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);

        parseMixins(operation);
        ws();

        ObjectNode shapeNode = IdlNodeParser.parseObjectNode(this, id.toString());
        LoaderUtils.checkForAdditionalProperties(shapeNode, id, RESOURCE_PROPERTY_NAMES).ifPresent(this::emit);
        optionalId(shapeNode, PUT_KEY, builder::put);
        optionalId(shapeNode, CREATE_KEY, builder::create);
        optionalId(shapeNode, READ_KEY, builder::read);
        optionalId(shapeNode, UPDATE_KEY, builder::update);
        optionalId(shapeNode, DELETE_KEY, builder::delete);
        optionalId(shapeNode, LIST_KEY, builder::list);
        optionalIdList(shapeNode, OPERATIONS_KEY, builder::addOperation);
        optionalIdList(shapeNode, RESOURCES_KEY, builder::addResource);
        optionalIdList(shapeNode, COLLECTION_OPERATIONS_KEY, builder::addCollectionOperation);

        // Load identifiers and resolve forward references.
        shapeNode.getObjectMember(IDENTIFIERS_KEY).ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                addForwardReference(target.getValue(), targetId -> builder.addIdentifier(name, targetId));
            }
        });
        // Load properties and resolve forward references.
        shapeNode.getObjectMember(PROPERTIES_KEY).ifPresent(properties -> {
            if (!modelVersion.supportsResourceProperties()) {
                throw syntax(id, "Resource properties can only be used with Smithy version 2 or later. "
                                 + "Attempted to use resource properties with version `" + modelVersion + "`.");
            }
            for (Map.Entry<StringNode, Node> entry : properties.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                addForwardReference(target.getValue(), targetId -> builder.addProperty(name, targetId));
            }
        });
        clearPendingDocs();
        operations.accept(operation);
    }

    // "//" *(not_newline)
    private void parseComment() {
        expect('/');
        consumeRemainingCharactersOnLine();
    }

    private void parseDocComment() {
        SourceLocation location = currentLocation();
        StringJoiner joiner = new StringJoiner("\n");
        do {
            joiner.add(parseDocCommentLine());
        } while (peekDocComment());
        pendingDocumentationComment = new TraitEntry(
                DocumentationTrait.ID.toString(), new StringNode(joiner.toString(), location), false);
    }

    private boolean peekDocComment() {
        return peek() == '/' && peek(1) == '/' && peek(2) == '/';
    }

    // documentation_comment = "///" *(not_newline)
    private String parseDocCommentLine() {
        expect('/');
        expect('/');
        expect('/');
        // Skip a leading space, if present.
        if (peek() == ' ') {
            skip();
        }
        int start = position();
        consumeRemainingCharactersOnLine();
        nl();
        sp();
        return StringUtils.stripEnd(sliceFrom(start), " \t\r\n");
    }

    private void nl() {
        switch (peek()) {
            case '\n':
                skip();
                break;
            case '\r':
                skip();
                if (peek() == '\n') {
                    expect('\n');
                }
                break;
            default:
                throw syntax("Expected a newline");
        }
    }

    private void parseApplyStatement() {
        expect('a');
        expect('p');
        expect('p');
        expect('l');
        expect('y');
        rsp();

        String name = ParserUtils.parseShapeId(this);
        rws();

        // Account for singular or block apply statements.
        List<TraitEntry> traitsToApply;
        if (peek() == '{') {
            expect('{');
            ws();
            traitsToApply = IdlTraitParser.parseTraits(this);
            expect('}');
        } else {
            traitsToApply = Collections.singletonList(IdlTraitParser.parseTraitValue(this));
        }

        // First, resolve the targeted shape.
        addForwardReference(name, target -> {
            for (TraitEntry traitEntry : traitsToApply) {
                onDeferredTrait(target, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
            }
        });

        // Clear out any errantly captured pending docs.
        clearPendingDocs();
        br();
    }

    private void addTraits(ShapeId id, List<TraitEntry> traits) {
        for (TraitEntry traitEntry : traits) {
            onDeferredTrait(id, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
        }
    }

    /**
     * Adds a trait to a shape after resolving all shape IDs.
     *
     * <p>Resolving the trait against a trait definition is deferred until
     * the entire model is loaded. A namespace is required to have been set
     * if the trait name is not absolute.
     *
     * @param target Shape to add the trait to.
     * @param traitName Trait name to add.
     * @param traitValue Trait value as a Node object.
     * @param isAnnotation Set to true to indicate that the value for the trait was omitted.
     */
    private void onDeferredTrait(ShapeId target, String traitName, Node traitValue, boolean isAnnotation) {
        addForwardReference(traitName, (traitId, typeProvider) -> {
            Node coerced = coerceTraitValue(traitValue, isAnnotation, typeProvider.apply(traitId));
            operations.accept(new LoadOperation.ApplyTrait(
                    modelVersion, traitValue.getSourceLocation(), expectNamespace(), target, traitId, coerced));
        });
    }

    private Node coerceTraitValue(Node value, boolean isAnnotation, ShapeType targetType) {
        if (isAnnotation && value.isNullNode()) {
            if (targetType == null || targetType == ShapeType.STRUCTURE || targetType == ShapeType.MAP) {
                // The targetType == null condition helps mitigate a confusing
                // failure mode where a trait isn't defined in the model, but a
                // TraitService is found as a service provider for the trait.
                // If the TraitService creates an annotation trait, then using null
                // instead of object results in a failure about passing null for an
                // annotation trait, and that's confusing because the actual error
                // message should be about the missing trait definition. Because the
                // vast majority of annotation traits are modeled as objects, this
                // makes the assumption that the value is an object (which addresses
                // the above failure case).
                return new ObjectNode(Collections.emptyMap(), value.getSourceLocation());
            } else if (targetType == ShapeType.LIST || targetType == ShapeType.SET) {
                return new ArrayNode(Collections.emptyList(), value.getSourceLocation());
            }
        }

        return value;
    }

    SourceLocation currentLocation() {
        return new SourceLocation(filename, line(), column());
    }

    NumberNode parseNumberNode(SourceLocation location) {
        String lexeme = ParserUtils.parseNumber(this);

        if (lexeme.contains("e") || lexeme.contains("E")  || lexeme.contains(".")) {
            double value = Double.parseDouble(lexeme);
            if (Double.isFinite(value)) {
                return new NumberNode(value, location);
            }
            return new NumberNode(new BigDecimal(lexeme), location);
        } else {
            try {
                return new NumberNode(Long.parseLong(lexeme), location);
            } catch (NumberFormatException e) {
                return new NumberNode(new BigInteger(lexeme), location);
            }
        }
    }

    private String peekDebugMessage() {
        StringBuilder result = new StringBuilder(expression().length());

        char c = peek();

        // Try to read an entire identifier for context (16 char max) if that's what's being peeked.
        if (c == ' ' || ParserUtils.isIdentifierStart(c) || ParserUtils.isDigit(c)) {
            if (c == ' ') {
                result.append(' ');
            }
            for (int i = c == ' ' ? 1 : 0; i < 16; i++) {
                c = peek(i);
                if (ParserUtils.isIdentifierStart(c) || ParserUtils.isDigit(c)) {
                    result.append(c);
                } else {
                    break;
                }
            }
        } else {
            // Take two characters for context.
            for (int i = 0; i < 2; i++) {
                char peek = peek(i);
                if (peek != Character.MIN_VALUE) {
                    result.append(peek);
                }
            }
        }

        return result.length() == 0 ? "[EOF]" : result.toString();
    }
}
