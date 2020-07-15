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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
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
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
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
    private static final String COLLECTION_OPERATIONS_KEY = "collectionOperations";
    private static final String IDENTIFIERS_KEY = "identifiers";
    private static final String VERSION_KEY = "version";
    private static final String TYPE_KEY = "type";

    static final Collection<String> RESOURCE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, CREATE_KEY, READ_KEY, UPDATE_KEY, DELETE_KEY, LIST_KEY,
            IDENTIFIERS_KEY, RESOURCES_KEY, OPERATIONS_KEY, PUT_KEY, COLLECTION_OPERATIONS_KEY);
    static final List<String> SERVICE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, VERSION_KEY, OPERATIONS_KEY, RESOURCES_KEY);
    private static final Collection<String> OPERATION_PROPERTY_NAMES = ListUtils.of("input", "output", "errors");
    private static final Set<String> SHAPE_TYPES = new HashSet<>();

    static {
        for (ShapeType type : ShapeType.values()) {
            if (type != ShapeType.MEMBER) {
                SHAPE_TYPES.add(type.toString());
            }
        }
    }

    private final String filename;
    private final LoaderVisitor visitor;
    private String namespace;
    private String definedVersion;
    private TraitEntry pendingDocumentationComment;

    /** Map of shape aliases to their targets. */
    private final Map<String, ShapeId> useShapes = new HashMap<>();

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

    IdlModelParser(String filename, String model, LoaderVisitor visitor) {
        super(model, MAX_NESTING_LEVEL);
        this.filename = filename;
        this.visitor = visitor;
    }

    void parse() {
        ws();
        parseControlSection();
        parseMetadataSection();
        parseShapeSection();
    }

    /**
     * Overrides whitespace parsing to handle comments.
     */
    @Override
    public void ws() {
        while (!eof()) {
            char c = peek();
            if (c == '/') {
                if (peekDocComment()) {
                    parseDocComment();
                } else {
                    parseComment();
                }
            } else if (!(c == ' ' || c == '\t' || c == '\r' || c == '\n')) {
                break;
            } else {
                skip();
            }
        }
    }

    @Override
    public ModelSyntaxException syntax(String message) {
        String formatted = format(
                "Parse error at line %d, column %d near `%s`: %s",
                line(), column(), peekDebugMessage(), message);
        return new ModelSyntaxException(formatted, filename, line(), column());
    }

    private void parseControlSection() {
        while (peek() == '$') {
            expect('$');
            ws();
            String key = IdlNodeParser.parseNodeObjectKey(this);
            ws();
            expect(':');
            ws();

            // Validation here for better error location.
            if (key.equals("version") && definedVersion != null) {
                throw syntax("Cannot define multiple versions in the same file");
            }

            Node value = IdlNodeParser.parseNode(this);

            if (key.equals("version")) {
                onVersion(value);
            } else {
                visitor.onError(ValidationEvent.builder()
                        .eventId(Validator.MODEL_ERROR)
                        .sourceLocation(value)
                        .severity(Severity.WARNING)
                        .message(format("Unknown control statement `%s` with value `%s", key, Node.printJson(value)))
                        .build());
            }

            br();
            ws();
        }
    }

    private void onVersion(Node value) {
        if (!value.isStringNode()) {
            value.expectStringNode("The $version control statement must have a string value, but found "
                                   + Node.printJson(value));
        }

        String parsedVersion = value.expectStringNode().getValue();
        if (!visitor.isVersionSupported(parsedVersion)) {
            throw syntax("Unsupported Smithy version number: " + parsedVersion);
        }

        definedVersion = parsedVersion;
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
            ws();
            String key = IdlNodeParser.parseNodeObjectKey(this);
            ws();
            expect('=');
            ws();
            visitor.onMetadata(key, IdlNodeParser.parseNode(this));
            br();
            ws();
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
            ws();

            // Parse the namespace.
            int start = position();
            ParserUtils.consumeNamespace(this);
            namespace = sliceFrom(start);

            br();
            // Clear out any erroneous documentation comments.
            clearPendingDocs();
            ws();
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
            ws();

            int start = position();
            ParserUtils.consumeNamespace(this);
            expect('#');
            ParserUtils.consumeIdentifier(this);
            String lexeme = sliceFrom(start);
            br();
            // Clear out any erroneous documentation comments.
            clearPendingDocs();
            ws();

            ShapeId target = ShapeId.from(lexeme);
            ShapeId previous = useShapes.put(target.getName(), target);
            if (previous != null) {
                throw syntax(String.format("Cannot use name `%s` because it conflicts with `%s`",
                                           target, previous));
            }
        }
    }

    private void parseShapeStatements() {
        while (!eof()) {
            ws();
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

        ws();
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
                parseStructuredShape(id, location, StructureShape.builder());
                break;
            case "union":
                parseStructuredShape(id, location, UnionShape.builder());
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
                throw syntax("Unexpected shape type: " + shapeType);
        }

        addTraits(id, traits);
        clearPendingDocs();
        br();
    }

    private ShapeId parseShapeName() {
        String name = ParserUtils.parseIdentifier(this);

        if (useShapes.containsKey(name)) {
            throw syntax(String.format(
                    "shape name `%s` conflicts with imported shape `%s`", name, useShapes.get(name)));
        }

        return ShapeId.fromRelative(namespace, name);
    }

    private void parseSimpleShape(ShapeId id, SourceLocation location, AbstractShapeBuilder builder) {
        visitor.onShape(builder.source(location).id(id));
    }

    // See parseMap for information on why members are parsed before the
    // list/set is registered with the LoaderVisitor.
    private void parseCollection(ShapeId id, SourceLocation location, CollectionShape.Builder builder) {
        ws();
        builder.id(id).source(location);
        parseMembers(id, SetUtils.of("member"));
        visitor.onShape(builder.id(id));
    }

    private void parseMembers(ShapeId id, Set<String> requiredMembers) {
        Set<String> remaining = requiredMembers.isEmpty()
                ? requiredMembers
                : new HashSet<>(requiredMembers);
        ws();
        expect('{');
        // Don't keep any previous state of captured doc comments when
        // parsing members.
        clearPendingDocs();
        ws();

        if (peek() != '}') {
            // Remove the parsed member from the remaining set to detect
            // when duplicates are found, or when members are missing.
            remaining.remove(parseMember(id, remaining));
            while (!eof()) {
                ws();
                if (peek() == ',') {
                    expect(',');
                    // A comma clears out any previously captured documentation
                    // comments that may have been found when parsing the member.
                    clearPendingDocs();
                    ws();
                    if (peek() == '}') {
                        // Trailing comma: "," "}"
                        break;
                    }

                    // Special casing to detect invalid members early on, even
                    // after draining all the valid members. This keeps builders
                    // from raising confusing errors about invalid members.
                    if (remaining.isEmpty() && !requiredMembers.isEmpty()) {
                        parseMember(id, requiredMembers);
                    } else {
                        remaining.remove(parseMember(id, remaining));
                    }
                } else {
                    // Assume '}'; break to enforce.
                    break;
                }
            }
        }

        if (!remaining.isEmpty()) {
            throw syntax("Missing required members of shape `" + id + "`: ["
                         + ValidationUtils.tickedList(remaining) + ']');
        }

        expect('}');
    }

    private String parseMember(ShapeId parent, Set<String> requiredMembers) {
        // Parse optional member traits.
        List<TraitEntry> memberTraits = parseDocsAndTraits();
        SourceLocation memberLocation = currentLocation();
        String memberName = ParserUtils.parseIdentifier(this);

        // Only enforce "allowedMembers" if it isn't empty.
        if (!requiredMembers.isEmpty() && !requiredMembers.contains(memberName)) {
            throw syntax("Unexpected member of " + parent + ": '" + memberName + '\'');
        }

        ws();
        expect(':');
        ws();
        ShapeId memberId = parent.withMember(memberName);
        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(memberLocation);
        SourceLocation targetLocation = currentLocation();
        String target = ParserUtils.parseShapeId(this);
        visitor.onShape(memberBuilder);
        onShapeTarget(target, targetLocation, memberBuilder::target);
        addTraits(memberId, memberTraits);

        return memberName;
    }

    private void parseMapStatement(ShapeId id, SourceLocation location) {
        // Parsing members of list/set/map before registering the shape with
        // the LoaderVisitor ensures that the shape is only registered if it
        // has all of its required members. Otherwise, the LoaderVisitor gives
        // a cryptic message with no context about how a "member" wasn't set
        // on a builder. This does not suffer the same error messages as
        // structures/unions because list/set/map have a fixed and required
        // set of members that must be provided.
        parseMembers(id, SetUtils.of("key", "value"));
        visitor.onShape(MapShape.builder().id(id).source(location));
    }

    private void parseStructuredShape(ShapeId id, SourceLocation location, AbstractShapeBuilder builder) {
        // Register the structure/union with the loader before parsing members.
        // This will detect shape conflicts with other types (like an operation)
        // and still give useful error messages. Trying to parse members first
        // would otherwise result in cryptic error messages like:
        // "Member `foo.baz#Foo$Baz` cannot be added to software.amazon.smithy.model.shapes.OperationShape$Builder"
        visitor.onShape(builder.id(id).source(location));
        parseMembers(id, Collections.emptySet());
    }

    private void parseOperationStatement(ShapeId id, SourceLocation location) {
        ws();
        OperationShape.Builder builder = OperationShape.builder().id(id).source(location);
        ObjectNode node = IdlNodeParser.parseObjectNode(this);
        visitor.checkForAdditionalProperties(node, id, OPERATION_PROPERTY_NAMES);
        visitor.onShape(builder);
        node.getStringMember("input").ifPresent(input -> {
            onShapeTarget(input.getValue(), input, builder::input);
        });
        node.getStringMember("output").ifPresent(output -> {
            onShapeTarget(output.getValue(), output, builder::output);
        });
        node.getArrayMember("errors").ifPresent(errors -> {
            for (StringNode value : errors.getElementsAs(StringNode.class)) {
                onShapeTarget(value.getValue(), value, builder::addError);
            }
        });
    }

    private void parseServiceStatement(ShapeId id, SourceLocation location) {
        ws();
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(location);
        ObjectNode shapeNode = IdlNodeParser.parseObjectNode(this);
        visitor.checkForAdditionalProperties(shapeNode, id, SERVICE_PROPERTY_NAMES);
        builder.version(shapeNode.expectStringMember(VERSION_KEY).getValue());
        visitor.onShape(builder);
        optionalIdList(shapeNode, id.getNamespace(), OPERATIONS_KEY).forEach(builder::addOperation);
        optionalIdList(shapeNode, id.getNamespace(), RESOURCES_KEY).forEach(builder::addResource);
    }

    private static Optional<ShapeId> optionalId(ObjectNode node, String namespace, String name) {
        return node.getStringMember(name).map(stringNode -> stringNode.expectShapeId(namespace));
    }

    private static List<ShapeId> optionalIdList(ObjectNode node, String namespace, String name) {
        return node.getArrayMember(name)
                .map(array -> array.getElements().stream()
                        .map(Node::expectStringNode)
                        .map(s -> s.expectShapeId(namespace))
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    private void parseResourceStatement(ShapeId id, SourceLocation location) {
        ws();
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(location);
        visitor.onShape(builder);
        ObjectNode shapeNode = IdlNodeParser.parseObjectNode(this);

        visitor.checkForAdditionalProperties(shapeNode, id, RESOURCE_PROPERTY_NAMES);
        optionalId(shapeNode, id.getNamespace(), PUT_KEY).ifPresent(builder::put);
        optionalId(shapeNode, id.getNamespace(), CREATE_KEY).ifPresent(builder::create);
        optionalId(shapeNode, id.getNamespace(), READ_KEY).ifPresent(builder::read);
        optionalId(shapeNode, id.getNamespace(), UPDATE_KEY).ifPresent(builder::update);
        optionalId(shapeNode, id.getNamespace(), DELETE_KEY).ifPresent(builder::delete);
        optionalId(shapeNode, id.getNamespace(), LIST_KEY).ifPresent(builder::list);
        optionalIdList(shapeNode, id.getNamespace(), OPERATIONS_KEY).forEach(builder::addOperation);
        optionalIdList(shapeNode, id.getNamespace(), RESOURCES_KEY).forEach(builder::addResource);
        optionalIdList(shapeNode, id.getNamespace(), COLLECTION_OPERATIONS_KEY)
                .forEach(builder::addCollectionOperation);

        // Load identifiers and resolve forward references.
        shapeNode.getObjectMember(IDENTIFIERS_KEY).ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                onShapeTarget(target.getValue(), target, targetId -> builder.addIdentifier(name, targetId));
            }
        });
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
        br();
        sp();
        return StringUtils.stripEnd(sliceFrom(start), " \t\r\n");
    }

    private void parseApplyStatement() {
        expect('a');
        expect('p');
        expect('p');
        expect('l');
        expect('y');
        ws();

        SourceLocation location = currentLocation();
        String name = ParserUtils.parseShapeId(this);
        ws();

        TraitEntry traitEntry = IdlTraitParser.parseTraitValue(this);

        // First, resolve the targeted shape.
        onShapeTarget(name, location, id -> {
            // Next, resolve the trait ID.
            onDeferredTrait(id, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
        });

        // Clear out any errantly captured pending docs.
        clearPendingDocs();
        br();
        ws();
    }

    /**
     * Resolve shape targets and tracks forward references.
     *
     * <p>Smithy models allow for forward references to shapes that have not
     * yet been defined. Only after all shapes are loaded is the entire set
     * of possible shape IDs known. This normally isn't a concern, but Smithy
     * allows for public shapes defined in the prelude to be referenced by
     * targets like members and resource identifiers without an absolute
     * shape ID (for example, {@code String}). However, a relative prelude
     * shape ID is only resolved for such a target if a shape of the same
     * name was not defined in the same namespace in which the target
     * was defined.
     *
     * <p>If a shape in the same namespace as the target has already been
     * defined or if the target is absolute and cannot resolve to a prelude
     * shape, the provided {@code resolver} is invoked immediately. Otherwise,
     * the {@code resolver} is invoked inside of the {@link LoaderVisitor#onEnd}
     * method only after all shapes have been declared.
     *
     * @param target Shape that is targeted.
     * @param sourceLocation The location of where the target occurred.
     * @param resolver The consumer to invoke once the shape ID is resolved.
     */
    void onShapeTarget(String target, FromSourceLocation sourceLocation, Consumer<ShapeId> resolver) {
        // Account for aliased shapes.
        if (useShapes.containsKey(target)) {
            resolver.accept(useShapes.get(target));
            return;
        }

        // A namespace is not set when parsing metadata.
        ShapeId expectedId = namespace == null
                ? ShapeId.from(target)
                : ShapeId.fromOptionalNamespace(namespace, target);
        if (isRealizedShapeId(expectedId, target)) {
            // Account for previously seen shapes in this namespace, absolute shapes, and prelude namespaces
            // always resolve to prelude shapes.
            resolver.accept(expectedId);
        } else {
            visitor.addForwardReference(expectedId, resolver);
        }
    }

    /**
     * Returns true if the shape ID does not need to be deferred.
     *
     * @param expectedId Shape ID that this ID probably references.
     * @param target The target name.
     * @return Returns true if this is a known shape ID.
     */
    private boolean isRealizedShapeId(ShapeId expectedId, String target) {
        return Objects.equals(namespace, Prelude.NAMESPACE)
               || visitor.hasDefinedShape(expectedId)
               || target.contains("#");
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
        onShapeTarget(traitName, traitValue.getSourceLocation(), id -> {
            if (isAnnotation) {
                visitor.onAnnotationTrait(target, id, traitValue.expectNullNode());
            } else {
                visitor.onTrait(target, id, traitValue);
            }
        });
    }

    SourceLocation currentLocation() {
        return new SourceLocation(filename, line(), column());
    }

    NumberNode parseNumberNode() {
        SourceLocation location = currentLocation();
        String lexeme = ParserUtils.parseNumber(this);
        if (lexeme.contains("e") || lexeme.contains(".")) {
            return new NumberNode(Double.valueOf(lexeme), location);
        } else {
            return new NumberNode(Long.parseLong(lexeme), location);
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
            return result.toString();
        }

        // Take two characters for context.
        for (int i = 0; i < 2; i++) {
            char peek = peek(i);
            if (peek == Character.MIN_VALUE) {
                result.append("[EOF]");
                break;
            }
            result.append(peek);
        }

        return result.toString();
    }
}
