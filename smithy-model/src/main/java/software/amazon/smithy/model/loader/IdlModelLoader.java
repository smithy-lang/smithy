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

import static java.lang.String.format;
import static software.amazon.smithy.model.loader.SmithyModelLexer.Token;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.ANNOTATION;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.COLON;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.COMMA;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.CONTROL;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.DOC;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.EQUAL;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.ERROR;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.LBRACE;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.LBRACKET;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.LPAREN;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.NUMBER;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.QUOTED;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.RBRACE;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.RBRACKET;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.RPAREN;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.UNQUOTED;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
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
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
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
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;

final class IdlModelLoader {

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

    // Top-level statements
    private static final Set<String> STATEMENTS = SetUtils.of(
            "namespace", "use", "service", "operation", "resource", "structure", "union",
            "list", "set", "map", "boolean", "string", "blob", "byte", "short",
            "integer", "long", "float", "document", "double", "bigInteger", "bigDecimal",
            "timestamp", "metadata", "apply");

    // Statements that support traits.
    private static final Set<String> SUPPORTS_TRAITS = SetUtils.of(
            "bigDecimal", "bigInteger", "blob", "boolean", "byte", "document",
            "double", "float", "integer", "list", "long", "map", "operation", "resource",
            "service", "set", "short", "string", "structure", "timestamp", "union");

    // Valid map keys.
    private static final Collection<String> MAP_KEYS = ListUtils.of("key", "value");

    // Valid operation properties.
    private static final Collection<String> OPERATION_PROPERTY_NAMES = ListUtils.of("input", "output", "errors");

    private final String filename;
    private final SmithyModelLexer lexer;
    private final LoaderVisitor visitor;
    private final List<TraitEntry> pendingTraits = new ArrayList<>();

    /** Map of shape aliases to their targets. */
    private final Map<String, ShapeId> useShapes = new HashMap<>();

    private Token current;
    private DocComment pendingDocComment;
    private String definedVersion;
    private String namespace;
    private boolean definedMetadata;
    private boolean definedShapes;

    private IdlModelLoader(String filename, SmithyModelLexer lexer, LoaderVisitor visitor) {
        this.filename = filename;
        this.visitor = visitor;
        this.lexer = lexer;

        while (!eof()) {
            Token token = expect(UNQUOTED, ANNOTATION, CONTROL, DOC);
            if (token.type == UNQUOTED) {
                parseStatement(token);
            } else if (token.type == ANNOTATION) {
                pendingTraits.add(parseTraitValue(token, TraitValueType.SHAPE));
            } else if (token.type == CONTROL) {
                parseControlStatement(token);
            } else if (token.type == DOC) {
                parseDocComment(token, false);
            }
        }
    }

    private enum TraitValueType { SHAPE, MEMBER, APPLY }

    /**
     * Pending documentation comment that can be added to shapes or trait defs.
     */
    private static final class DocComment {
        final SourceLocation sourceLocation;
        final String content;

        DocComment(String content, SourceLocation sourceLocation) {
            this.content = content;
            this.sourceLocation = sourceLocation;
        }
    }

    // A pending trait that also doesn't yet have a resolved trait shape ID.
    private static final class TraitEntry {
        final String traitName;
        final Node value;
        final boolean isAnnotation;

        TraitEntry(String traitName, Node value, boolean isAnnotation) {
            this.traitName = traitName;
            this.value = value;
            this.isAnnotation = isAnnotation;
        }
    }

    public static void load(String path, Supplier<InputStream> contentSupplier, LoaderVisitor visitor) {
        try (SmithyModelLexer lexer = new SmithyModelLexer(path, contentSupplier.get())) {
            new IdlModelLoader(path, lexer, visitor);
        } catch (IOException e) {
            throw new ModelImportException("Error loading " + path + ": " + e.getMessage(), e);
        }
    }

    private void parseStatement(Token token) {
        switch (token.lexeme) {
            case "namespace":
                parseNamespace();
                break;
            case "use":
                parseUseStatement();
                break;
            case "structure":
                parseStructuredShape("structure", StructureShape.builder());
                break;
            case "union":
                parseStructuredShape("union", UnionShape.builder());
                break;
            case "list":
                parseCollection("list", ListShape.builder());
                break;
            case "set":
                parseCollection("set", SetShape.builder());
                break;
            case "map":
                parseMap();
                break;
            case "boolean":
                parseSimpleShape(BooleanShape.builder());
                break;
            case "string":
                parseSimpleShape(StringShape.builder());
                break;
            case "blob":
                parseSimpleShape(BlobShape.builder());
                break;
            case "byte":
                parseSimpleShape(ByteShape.builder());
                break;
            case "short":
                parseSimpleShape(ShortShape.builder());
                break;
            case "integer":
                parseSimpleShape(IntegerShape.builder());
                break;
            case "long":
                parseSimpleShape(LongShape.builder());
                break;
            case "float":
                parseSimpleShape(FloatShape.builder());
                break;
            case "document":
                parseSimpleShape(DocumentShape.builder());
                break;
            case "double":
                parseSimpleShape(DoubleShape.builder());
                break;
            case "bigInteger":
                parseSimpleShape(BigIntegerShape.builder());
                break;
            case "bigDecimal":
                parseSimpleShape(BigDecimalShape.builder());
                break;
            case "timestamp":
                parseSimpleShape(TimestampShape.builder());
                break;
            case "service":
                parseService();
                break;
            case "operation":
                parseOperation();
                break;
            case "resource":
                parseResource();
                break;
            case "metadata":
                parseMetadata();
                break;
            case "apply":
                parseApply();
                break;
            default:
                throw syntax(format("Expected one of %s", ValidationUtils.tickedList(STATEMENTS)));
        }
    }

    private void parseNamespace() {
        if (namespace != null) {
            throw syntax("Only a single namespace can be declared per/file. The previous namespace was set to "
                         + "`" + namespace + "`.");
        }

        String parsedNamespace = expect(UNQUOTED).lexeme;

        if (!ShapeId.isValidNamespace(parsedNamespace)) {
            throw syntax(format("Invalid namespace name `%s`", parsedNamespace));
        }

        namespace = parsedNamespace;
    }

    private void parseUseStatement() {
        if (namespace == null) {
            throw syntax("Use statements must appear after a namespace is defined");
        }

        if (definedShapes) {
            throw syntax("A use statement must come before any shape definition");
        }

        try {
            Token namespaceToken = expect(UNQUOTED);
            ShapeId target = ShapeId.from(namespaceToken.lexeme);
            ShapeId previous = useShapes.put(target.getName(), target);
            if (previous != null) {
                String message = String.format("Cannot use name `%s` because it conflicts with `%s`",
                                               target, previous);
                throw new UseException(message, namespaceToken.getSourceLocation());
            }
            expectNewline();
        } catch (ShapeIdSyntaxException e) {
            throw syntax(e.getMessage());
        }
    }

    private void parseControlStatement(Token token) {
        if (definedMetadata || namespace != null) {
            throw syntax("A control statement must come before any namespace, metadata, or shape");
        }

        String key = token.lexeme;
        Node value = parseNodeValue(next());

        if (key.equals("version")) {
            onVersion(value);
        } else {
            visitor.onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .sourceLocation(value)
                    .severity(Severity.WARNING)
                    .message(format(
                            "Unknown control statement `%s` with value `%s", key, Node.printJson(value)))
                    .build());
        }
    }

    private void onVersion(Node value) {
        if (!value.isStringNode()) {
            value.expectStringNode("The $version control statement must have a string value, but found "
                                   + Node.printJson(value));
        }

        String parsedVersion = value.expectStringNode().getValue();

        if (definedVersion != null) {
            throw syntax("Cannot define multiple versions in the same file");
        }

        if (!SmithyVersion.isSupported(parsedVersion)) {
            throw syntax(format("Invalid Smithy version number: %s", parsedVersion));
        }

        definedVersion = parsedVersion;
    }

    private void parseMetadata() {
        if (namespace != null) {
            throw syntax("Metadata statements must appear before a namespace statement");
        }

        definedMetadata = true;

        // metadata key = value\n
        String key = expect(QUOTED, UNQUOTED).lexeme;
        expect(EQUAL);
        visitor.onMetadata(key, parseNode());
        expectNewline();
    }

    private void parseDocComment(Token token, boolean memberScope) {
        StringBuilder builder = new StringBuilder(token.getDocContents());
        while (test(DOC)) {
            builder.append('\n').append(next().getDocContents());
        }

        pendingDocComment = new DocComment(builder.toString(), token.getSourceLocation());

        if (!peek().isPresent()) {
            throw syntax("Found a documentation comment that doesn't document anything");
        }

        Token next = peek().get();
        if (next.type != ANNOTATION
            && (next.type != UNQUOTED || (!memberScope && !SUPPORTS_TRAITS.contains(next.lexeme)))) {
            throw syntax("Documentation cannot be applied to `" + next.lexeme + "`");
        }
    }

    private TraitEntry parseTraitValue(Token token, TraitValueType type) {
        try {
            requireNamespaceOrThrow();

            // Resolve the trait name and ensure that the trait forms a syntactically valid value.
            ShapeId.fromOptionalNamespace(namespace, token.lexeme);
            TraitEntry result = parseTraitValueBody(token.lexeme);

            // `apply` doesn't require any specific token to follow.
            if (type == TraitValueType.APPLY) {
                return result;
            }

            // Other kinds of trait values require an annotation or definition to follow.
            if (!peek().isPresent()) {
                throw syntax("Found a trait doesn't apply to anything");
            }

            Token next = peek().get();
            if (next.type != ANNOTATION) {
                if (next.type != UNQUOTED
                        || (type != TraitValueType.MEMBER && !SUPPORTS_TRAITS.contains(next.lexeme))) {
                    throw syntax("Traits cannot be applied to `" + next.lexeme + "`");
                }
            }

            return result;
        } catch (ShapeIdSyntaxException e) {
            throw syntax("Invalid trait name syntax. Trait names must adhere to the same syntax as shape IDs.");
        }
    }

    private void requireNamespaceOrThrow() {
        if (namespace == null) {
            throw syntax("A namespace must be set before shapes or traits can be defined");
        }
    }

    private TraitEntry parseTraitValueBody(String traitName) {
        // Null is coerced into the appropriate type for the trait.
        if (!test(LPAREN)) {
            return new TraitEntry(traitName, new NullNode(currentLocation()), true);
        }

        expect(LPAREN);
        Token next = expect(RPAREN, QUOTED, UNQUOTED, LBRACKET, NUMBER);

        if (next.type == RPAREN) {
            // An open and closed "()" signals an empty object.
            return new TraitEntry(traitName, new ObjectNode(MapUtils.of(), next.getSourceLocation()), false);
        }

        // Test to see if this is just a string or if it's an object.
        if (test(COLON)) {
            if (next.type == QUOTED || next.type == UNQUOTED) {
                // Parse the object using the already parsed key.
                return new TraitEntry(traitName, parseObjectNodeWithKey(currentLocation(), RPAREN, next), false);
            }
            throw syntax("Expected a string to start a trait value object");
        }

        Node result;
        if (next.type == LBRACKET) {
            result = parseArrayNode(next.getSourceLocation());
        } else if (next.type == NUMBER) {
            result = parseNumber(next);
        } else {
            result = parseNodeValue(next);
        }
        expect(RPAREN);

        return new TraitEntry(traitName, result, false);
    }

    private Node parseNode() {
        return parseNodeValue(expect(LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER));
    }

    private Node parseNodeValue(Token token) {
        switch (token.type) {
            case LBRACE: return parseObjectNode(token.getSourceLocation(), RBRACE);
            case LBRACKET: return parseArrayNode(token.getSourceLocation());
            case QUOTED: return new StringNode(token.lexeme, token.getSourceLocation());
            case NUMBER: return parseNumber(token);
            case UNQUOTED: return parseUnquotedNode(token);
            default: throw new IllegalStateException("Parse node value not expected to be called with: " + token);
        }
    }

    private Node parseUnquotedNode(Token token) {
        switch (token.lexeme) {
            case "true": return new BooleanNode(true, token.getSourceLocation());
            case "false": return new BooleanNode(false, token.getSourceLocation());
            case "null": return new NullNode(token.getSourceLocation());
            default:
                // Unquoted node values syntactically are assumed to be references
                // to shapes. A lazy string node is used because the shape ID may
                // not be able to be resolved until after the entire model is loaded.
                Pair<StringNode, Consumer<String>> pair = StringNode.createLazyString(
                        token.lexeme, token.getSourceLocation());
                Consumer<String> consumer = pair.right;
                onShapeTarget(token.lexeme, token, id -> consumer.accept(id.toString()));
                return pair.left;
        }
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
    private void onShapeTarget(String target, FromSourceLocation sourceLocation, Consumer<ShapeId> resolver) {
        // Account for aliased shapes.
        if (useShapes.containsKey(target)) {
            resolver.accept(useShapes.get(target));
            return;
        }

        try {
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
        } catch (ShapeIdSyntaxException e) {
            throw new SourceException("Error resolving shape target; " + e.getMessage(), sourceLocation, e);
        }
    }

    private NumberNode parseNumber(Token token) {
        if (token.lexeme.contains("e") || token.lexeme.contains(".")) {
            return new NumberNode(Double.valueOf(token.lexeme), token.getSourceLocation());
        } else {
            return new NumberNode(Long.parseLong(token.lexeme), token.getSourceLocation());
        }
    }

    private ObjectNode parseObjectNode(SourceLocation location, TokenType closing) {
        return parseObjectNodeWithKey(location, closing, expect(QUOTED, UNQUOTED, closing));
    }

    private ObjectNode parseObjectNodeWithKey(SourceLocation sloc, TokenType closing, Token key) {
        Map<StringNode, Node> entries = new LinkedHashMap<>();
        // Prevents entering the loop if immediately closed, and this checks for trailing commas.
        while (key.type != closing) {
            expect(COLON);
            Node value = parseNode();
            entries.put(new StringNode(key.lexeme, key.getSourceLocation()), value);
            if (expect(closing, COMMA).type == closing) {
                break;
            }
            key = expect(closing, QUOTED, UNQUOTED);
        }

        return new ObjectNode(entries, sloc);
    }

    private ArrayNode parseArrayNode(SourceLocation location) {
        List<Node> values = new ArrayList<>();
        Token next = expect(LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER, RBRACKET);
        // Checks initially and does a double-check for trailing commas.
        while (next.type != RBRACKET) {
            values.add(parseNodeValue(next));
            next = expect(RBRACKET, COMMA);
            if (next.type == RBRACKET) {
                break;
            }
            next = expect(RBRACKET, LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER);
        }

        return new ArrayNode(values, location);
    }

    private void parseSimpleShape(AbstractShapeBuilder builder) {
        builder.source(currentLocation());
        builder.id(parseShapeName());
        visitor.onShape(builder);
        expectNewline();
    }

    /**
     * Expects that the next token is a valid shape name and creates a
     * shape ID. A namespace must have been set before parsing a shape name.
     * Any traits that were defined before the shape are applied to the
     * parsed shape ID.
     *
     * @return Returns the parsed shape ID.
     */
    private ShapeId parseShapeName() {
        requireNamespaceOrThrow();
        definedShapes = true;
        Token nameToken = expect(UNQUOTED);
        String name = nameToken.lexeme;

        if (useShapes.containsKey(name)) {
            String msg = String.format("shape name `%s` conflicts with imported shape `%s`",
                                       name, useShapes.get(name));
            throw new UseException(msg, nameToken);
        }

        try {
            ShapeId id = ShapeId.fromRelative(namespace, name);
            for (TraitEntry traitEntry : pendingTraits) {
                onDeferredTrait(id, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
            }
            pendingTraits.clear();
            collectPendingDocString(id);
            return id;
        } catch (ShapeIdSyntaxException e) {
            throw new ModelSyntaxException("Invalid shape name: " + name, nameToken);
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

    private boolean isRealizedShapeId(ShapeId expectedId, String target) {
        return Objects.equals(namespace, Prelude.NAMESPACE)
               || visitor.hasDefinedShape(expectedId)
               || target.contains("#");
    }

    private void collectPendingDocString(ShapeId id) {
        if (pendingDocComment != null) {
            Node value = new StringNode(pendingDocComment.content, pendingDocComment.sourceLocation);
            pendingDocComment = null;
            visitor.onTrait(id, new DocumentationTrait(value.toString(), value.getSourceLocation()));
        }
    }

    private void parseStructuredShape(String shapeType, AbstractShapeBuilder builder) {
        builder.source(currentLocation());
        ShapeId id = parseShapeName();
        visitor.onShape(builder.id(id));
        parseStructuredBody(shapeType, id);
    }

    private void parseStructuredBody(String shapeType, ShapeId parent) {
        parseStructuredContents(shapeType, parent, SetUtils.of());
        expectNewline();
    }

    private void parseStructuredContents(String shapeType, ShapeId parent, Collection<String> requiredMembers) {
        expect(LBRACE);
        List<TraitEntry> memberTraits = new ArrayList<>();
        Set<String> remainingMembers = requiredMembers.isEmpty() ? SetUtils.of() : new HashSet<>(requiredMembers);

        Token token = expect(ANNOTATION, QUOTED, UNQUOTED, RBRACE, DOC);
        while (token.type != RBRACE) {
            if (token.type == ANNOTATION) {
                memberTraits.add(parseTraitValue(token, TraitValueType.MEMBER));
                // Traits can't come before a closing brace, so continue
                // to make sure they come before another trait or a key.
            } else if (token.type == DOC) {
                parseDocComment(token, true);
            } else {
                String memberName = token.lexeme;
                if (!requiredMembers.isEmpty()) {
                    if (!requiredMembers.contains(memberName)) {
                        throw syntax(format(
                                "Invalid member `%s` found in %s shape `%s`. Expected one of the following "
                                + "members: [%s]",
                                memberName, shapeType, parent, ValidationUtils.tickedList(requiredMembers)));
                    }
                    remainingMembers.remove(memberName);
                }

                ShapeId memberId = parent.withMember(memberName);
                expect(COLON);
                parseMember(memberId);
                // Add the loaded traits on the member now that the ID is known.
                for (TraitEntry traitEntry : memberTraits) {
                    onDeferredTrait(memberId, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
                }
                memberTraits.clear();
                collectPendingDocString(memberId);

                if (expect(COMMA, RBRACE).type == RBRACE) {
                    break;
                }
            }

            token = expect(ANNOTATION, QUOTED, UNQUOTED, RBRACE, DOC);
        }

        if (!remainingMembers.isEmpty()) {
            throw syntax(format(
                    "Missing required members of %s shape `%s`: [%s]",
                    shapeType, parent, ValidationUtils.tickedList(remainingMembers)));
        }
    }

    private void parseMember(ShapeId memberId) {
        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(currentLocation());
        visitor.onShape(memberBuilder);
        Token targetToken = expect(UNQUOTED, QUOTED);
        onShapeTarget(targetToken.lexeme, targetToken, memberBuilder::target);
    }

    private void parseCollection(String shapeType, CollectionShape.Builder builder) {
        // list Foo { member: Bar }
        builder.source(currentLocation());
        ShapeId id = parseShapeName();
        parseStructuredContents(shapeType, id, SetUtils.of("member"));
        visitor.onShape(builder.id(id));
        expectNewline();
    }

    private void parseMap() {
        // map Foo { key: Bar, value: Baz }
        SourceLocation sourceLocation = currentLocation();
        ShapeId id = parseShapeName();
        MapShape.Builder builder = MapShape.builder()
                .id(id)
                .source(sourceLocation);

        parseStructuredContents("map", id, MAP_KEYS);
        visitor.onShape(builder);
        expectNewline();
    }

    private void parseApply() {
        requireNamespaceOrThrow();
        // apply <ShapeName> @<trait>\n
        Token nextToken = expect(UNQUOTED);
        String name = nextToken.lexeme;
        Token token = expect(ANNOTATION);
        TraitEntry traitEntry = parseTraitValue(token, TraitValueType.APPLY);
        expectNewline();

        // First, resolve the targeted shape.
        onShapeTarget(name, nextToken.getSourceLocation(), id -> {
            // Next, resolve the trait ID.
            onDeferredTrait(id, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
        });
    }

    /**
     * @return Returns true if the parser is at the last token.
     */
    private boolean eof() {
        return !lexer.hasNext();
    }

    /**
     * @return Gets the last valid token.
     */
    private Token current() {
        return Objects.requireNonNull(current, "Call to next must occur before accessing current");
    }

    /**
     * @return Get the current source location.
     */
    private SourceLocation currentLocation() {
        return current.getSourceLocation();
    }

    /**
     * Expects that there is a next token and returns it.
     *
     * @return Returns the next token.
     * @throws ModelSyntaxException if there is no next token.
     */
    private Token next() {
        if (!lexer.hasNext()) {
            throw syntax("Unexpected EOF", current != null ? current.span : 0);
        }

        current = lexer.next();
        return current;
    }

    /**
     * Expects that there is a next token and that it is one of the
     * given token types.
     *
     * @param tokens Token types to expect.
     * @return Returns the next token.
     * @throws ModelSyntaxException if the next token is invalid.
     */
    private Token expect(TokenType... tokens) {
        Token next = next();
        for (TokenType type : tokens) {
            if (type == next.type) {
                return next;
            }
        }

        if (next.type != ERROR) {
            throw syntax(tokens.length == 1
                         ? "Expected " + tokens[0]
                         : "Expected one of " + Arrays.toString(tokens));
        } else if (next.lexeme.contains("'") || next.lexeme.contains("\"")) {
            throw syntax("Unexpected syntax. Perhaps an unclosed quote?");
        } else if (next.errorMessage == null) {
            throw syntax("Unexpected syntax");
        } else {
            throw syntax(next.errorMessage);
        }
    }

    /**
     * Tests if a lookahead token matches the given type.
     *
     * @param type Token type to test for.
     * @return Returns true if the next token is of the given type.
     */
    private boolean test(TokenType type) {
        return !eof() && lexer.peek().type == type;
    }

    /**
     * Peeks at the next token.
     *
     * @return Returns the optionally present next token.
     */
    private Optional<Token> peek() {
        return Optional.ofNullable(lexer.peek());
    }

    /**
     * Expects either EOF or that the next token is on a new line.
     */
    private void expectNewline() {
        if (peek().isPresent() && peek().get().line == current().line) {
            next();
            throw syntax("Expected a new line before this token");
        }
    }

    /**
     * Creates a syntax error using the provided message.
     *
     * @param message Syntax error message.
     * @return Returns the created syntax error.
     */
    private ModelSyntaxException syntax(String message) {
        return syntax(message, 0);
    }

    private ModelSyntaxException syntax(String message, int offset) {
        Token token = current();
        int line = token.line;
        int column = token.column + offset;
        String lexeme = token.lexeme;
        String formatted = format(
                "Parse error at line %d, column %d near `%s`: %s", line, column, lexeme, message);
        return new ModelSyntaxException(formatted, new SourceLocation(filename, line, column));
    }

    private void parseService() {
        SourceLocation sourceLocation = currentLocation();
        ShapeId shapeId = parseShapeName();
        ServiceShape.Builder builder = new ServiceShape.Builder()
                .id(shapeId)
                .source(sourceLocation);

        ObjectNode shapeNode = parseObjectNode(expect(LBRACE).getSourceLocation(), RBRACE);
        visitor.checkForAdditionalProperties(shapeNode, shapeId, SERVICE_PROPERTY_NAMES);
        builder.version(shapeNode.expectStringMember(VERSION_KEY).getValue());
        optionalIdList(shapeNode, shapeId.getNamespace(), OPERATIONS_KEY).forEach(builder::addOperation);
        optionalIdList(shapeNode, shapeId.getNamespace(), RESOURCES_KEY).forEach(builder::addResource);
        visitor.onShape(builder);
        expectNewline();
    }

    static Optional<ShapeId> optionalId(ObjectNode node, String namespace, String name) {
        return node.getStringMember(name).map(stringNode -> stringNode.expectShapeId(namespace));
    }

    static List<ShapeId> optionalIdList(ObjectNode node, String namespace, String name) {
        return node.getArrayMember(name)
                .map(array -> array.getElements().stream()
                        .map(Node::expectStringNode)
                        .map(s -> s.expectShapeId(namespace))
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    private void parseResource() {
        SourceLocation sourceLocation = currentLocation();
        ShapeId shapeId = parseShapeName();
        ResourceShape.Builder builder = ResourceShape.builder().id(shapeId).source(sourceLocation);
        visitor.onShape(builder);
        ObjectNode shapeNode = parseObjectNode(expect(LBRACE).getSourceLocation(), RBRACE);

        visitor.checkForAdditionalProperties(shapeNode, shapeId, RESOURCE_PROPERTY_NAMES);
        optionalId(shapeNode, shapeId.getNamespace(), PUT_KEY).ifPresent(builder::put);
        optionalId(shapeNode, shapeId.getNamespace(), CREATE_KEY).ifPresent(builder::create);
        optionalId(shapeNode, shapeId.getNamespace(), READ_KEY).ifPresent(builder::read);
        optionalId(shapeNode, shapeId.getNamespace(), UPDATE_KEY).ifPresent(builder::update);
        optionalId(shapeNode, shapeId.getNamespace(), DELETE_KEY).ifPresent(builder::delete);
        optionalId(shapeNode, shapeId.getNamespace(), LIST_KEY).ifPresent(builder::list);
        optionalIdList(shapeNode, shapeId.getNamespace(), OPERATIONS_KEY).forEach(builder::addOperation);
        optionalIdList(shapeNode, shapeId.getNamespace(), RESOURCES_KEY).forEach(builder::addResource);
        optionalIdList(shapeNode, shapeId.getNamespace(), COLLECTION_OPERATIONS_KEY)
                .forEach(builder::addCollectionOperation);

        // Load identifiers and resolve forward references.
        shapeNode.getObjectMember(IDENTIFIERS_KEY).ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                onShapeTarget(target.getValue(), target, id -> builder.addIdentifier(name, id));
            }
        });

        expectNewline();
    }

    private void parseOperation() {
        SourceLocation sourceLocation = currentLocation();
        ShapeId id = parseShapeName();
        OperationShape.Builder builder = OperationShape.builder().id(id).source(sourceLocation);
        visitor.onShape(builder);

        Token opening = expect(LBRACE);
        ObjectNode node = parseObjectNode(opening.getSourceLocation(), RBRACE);
        visitor.checkForAdditionalProperties(node, id, OPERATION_PROPERTY_NAMES);
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

        expectNewline();
    }
}
