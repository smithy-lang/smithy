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
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.RETURN;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.RPAREN;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.UNQUOTED;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    private final List<Pair<String, Node>> pendingTraits = new ArrayList<>();
    private final Set<VersionFeature> features = new HashSet<>();

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
        visitor.onOpenFile(filename);

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

    enum VersionFeature {
        ALLOW_USE_BEFORE_NAMESPACE {
            @Override
            void validate(IdlModelLoader loader) {
                if (loader.namespace == null && !loader.features.contains(this)) {
                    raise(loader, "Use statements must appear after a namespace is defined");
                }
            }
        },

        ALLOW_MULTIPLE_NAMESPACES {
            @Override
            void validate(IdlModelLoader loader) {
                if (loader.namespace != null && !loader.features.contains(this)) {
                    raise(loader, format(
                            "Only a single namespace can be declared per/file. The previous namespace was "
                            + "set to `%s`.", loader.namespace));
                }
            }
        },

        ALLOW_METADATA_AFTER_NAMESPACE {
            @Override
            void validate(IdlModelLoader loader) {
                if (loader.namespace != null && !loader.features.contains(this)) {
                    raise(loader, "Metadata statements must appear before a namespace statement");
                }
            }
        },

        ALLOW_MULTIPLE_VERSIONS {
            @Override
            void validate(IdlModelLoader loader) {
                if (loader.definedVersion != null && !loader.features.contains(this)) {
                    raise(loader, "Cannot define multiple versions in the same file");
                }
            }
        };

        abstract void validate(IdlModelLoader loader);

        // Halts parsing if a version has been explicitly set, otherwise
        // adds a WARNING event.
        void raise(IdlModelLoader loader, String message) {
            if (loader.definedVersion != null) {
                throw loader.syntax(message);
            }

            loader.visitor.onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .severity(Severity.WARNING)
                    .sourceLocation(loader.current)
                    .message("Detected deprecated IDL features that will break in future versions "
                             + "of Smithy: " + message)
                    .build());
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
        VersionFeature.ALLOW_MULTIPLE_NAMESPACES.validate(this);
        String parsedNamespace = expect(UNQUOTED).lexeme;

        if (!ShapeId.isValidNamespace(parsedNamespace)) {
            throw syntax(format("Invalid namespace name `%s`", parsedNamespace));
        }

        visitor.onNamespace(parsedNamespace, current());
        this.namespace = parsedNamespace;
    }

    private void parseUseStatement() {
        VersionFeature.ALLOW_USE_BEFORE_NAMESPACE.validate(this);

        if (definedShapes) {
            throw syntax("A use statement must come before any shape definition");
        }

        try {
            Token namespaceToken = expect(UNQUOTED);
            visitor.onUseShape(ShapeId.from(namespaceToken.lexeme), namespaceToken);
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
        VersionFeature.ALLOW_MULTIPLE_VERSIONS.validate(this);

        if (!SmithyVersion.isSupported(parsedVersion)) {
            throw syntax(format("Invalid Smithy version number: %s", parsedVersion));
        }

        definedVersion = parsedVersion;

        // Enable old Smithy 0.4.0 features that were removed in 0.5.0.
        if (definedVersion.equals(SmithyVersion.VERSION_0_4_0.value)) {
            features.add(VersionFeature.ALLOW_USE_BEFORE_NAMESPACE);
            features.add(VersionFeature.ALLOW_MULTIPLE_NAMESPACES);
            features.add(VersionFeature.ALLOW_METADATA_AFTER_NAMESPACE);
            features.add(VersionFeature.ALLOW_MULTIPLE_VERSIONS);
        }
    }

    private void parseMetadata() {
        VersionFeature.ALLOW_METADATA_AFTER_NAMESPACE.validate(this);
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

    private Pair<String, Node> parseTraitValue(Token token, TraitValueType type) {
        try {
            requireNamespaceOrThrow();

            // Resolve the trait name and ensure that the trait forms a syntactically valid value.
            ShapeId.fromOptionalNamespace(visitor.getNamespace(), token.lexeme);
            Pair<String, Node> result = Pair.of(token.lexeme, parseTraitValueBody());

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

    private Node parseTraitValueBody() {
        // Null is coerced into the appropriate type for the trait.
        if (!test(LPAREN)) {
            return new NullNode(currentLocation());
        }

        expect(LPAREN);
        Token next = expect(RPAREN, QUOTED, UNQUOTED, LBRACKET, NUMBER);

        if (next.type == RPAREN) {
            // An open and closed "()" signals an empty object.
            return new ObjectNode(MapUtils.of(), next.getSourceLocation());
        }

        // Test to see if this is just a string or if it's an object.
        if (test(COLON)) {
            if (next.type == QUOTED || next.type == UNQUOTED) {
                // Parse the object using the already parsed key.
                return parseObjectNodeWithKey(currentLocation(), RPAREN, next);
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

        return result;
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
                visitor.onShapeTarget(token.lexeme, token, id -> consumer.accept(id.toString()));
                return pair.left;
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
        definedShapes = true;
        Token nameToken = expect(UNQUOTED);
        String name = nameToken.lexeme;
        ShapeId id = visitor.onShapeDefName(name, nameToken);
        pendingTraits.forEach(pair -> visitor.onTrait(id, pair.getLeft(), pair.getRight()));
        pendingTraits.clear();
        collectPendingDocString(id);
        return id;
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
        List<Pair<String, Node>> memberTraits = new ArrayList<>();
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
                for (Pair<String, Node> pair : memberTraits) {
                    visitor.onTrait(memberId, pair.getLeft(), pair.getRight());
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
        visitor.onShapeTarget(targetToken.lexeme, targetToken, memberBuilder::target);
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
        String name = expect(UNQUOTED).lexeme;
        ShapeId id = ShapeId.fromOptionalNamespace(visitor.getNamespace(), name);
        Token token = expect(ANNOTATION);
        Pair<String, Node> trait = parseTraitValue(token, TraitValueType.APPLY);
        visitor.onTrait(id, trait.getLeft(), trait.getRight());
        expectNewline();
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
        shapeNode.warnIfAdditionalProperties(LoaderUtils.SERVICE_PROPERTY_NAMES);
        LoaderUtils.loadServiceObject(builder, shapeId, shapeNode);
        visitor.onShape(builder);
        expectNewline();
    }

    private void parseResource() {
        SourceLocation sourceLocation = currentLocation();
        ShapeId shapeId = parseShapeName();
        ResourceShape.Builder builder = ResourceShape.builder().id(shapeId).source(sourceLocation);
        visitor.onShape(builder);
        ObjectNode shapeNode = parseObjectNode(expect(LBRACE).getSourceLocation(), RBRACE);
        shapeNode.warnIfAdditionalProperties(LoaderUtils.RESOURCE_PROPERTY_NAMES);
        LoaderUtils.loadResourceObject(builder, shapeId, shapeNode, visitor);
        expectNewline();
    }

    private void parseOperation() {
        SourceLocation sourceLocation = currentLocation();
        ShapeId id = parseShapeName();
        OperationShape.Builder builder = OperationShape.builder().id(id).source(sourceLocation);
        visitor.onShape(builder);

        Token opening = expect(LPAREN, LBRACE);
        if (opening.type == LPAREN) {
            parseDeprecatedOperationSyntax(builder);
        } else {
            ObjectNode node = parseObjectNode(opening.getSourceLocation(), RBRACE);
            node.expectNoAdditionalProperties(OPERATION_PROPERTY_NAMES);
            node.getStringMember("input").ifPresent(input -> {
                visitor.onShapeTarget(input.getValue(), input, builder::input);
            });
            node.getStringMember("output").ifPresent(output -> {
                visitor.onShapeTarget(output.getValue(), output, builder::output);
            });
            node.getArrayMember("errors").ifPresent(errors -> {
                for (StringNode value : errors.getElementsAs(StringNode.class)) {
                    visitor.onShapeTarget(value.getValue(), value, builder::addError);
                }
            });
        }

        expectNewline();
    }

    private void parseDeprecatedOperationSyntax(OperationShape.Builder builder) {
        visitor.onError(ValidationEvent.builder()
                .eventId(Validator.MODEL_ERROR)
                .sourceLocation(builder.getSourceLocation())
                .shapeId(builder.getId())
                .message("Deprecated IDL syntax detected for operation definition")
                .severity(Severity.WARNING)
                .build());

        Token next = expect(RPAREN, UNQUOTED);
        if (next.type == UNQUOTED) {
            visitor.onShapeTarget(next.lexeme, next, builder::input);
            expect(RPAREN);
        }

        // Parse the optionally present return value.
        if (test(RETURN)) {
            expect(RETURN);
            Token returnToken = expect(UNQUOTED);
            visitor.onShapeTarget(returnToken.lexeme, returnToken, builder::output);
        }

        // Parse the optionally present errors list.
        if (peek().filter(token -> token.lexeme.equals("errors")).isPresent()) {
            next(); // Skip the error.
            expect(LBRACKET);
            if (!test(RBRACKET)) {
                while (true) {
                    Token errorToken = expect(UNQUOTED);
                    visitor.onShapeTarget(errorToken.lexeme, errorToken, builder::addError);
                    if (test(RBRACKET)) {
                        break;
                    }
                    expect(COMMA);
                }
            }
            expect(RBRACKET);
        }
    }
}
