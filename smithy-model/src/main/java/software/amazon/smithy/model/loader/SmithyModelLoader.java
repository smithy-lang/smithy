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

import static software.amazon.smithy.model.loader.SmithyModelLexer.Token;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.ANNOTATION;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.COLON;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.COMMA;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.DOC;
import static software.amazon.smithy.model.loader.SmithyModelLexer.TokenType.DOLLAR;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

/**
 * Parses custom Smithy model IDL syntax and invokes a LoaderVisitor
 * to load a model.
 */
final class SmithyModelLoader implements ModelLoader {
    private static final Collection<String> MAP_KEYS = ListUtils.of("key", "value");

    /** Top-level statements that can be encountered while parsing. */
    private static final Map<String, Consumer<State>> STATEMENTS = new HashMap<>();

    static {
        STATEMENTS.put("namespace", SmithyModelLoader::parseNamespace);
        STATEMENTS.put("service", SmithyModelLoader::parseService);
        STATEMENTS.put("operation", SmithyModelLoader::parseOperation);
        STATEMENTS.put("resource", SmithyModelLoader::parseResource);
        STATEMENTS.put("structure", state -> parseStructuredShape(state, "structure", StructureShape.builder()));
        STATEMENTS.put("union", state -> parseStructuredShape(state, "union", UnionShape.builder()));
        STATEMENTS.put("list", state -> parseCollection(state, "list", ListShape.builder()));
        STATEMENTS.put("set", state -> parseCollection(state, "set", SetShape.builder()));
        STATEMENTS.put("map", SmithyModelLoader::parseMap);
        STATEMENTS.put("boolean", state -> parseSimpleShape(state, BooleanShape.builder()));
        STATEMENTS.put("string", state -> parseSimpleShape(state, StringShape.builder()));
        STATEMENTS.put("blob", state -> parseSimpleShape(state, BlobShape.builder()));
        STATEMENTS.put("byte", state -> parseSimpleShape(state, ByteShape.builder()));
        STATEMENTS.put("short", state -> parseSimpleShape(state, ShortShape.builder()));
        STATEMENTS.put("integer", state -> parseSimpleShape(state, IntegerShape.builder()));
        STATEMENTS.put("long", state -> parseSimpleShape(state, LongShape.builder()));
        STATEMENTS.put("float", state -> parseSimpleShape(state, FloatShape.builder()));
        STATEMENTS.put("document", state -> parseSimpleShape(state, DocumentShape.builder()));
        STATEMENTS.put("double", state -> parseSimpleShape(state, DoubleShape.builder()));
        STATEMENTS.put("bigInteger", state -> parseSimpleShape(state, BigIntegerShape.builder()));
        STATEMENTS.put("bigDecimal", state -> parseSimpleShape(state, BigDecimalShape.builder()));
        STATEMENTS.put("timestamp", state -> parseSimpleShape(state, TimestampShape.builder()));
        STATEMENTS.put("metadata", SmithyModelLoader::parseMetadata);
        STATEMENTS.put("apply", SmithyModelLoader::parseApply);
        STATEMENTS.put("trait", SmithyModelLoader::parseTraitDefinition);
    }

    private static final Set<String> SUPPORTS_TRAITS = SetUtils.of(
            "bigDecimal", "bigInteger", "blob", "boolean", "byte", "document",
            "double", "float", "integer", "list", "long", "map", "operation", "resource",
            "service", "set", "short", "string", "structure", "timestamp", "union");

    private static final Set<String> SUPPORTS_DOCS = new HashSet<>(SUPPORTS_TRAITS);

    static {
        SUPPORTS_DOCS.add("trait");
    }


    @Override
    public boolean load(String path, Supplier<String> contentSupplier, LoaderVisitor visitor) {
        if (!path.endsWith(".smithy")) {
            return false;
        }

        SmithyModelLexer lexer = new SmithyModelLexer(contentSupplier.get());
        State state = new State(path, lexer, visitor);
        parse(state);
        return true;
    }

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

    /**
     * Encapsulates all of the parsing state.
     */
    private static final class State {
        final String filename;
        final SmithyModelLexer lexer;
        final List<Pair<String, Node>> pendingTraits = new ArrayList<>();
        final LoaderVisitor visitor;
        boolean definedMetadata;
        Token current;
        String namespace;
        DocComment pendingDocComment;

        State(String filename, SmithyModelLexer lexer, LoaderVisitor visitor) {
            this.filename = filename;
            this.lexer = lexer;
            this.visitor = visitor;
        }

        /**
         * @return Returns true if the parser is at the last token.
         */
        boolean eof() {
            return !lexer.hasNext();
        }

        /**
         * @return Gets the last valid token.
         */
        Token current() {
            return Objects.requireNonNull(current, "Call to next must occur before accessing current");
        }

        /**
         * Expects that there is a next token and returns it.
         *
         * @return Returns the next token.
         * @throws ModelSyntaxException if there is no next token.
         */
        Token next() {
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
        Token expect(TokenType... tokens) {
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
            } else {
                throw syntax("Unexpected syntax");
            }
        }

        /**
         * Tests if a lookahead token matches the given type.
         *
         * @param type Token type to test for.
         * @return Returns true if the next token is of the given type.
         */
        boolean test(TokenType type) {
            return !eof() && lexer.peek().type == type;
        }

        /**
         * Peeks at the next token.
         *
         * @return Returns the optionally present next token.
         */
        Optional<Token> peek() {
            return Optional.ofNullable(lexer.peek());
        }

        /**
         * Expects either EOF or that the next token is on a new line.
         */
        void expectNewline() {
            peek().filter(nextToken -> nextToken.line == current().line).ifPresent(nextToken -> {
                next();
                throw syntax("Expected a new line before this token");
            });
        }

        /**
         * Creates a syntax error using the provided message.
         *
         * @param message Syntax error message.
         * @return Returns the created syntax error.
         */
        ModelSyntaxException syntax(String message) {
            return syntax(message, 0);
        }

        ModelSyntaxException syntax(String message, int offset) {
            Token token = current();
            int line = token.line;
            int column = token.column + offset;
            String lexeme = token.lexeme;
            String formatted = String.format(
                    "Parse error at line %d, column %d near `%s`: %s", line, column, lexeme, message);
            return new ModelSyntaxException(formatted, new SourceLocation(filename, line, column));
        }
    }

    private static void parse(State state) {
        while (!state.eof()) {
            Token token = state.expect(UNQUOTED, ANNOTATION, DOLLAR, DOC);
            if (token.type == UNQUOTED) {
                parseStatement(token, state);
            } else if (token.type == ANNOTATION) {
                state.pendingTraits.add(parseTraitValue(token, state, false));
            } else if (token.type == DOLLAR) {
                parseControlStatement(state);
            } else if (token.type == DOC) {
                parseDocComment(state, token, false);
            }
        }
    }

    private static void parseStatement(Token token, State state) {
        if (!STATEMENTS.containsKey(token.lexeme)) {
            throw state.syntax(String.format("Expected one of %s", ValidationUtils.tickedList(STATEMENTS.keySet())));
        }

        STATEMENTS.get(token.lexeme).accept(state);
    }

    private static void parseControlStatement(State state) {
        if (state.namespace != null || state.definedMetadata) {
            throw state.syntax("A control statement must come before any namespace, metadata, or shape");
        }

        String key = state.expect(UNQUOTED, QUOTED).lexeme;
        state.expect(COLON);
        Node value = parseNodeValue(state, state.next());

        switch (key) {
            case "version":
                if (!value.isStringNode()) {
                    value.expectStringNode("The $version control statement must have a string value, but found "
                                           + Node.printJson(value));
                }
                state.visitor.onVersion(sourceFromToken(state, state.current), value.expectStringNode().getValue());
                break;
            default:
                state.visitor.onError(ValidationEvent.builder()
                        .eventId(Validator.MODEL_ERROR)
                        .sourceLocation(value)
                        .severity(Severity.WARNING)
                        .message(String.format(
                                "Unknown control statement `%s` with value `%s", key, Node.printJson(value)))
                        .build());
                break;
        }
    }

    private static void parseDocComment(State state, Token token, boolean memberScope) {
        StringBuilder builder = new StringBuilder(token.getDocContents());
        while (state.peek().filter(tok -> tok.type == DOC).isPresent()) {
            builder.append('\n').append(state.next().getDocContents());
        }

        state.pendingDocComment = new DocComment(builder.toString(), sourceFromToken(state, token));

        if (!state.peek().isPresent()) {
            throw state.syntax("Found a documentation comment that doesn't document anything");
        }

        Token next = state.peek().get();
        if (next.type != ANNOTATION
                && (next.type != UNQUOTED || (!memberScope && !SUPPORTS_DOCS.contains(next.lexeme)))) {
            throw state.syntax("Documentation cannot be applied to `" + next.lexeme + "`");
        }
    }

    private static void parseNamespace(State state) {
        String namespace = state.expect(UNQUOTED).lexeme;
        if (!ShapeId.VALID_NAMESPACE.matcher(namespace).find()) {
            throw state.syntax("Invalid namespace name. Namespaces must match the following regular expression: "
                               + ShapeId.VALID_NAMESPACE);
        }

        state.namespace = namespace;
    }

    private static void requireNamespaceOrThrow(State state) {
        if (state.namespace == null) {
            throw state.syntax("A namespace must be set before shapes or traits can be defined");
        }
    }

    private static Pair<String, Node> parseTraitValue(Token token, State state, boolean memberScope) {
        requireNamespaceOrThrow(state);

        try {
            // Ensure that the trait forms a syntactically valid value.
            ShapeId.fromOptionalNamespace(state.namespace, token.lexeme);
            Pair<String, Node> result = Pair.of(token.lexeme, parseTraitValueBody(state));

            if (!state.peek().isPresent()) {
                throw state.syntax("Found a trait doesn't apply to anything");
            }

            Token next = state.peek().get();
            if (next.type != ANNOTATION
                && (next.type != UNQUOTED || (!memberScope && !SUPPORTS_TRAITS.contains(next.lexeme)))) {
                throw state.syntax("Traits cannot be applied to `" + next.lexeme + "`");
            }

            return result;
        } catch (ShapeIdSyntaxException e) {
            throw state.syntax("Invalid trait name syntax. Trait names must adhere to the same syntax as shape IDs.");
        }
    }

    private static Node parseTraitValueBody(State state) {
        // Null is coerced into the appropriate type for the trait.
        if (!state.test(LPAREN)) {
            return new NullNode(sourceFromToken(state, state.current()));
        }

        state.expect(LPAREN);
        Token next = state.expect(RPAREN, QUOTED, UNQUOTED, LBRACKET, NUMBER);

        if (next.type == RPAREN) {
            // An open and closed "()" signals an empty object.
            return new ObjectNode(MapUtils.of(), sourceFromToken(state, next));
        }

        // Test to see if this is just a string or if it's an object.
        if (state.test(COLON)) {
            if (next.type == QUOTED || next.type == UNQUOTED) {
                // Parse the object using the already parsed key.
                return parseObjectNodeWithKey(state, sourceFromToken(state, state.current()), RPAREN, next);
            }
            throw state.syntax("Expected a string to start a trait value object");
        }

        Node result;
        if (next.type == LBRACKET) {
            result = parseArrayNode(state, sourceFromToken(state, next));
        } else if (next.type == NUMBER) {
            result = parseNumber(state, next);
        } else {
            result = parseNodeValue(state, next);
        }
        state.expect(RPAREN);

        return result;
    }

    /**
     * Expects that the next token is a valid shape name and creates a
     * shape ID. A namespace must have been set before parsing a shape name.
     * Any traits that were defined before the shape are applied to the
     * parsed shape ID.
     *
     * @param state Parse state.
     * @return Returns the parsed shape ID.
     */
    private static ShapeId parseShapeName(State state) {
        requireNamespaceOrThrow(state);
        String name = state.expect(UNQUOTED).lexeme;
        try {
            ShapeId id = ShapeId.fromOptionalNamespace(state.namespace, name);
            state.pendingTraits.forEach(pair -> state.visitor.onTrait(
                    id, state.namespace, pair.getLeft(), pair.getRight()));
            state.pendingTraits.clear();
            collectPendingDocString(state, id);
            return id;
        } catch (ShapeIdSyntaxException e) {
            throw state.syntax(e.getMessage());
        }
    }

    private static void collectPendingDocString(State state, ShapeId id) {
        if (state.pendingDocComment != null) {
            Node value = new StringNode(state.pendingDocComment.content, state.pendingDocComment.sourceLocation);
            state.pendingDocComment = null;
            state.visitor.onTrait(id, state.namespace, DocumentationTrait.NAME, value);
        }
    }

    private static void parseSimpleShape(State state, AbstractShapeBuilder builder) {
        builder.source(currentSourceLocation(state));
        builder.id(parseShapeName(state));
        state.visitor.onShape(builder);
        state.expectNewline();
    }

    private static void parseService(State state) {
        SourceLocation sourceLocation = currentSourceLocation(state);
        ShapeId shapeId = parseShapeName(state);
        ServiceShape.Builder builder = new ServiceShape.Builder()
                .id(shapeId)
                .source(sourceLocation);

        ObjectNode shapeNode = parseObjectNode(state, sourceFromToken(state, state.expect(LBRACE)), RBRACE);
        shapeNode.warnIfAdditionalProperties(LoaderUtils.SERVICE_PROPERTY_NAMES);
        LoaderUtils.loadServiceObject(builder, shapeId, shapeNode);
        state.visitor.onShape(builder);
        state.expectNewline();
    }

    private static void parseResource(State state) {
        SourceLocation sourceLocation = currentSourceLocation(state);
        ShapeId shapeId = parseShapeName(state);
        ResourceShape.Builder builder = ResourceShape.builder().id(shapeId).source(sourceLocation);
        state.visitor.onShape(builder);
        ObjectNode shapeNode = parseObjectNode(state, sourceFromToken(state, state.expect(LBRACE)), RBRACE);
        shapeNode.warnIfAdditionalProperties(LoaderUtils.RESOURCE_PROPERTY_NAMES);
        LoaderUtils.loadResourceObject(builder, shapeId, shapeNode, state.visitor);
        state.expectNewline();
    }

    private static void parseOperation(State state) {
        SourceLocation sourceLocation = currentSourceLocation(state);
        ShapeId id = parseShapeName(state);
        OperationShape.Builder builder = OperationShape.builder().id(id).source(sourceLocation);
        state.visitor.onShape(builder);

        // Parse the optionally present input target.
        state.expect(LPAREN);
        Token next = state.expect(RPAREN, UNQUOTED);
        if (next.type == UNQUOTED) {
            builder.input(ShapeId.fromOptionalNamespace(state.namespace, next.lexeme));
            state.expect(RPAREN);
        }

        // Parse the optionally present return value.
        if (state.test(RETURN)) {
            state.expect(RETURN);
            builder.output(ShapeId.fromOptionalNamespace(state.namespace, state.expect(UNQUOTED).lexeme));
        }

        // Parse the optionally present errors list.
        if (state.peek().filter(token -> token.lexeme.equals("errors")).isPresent()) {
            state.next(); // Skip the error.
            state.expect(LBRACKET);
            if (!state.test(RBRACKET)) {
                while (true) {
                    ShapeId error = ShapeId.fromOptionalNamespace(state.namespace, state.expect(UNQUOTED).lexeme);
                    builder.addError(error);
                    if (state.test(RBRACKET)) {
                        break;
                    }
                    state.expect(COMMA);
                }
            }
            state.expect(RBRACKET);
        }

        state.expectNewline();
    }

    private static void parseStructuredShape(State state, String shapeType, AbstractShapeBuilder builder) {
        builder.source(currentSourceLocation(state));
        ShapeId id = parseShapeName(state);
        state.visitor.onShape(builder.id(id));
        parseStructuredBody(shapeType, state, id);
    }

    private static void parseStructuredBody(String shapeType, State state, ShapeId parent) {
        parseStructuredContents(shapeType, state, parent, SetUtils.of());
        state.expectNewline();
    }

    private static void parseStructuredContents(
            String shapeType,
            State state,
            ShapeId parent,
            Collection<String> requiredMembers
    ) {
        state.expect(LBRACE);
        List<Pair<String, Node>> memberTraits = new ArrayList<>();
        Set<String> remainingMembers = requiredMembers.isEmpty() ? SetUtils.of() : new HashSet<>(requiredMembers);

        Token token = state.expect(ANNOTATION, QUOTED, UNQUOTED, RBRACE, DOC);
        while (token.type != RBRACE) {
            if (token.type == ANNOTATION) {
                memberTraits.add(parseTraitValue(token, state, true));
                // Traits can't come before a closing brace, so continue
                // to make sure they come before another trait or a key.
            } else if (token.type == DOC) {
                parseDocComment(state, token, true);
            } else {
                String memberName = token.lexeme;
                if (!requiredMembers.isEmpty()) {
                    if (!requiredMembers.contains(memberName)) {
                        throw state.syntax(String.format(
                                "Invalid member `%s` found in %s shape `%s`. Expected one of the following "
                                + "members: [%s]",
                                memberName, shapeType, parent, ValidationUtils.tickedList(requiredMembers)));
                    }
                    remainingMembers.remove(memberName);
                }

                ShapeId memberId = parent.withMember(memberName);
                state.expect(COLON);
                parseMember(state, memberId);
                // Add the loaded traits on the member now that the ID is known.
                for (Pair<String, Node> pair : memberTraits) {
                    state.visitor.onTrait(memberId, state.namespace, pair.getLeft(), pair.getRight());
                }
                memberTraits.clear();
                collectPendingDocString(state, memberId);

                if (state.expect(COMMA, RBRACE).type == RBRACE) {
                    break;
                }
            }

            token = state.expect(ANNOTATION, QUOTED, UNQUOTED, RBRACE, DOC);
        }

        if (!remainingMembers.isEmpty()) {
            throw state.syntax(String.format(
                    "Missing required members of %s shape `%s`: [%s]",
                    shapeType, parent, ValidationUtils.tickedList(remainingMembers)));
        }
    }

    private static void parseMember(State state, ShapeId memberId) {
        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(currentSourceLocation(state));
        state.visitor.onShape(memberBuilder);
        String target = state.expect(UNQUOTED, QUOTED).lexeme;
        state.visitor.onShapeTarget(state.namespace, target, memberBuilder::target);
    }

    private static void parseCollection(State state, String shapeType, CollectionShape.Builder builder) {
        // list Foo { member: Bar }
        builder.source(currentSourceLocation(state));
        ShapeId id = parseShapeName(state);
        parseStructuredContents(shapeType, state, id, SetUtils.of("member"));
        state.visitor.onShape(builder.id(id));
        state.expectNewline();
    }

    private static void parseMap(State state) {
        // map Foo { key: Bar, value: Baz }
        SourceLocation sourceLocation = currentSourceLocation(state);
        ShapeId id = parseShapeName(state);
        MapShape.Builder builder = MapShape.builder()
                .id(id)
                .source(sourceLocation);

        parseStructuredContents("map", state, id, MAP_KEYS);
        state.visitor.onShape(builder);
        state.expectNewline();
    }

    private static void parseMetadata(State state) {
        state.definedMetadata = true;

        // metadata key = value\n
        String key = state.expect(QUOTED, UNQUOTED).lexeme;
        state.expect(EQUAL);
        state.visitor.onMetadata(key, parseNode(state));
        state.expectNewline();
    }

    private static void parseApply(State state) {
        requireNamespaceOrThrow(state);
        // apply <ShapeName> @<trait>\n
        String name = state.expect(UNQUOTED).lexeme;
        ShapeId id = ShapeId.fromOptionalNamespace(state.namespace, name);
        Token token = state.expect(ANNOTATION);
        Pair<String, Node> trait = parseTraitValue(token, state, false);
        state.visitor.onTrait(id, state.namespace, trait.getLeft(), trait.getRight());
        state.expectNewline();
    }

    private static void parseTraitDefinition(State state) {
        requireNamespaceOrThrow(state);

        String docs = state.pendingDocComment != null ? state.pendingDocComment.content : null;
        state.pendingDocComment = null;

        String name = state.expect(UNQUOTED).lexeme;
        state.expect(LBRACE);
        ObjectNode node = parseObjectNode(state, sourceFromToken(state, state.current()), RBRACE);
        LoaderUtils.loadTraitDefinition(state.namespace, name, node, state.visitor, docs);
        state.expectNewline();
    }

    private static SourceLocation currentSourceLocation(State state) {
        return sourceFromToken(state, state.current());
    }

    private static SourceLocation sourceFromToken(State state, Token token) {
        return new SourceLocation(state.filename, token.line, token.column);
    }

    private static Node parseNode(State state) {
        return parseNodeValue(state, state.expect(LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER));
    }

    private static Node parseNodeValue(State state, Token token) {
        switch (token.type) {
            case LBRACE: return parseObjectNode(state, sourceFromToken(state, token), RBRACE);
            case LBRACKET: return parseArrayNode(state, sourceFromToken(state, token));
            case QUOTED: return new StringNode(token.lexeme, sourceFromToken(state, token));
            case NUMBER: return parseNumber(state, token);
            case UNQUOTED: return parseUnquotedNode(state, token);
            default: throw new IllegalStateException("Parse node value not expected to be called with invalid token");
        }
    }

    private static Node parseUnquotedNode(State state, Token token) {
        switch (token.lexeme) {
            case "true": return new BooleanNode(true, sourceFromToken(state, token));
            case "false": return new BooleanNode(false, sourceFromToken(state, token));
            case "null": return new NullNode(sourceFromToken(state, token));
            default: return new StringNode(token.lexeme, sourceFromToken(state, token));
        }
    }

    private static NumberNode parseNumber(State state, Token token) {
        if (token.lexeme.contains("e") || token.lexeme.contains(".")) {
            return new NumberNode(Double.valueOf(token.lexeme), sourceFromToken(state, token));
        } else {
            return new NumberNode(Long.parseLong(token.lexeme), sourceFromToken(state, token));
        }
    }

    private static ObjectNode parseObjectNode(State state, SourceLocation location, TokenType closing) {
        return parseObjectNodeWithKey(state, location, closing, state.expect(QUOTED, UNQUOTED, closing));
    }

    private static ObjectNode parseObjectNodeWithKey(State state, SourceLocation sloc, TokenType closing, Token key) {
        Map<StringNode, Node> entries = new LinkedHashMap<>();
        // Prevents entering the loop if immediately closed, and this checks for trailing commas.
        while (key.type != closing) {
            state.expect(COLON);
            Node value = parseNode(state);
            entries.put(new StringNode(key.lexeme, sourceFromToken(state, key)), value);
            if (state.expect(closing, COMMA).type == closing) {
                break;
            }
            key = state.expect(closing, QUOTED, UNQUOTED);
        }

        return new ObjectNode(entries, sloc);
    }

    private static ArrayNode parseArrayNode(State state, SourceLocation location) {
        List<Node> values = new ArrayList<>();
        Token next = state.expect(LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER, RBRACKET);
        // Checks initially and does a double-check for trailing commas.
        while (next.type != RBRACKET) {
            values.add(parseNodeValue(state, next));
            next = state.expect(RBRACKET, COMMA);
            if (next.type == RBRACKET) {
                break;
            }
            next = state.expect(RBRACKET, LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER);
        }

        return new ArrayNode(values, location);
    }
}
