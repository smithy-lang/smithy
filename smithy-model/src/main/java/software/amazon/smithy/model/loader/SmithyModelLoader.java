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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.smithy.model.FromSourceLocation;
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
        STATEMENTS.put("use", SmithyModelLoader::parseUseStatement);
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

        visitor.onOpenFile(path);
        SmithyModelLexer lexer = new SmithyModelLexer(path, contentSupplier.get());
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
        Token current;
        DocComment pendingDocComment;
        boolean definedMetadata;
        boolean definedShapesOrTraits;

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
         * @return Get the current source location.
         */
        SourceLocation currentLocation() {
            return current.getSourceLocation();
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
            Token token = state.expect(UNQUOTED, ANNOTATION, CONTROL, DOC);
            if (token.type == UNQUOTED) {
                parseStatement(token, state);
            } else if (token.type == ANNOTATION) {
                state.pendingTraits.add(parseTraitValue(token, state, false));
            } else if (token.type == CONTROL) {
                parseControlStatement(state, token);
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

    private static void parseControlStatement(State state, Token token) {
        if (state.visitor.getNamespace() != null || state.definedMetadata) {
            throw state.syntax("A control statement must come before any namespace, metadata, or shape");
        }

        // Remove the starting "$" and ending ":".
        String key = token.lexeme.substring(1, token.lexeme.length() - 1);
        Node value = parseNodeValue(state, state.next());

        if (key.equals("version")) {
            if (!value.isStringNode()) {
                value.expectStringNode("The $version control statement must have a string value, but found "
                                       + Node.printJson(value));
            }
            state.visitor.onVersion(state.currentLocation(), value.expectStringNode().getValue());
        } else {
            state.visitor.onError(ValidationEvent.builder()
                    .eventId(Validator.MODEL_ERROR)
                    .sourceLocation(value)
                    .severity(Severity.WARNING)
                    .message(String.format(
                            "Unknown control statement `%s` with value `%s", key, Node.printJson(value)))
                    .build());
        }
    }

    private static void parseUseStatement(State state) {
        if (state.definedShapesOrTraits) {
            throw state.syntax("A use statement must come before any shape or trait definition");
        }

        switch (state.expect(UNQUOTED).lexeme) {
            case "shape":
                parseUse(state, state.visitor::onUseShape);
                break;
            case "trait":
                parseUse(state, state.visitor::onUseTrait);
                break;
            default:
                throw state.syntax("Invalid use statement");
        }
    }

    private static void parseUse(State state, BiConsumer<ShapeId, FromSourceLocation> consumer) {
        Token namespaceToken = state.expect(UNQUOTED);
        String namespace = namespaceToken.lexeme;

        int delimiter = namespace.indexOf('#');
        if (delimiter <= 0) {
            throw new UseException("Use statement is missing a namespace: " + namespace, namespaceToken);
        } else if (!ShapeId.isValidNamespace(namespace.substring(0, delimiter))) {
            throw new UseException("Invalid use statement namespace: " + namespace, namespaceToken);
        }

        if (namespace.length() > delimiter + 1) {
            consumer.accept(parseUseShapeId(state, namespace), namespaceToken);
            state.expectNewline();
            return;
        }

        state.expect(LBRACKET);
        Token next = state.expect(UNQUOTED);

        do {
            if (next.lexeme.contains("#")) {
                throw new UseException("Multi-use statement values must be relative. Found: " + next.lexeme, next);
            }
            consumer.accept(parseUseShapeId(state, namespace + next.lexeme), next);
            // Account for trailing commas.
            if (state.expect(RBRACKET, COMMA).type == RBRACKET) {
                break;
            }
            next = state.expect(RBRACKET, UNQUOTED);
        } while (next.type != RBRACKET);

        state.expectNewline();
    }

    private static ShapeId parseUseShapeId(State state, String token) {
        try {
            return ShapeId.from(token);
        } catch (ShapeIdSyntaxException e) {
            throw state.syntax("Invalid use ID syntax (" + e.getMessage() + ")");
        }
    }

    private static void parseDocComment(State state, Token token, boolean memberScope) {
        StringBuilder builder = new StringBuilder(token.getDocContents());
        while (state.test(DOC)) {
            builder.append('\n').append(state.next().getDocContents());
        }

        state.pendingDocComment = new DocComment(builder.toString(), token.getSourceLocation());

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
        state.visitor.onNamespace(state.expect(UNQUOTED).lexeme, state.current);
    }

    private static void requireNamespaceOrThrow(State state) {
        if (state.visitor.getNamespace() == null) {
            throw state.syntax("A namespace must be set before shapes or traits can be defined");
        }
    }

    private static Pair<String, Node> parseTraitValue(Token token, State state, boolean memberScope) {
        try {
            requireNamespaceOrThrow(state);

            // Resolve the trait name and ensure that the trait forms a syntactically valid value.
            ShapeId.fromOptionalNamespace(state.visitor.getNamespace(), token.lexeme);
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
            return new NullNode(state.currentLocation());
        }

        state.expect(LPAREN);
        Token next = state.expect(RPAREN, QUOTED, UNQUOTED, LBRACKET, NUMBER);

        if (next.type == RPAREN) {
            // An open and closed "()" signals an empty object.
            return new ObjectNode(MapUtils.of(), next.getSourceLocation());
        }

        // Test to see if this is just a string or if it's an object.
        if (state.test(COLON)) {
            if (next.type == QUOTED || next.type == UNQUOTED) {
                // Parse the object using the already parsed key.
                return parseObjectNodeWithKey(state, state.currentLocation(), RPAREN, next);
            }
            throw state.syntax("Expected a string to start a trait value object");
        }

        Node result;
        if (next.type == LBRACKET) {
            result = parseArrayNode(state, next.getSourceLocation());
        } else if (next.type == NUMBER) {
            result = parseNumber(next);
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
        state.definedShapesOrTraits = true;
        Token nameToken = state.expect(UNQUOTED);
        String name = nameToken.lexeme;
        ShapeId id = state.visitor.onShapeDefName(name, nameToken);
        state.pendingTraits.forEach(pair -> state.visitor.onTrait(id, pair.getLeft(), pair.getRight()));
        state.pendingTraits.clear();
        collectPendingDocString(state, id);
        return id;
    }

    private static void collectPendingDocString(State state, ShapeId id) {
        if (state.pendingDocComment != null) {
            Node value = new StringNode(state.pendingDocComment.content, state.pendingDocComment.sourceLocation);
            state.pendingDocComment = null;
            state.visitor.onTrait(id, DocumentationTrait.NAME, value);
        }
    }

    private static void parseSimpleShape(State state, AbstractShapeBuilder builder) {
        builder.source(state.currentLocation());
        builder.id(parseShapeName(state));
        state.visitor.onShape(builder);
        state.expectNewline();
    }

    private static void parseService(State state) {
        SourceLocation sourceLocation = state.currentLocation();
        ShapeId shapeId = parseShapeName(state);
        ServiceShape.Builder builder = new ServiceShape.Builder()
                .id(shapeId)
                .source(sourceLocation);

        ObjectNode shapeNode = parseObjectNode(state, state.expect(LBRACE).getSourceLocation(), RBRACE);
        shapeNode.warnIfAdditionalProperties(LoaderUtils.SERVICE_PROPERTY_NAMES);
        LoaderUtils.loadServiceObject(builder, shapeId, shapeNode);
        state.visitor.onShape(builder);
        state.expectNewline();
    }

    private static void parseResource(State state) {
        SourceLocation sourceLocation = state.currentLocation();
        ShapeId shapeId = parseShapeName(state);
        ResourceShape.Builder builder = ResourceShape.builder().id(shapeId).source(sourceLocation);
        state.visitor.onShape(builder);
        ObjectNode shapeNode = parseObjectNode(state, state.expect(LBRACE).getSourceLocation(), RBRACE);
        shapeNode.warnIfAdditionalProperties(LoaderUtils.RESOURCE_PROPERTY_NAMES);
        LoaderUtils.loadResourceObject(builder, shapeId, shapeNode, state.visitor);
        state.expectNewline();
    }

    private static void parseOperation(State state) {
        SourceLocation sourceLocation = state.currentLocation();
        ShapeId id = parseShapeName(state);
        OperationShape.Builder builder = OperationShape.builder().id(id).source(sourceLocation);
        state.visitor.onShape(builder);

        // Parse the optionally present input target.
        state.expect(LPAREN);
        Token next = state.expect(RPAREN, UNQUOTED);
        if (next.type == UNQUOTED) {
            state.visitor.onShapeTarget(next.lexeme, next, builder::input);
            state.expect(RPAREN);
        }

        // Parse the optionally present return value.
        if (state.test(RETURN)) {
            state.expect(RETURN);
            Token returnToken = state.expect(UNQUOTED);
            state.visitor.onShapeTarget(returnToken.lexeme, returnToken, builder::output);
        }

        // Parse the optionally present errors list.
        if (state.peek().filter(token -> token.lexeme.equals("errors")).isPresent()) {
            state.next(); // Skip the error.
            state.expect(LBRACKET);
            if (!state.test(RBRACKET)) {
                while (true) {
                    Token errorToken = state.expect(UNQUOTED);
                    state.visitor.onShapeTarget(errorToken.lexeme, errorToken, builder::addError);
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
        builder.source(state.currentLocation());
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
                    state.visitor.onTrait(memberId, pair.getLeft(), pair.getRight());
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
        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(state.currentLocation());
        state.visitor.onShape(memberBuilder);
        Token targetToken = state.expect(UNQUOTED, QUOTED);
        state.visitor.onShapeTarget(targetToken.lexeme, targetToken, memberBuilder::target);
    }

    private static void parseCollection(State state, String shapeType, CollectionShape.Builder builder) {
        // list Foo { member: Bar }
        builder.source(state.currentLocation());
        ShapeId id = parseShapeName(state);
        parseStructuredContents(shapeType, state, id, SetUtils.of("member"));
        state.visitor.onShape(builder.id(id));
        state.expectNewline();
    }

    private static void parseMap(State state) {
        // map Foo { key: Bar, value: Baz }
        SourceLocation sourceLocation = state.currentLocation();
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
        ShapeId id = ShapeId.fromOptionalNamespace(state.visitor.getNamespace(), name);
        Token token = state.expect(ANNOTATION);
        Pair<String, Node> trait = parseTraitValue(token, state, false);
        state.visitor.onTrait(id, trait.getLeft(), trait.getRight());
        state.expectNewline();
    }

    private static void parseTraitDefinition(State state) {
        state.definedShapesOrTraits = true;
        Token nameToken = state.expect(UNQUOTED);
        ShapeId resolved = state.visitor.onTraitDefName(nameToken.lexeme, nameToken);
        // Grab any pending documentation comment.
        String docs = state.pendingDocComment != null ? state.pendingDocComment.content : null;
        state.pendingDocComment = null;
        // Parse the opening of the trait, and the properties.
        state.expect(LBRACE);
        ObjectNode node = parseObjectNode(state, state.currentLocation(), RBRACE);
        LoaderUtils.loadTraitDefinition(resolved.getNamespace(), resolved.getName(), node, state.visitor, docs);
        state.expectNewline();
    }

    private static Node parseNode(State state) {
        return parseNodeValue(state, state.expect(LBRACE, LBRACKET, QUOTED, UNQUOTED, NUMBER));
    }

    private static Node parseNodeValue(State state, Token token) {
        switch (token.type) {
            case LBRACE: return parseObjectNode(state, token.getSourceLocation(), RBRACE);
            case LBRACKET: return parseArrayNode(state, token.getSourceLocation());
            case QUOTED: return new StringNode(token.lexeme, token.getSourceLocation());
            case NUMBER: return parseNumber(token);
            case UNQUOTED: return parseUnquotedNode(state, token);
            default: throw new IllegalStateException("Parse node value not expected to be called with invalid token");
        }
    }

    private static Node parseUnquotedNode(State state, Token token) {
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
                state.visitor.onShapeTarget(token.lexeme, token, id -> consumer.accept(id.toString()));
                return pair.left;
        }
    }

    private static NumberNode parseNumber(Token token) {
        if (token.lexeme.contains("e") || token.lexeme.contains(".")) {
            return new NumberNode(Double.valueOf(token.lexeme), token.getSourceLocation());
        } else {
            return new NumberNode(Long.parseLong(token.lexeme), token.getSourceLocation());
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
            entries.put(new StringNode(key.lexeme, key.getSourceLocation()), value);
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
