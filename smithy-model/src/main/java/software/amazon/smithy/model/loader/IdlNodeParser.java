/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.function.Consumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;

/**
 * Parses Node values from a {@link IdlInternalTokenizer}.
 */
final class IdlNodeParser {

    private static final String SYNTACTIC_SHAPE_ID_TARGET = "SyntacticShapeIdTarget";

    private IdlNodeParser() {}

    /**
     * Expects that the current token is a valid Node, and parses it into a {@link Node} value.
     *
     * <p>The tokenizer is advanced to the next token after parsing the Node value.</p>
     *
     * @param loader IDL parser.
     * @return Returns the parsed node value.
     * @throws ModelSyntaxException if the Node is not well-formed.
     */
    static Node expectAndSkipNode(IdlModelLoader loader) {
        return expectAndSkipNode(loader, loader.getTokenizer().getCurrentTokenLocation());
    }

    /**
     * Expects that the current token is a valid Node, parses it into a {@link Node} value, and assigns it a custom
     * {@link SourceLocation}.
     *
     * <p>The tokenizer is advanced to the next token after parsing the Node value.</p>
     *
     * @param loader IDL loader.
     * @param location Source location to assign to the node.
     * @return Returns the parsed node value.
     * @throws ModelSyntaxException if the Node is not well-formed.
     */
    static Node expectAndSkipNode(IdlModelLoader loader, SourceLocation location) {
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        IdlToken token = tokenizer.expect(IdlToken.STRING,
                IdlToken.TEXT_BLOCK,
                IdlToken.NUMBER,
                IdlToken.IDENTIFIER,
                IdlToken.LBRACE,
                IdlToken.LBRACKET);

        switch (token) {
            case STRING:
            case TEXT_BLOCK:
                Node result = new StringNode(tokenizer.getCurrentTokenStringSlice().toString(), location);
                tokenizer.next();
                return result;
            case IDENTIFIER:
                String shapeId = loader.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
                return createIdentifier(loader, shapeId, location);
            case NUMBER:
                Number number = tokenizer.getCurrentTokenNumberValue();
                tokenizer.next();
                return new NumberNode(number, location);
            case LBRACE:
                return parseObjectNode(loader, location);
            case LBRACKET:
            default:
                return parseArrayNode(loader, location);
        }
    }

    /**
     * Parse a Node identifier String, taking into account keywords and forward references.
     *
     * @param loader     IDL parser.
     * @param identifier Identifier to parse.
     * @param location   Source location to assign to the identifier.
     * @return Returns the parsed identifier.
     */
    static Node createIdentifier(IdlModelLoader loader, String identifier, SourceLocation location) {
        Keyword keyword = Keyword.from(identifier);
        return keyword == null
                ? createSyntacticShapeId(loader, identifier, location)
                : keyword.createNode(location);
    }

    private enum Keyword {
        TRUE {
            @Override
            protected Node createNode(SourceLocation location) {
                return new BooleanNode(true, location);
            }
        },
        FALSE {
            @Override
            protected Node createNode(SourceLocation location) {
                return new BooleanNode(false, location);
            }
        },
        NULL {
            @Override
            protected Node createNode(SourceLocation location) {
                return new NullNode(location);
            }
        };

        protected abstract Node createNode(SourceLocation location);

        static Keyword from(String keyword) {
            switch (keyword) {
                case "true":
                    return Keyword.TRUE;
                case "false":
                    return Keyword.FALSE;
                case "null":
                    return Keyword.NULL;
                default:
                    return null;
            }
        }
    }

    private static Node createSyntacticShapeId(
            IdlModelLoader loader,
            String identifier,
            SourceLocation location
    ) {
        // Unquoted node values syntactically are assumed to be references to shapes. A lazy string node is
        // used because the shape ID may not be able to be resolved until after the entire model is loaded.
        Pair<StringNode, Consumer<String>> pair = StringNode.createLazyString(identifier, location);
        Consumer<String> consumer = pair.right;
        loader.addForwardReference(identifier, (id, type) -> {
            consumer.accept(id.toString());
            if (type != null) {
                return null;
            } else {
                return ValidationEvent.builder()
                        .id(SYNTACTIC_SHAPE_ID_TARGET)
                        .severity(Severity.DANGER)
                        .message(String.format("Syntactic shape ID `%s` does not resolve to a valid shape ID: "
                                + "`%s`. Did you mean to quote this string? Are you missing a "
                                + "model file?", identifier, id))
                        .sourceLocation(location)
                        .build();
            }
        });
        return pair.left;
    }

    private static ArrayNode parseArrayNode(IdlModelLoader loader, SourceLocation location) {
        loader.increaseNestingLevel();
        ArrayNode.Builder builder = ArrayNode.builder().sourceLocation(location);

        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        tokenizer.expect(IdlToken.LBRACKET);
        tokenizer.next();
        tokenizer.skipWsAndDocs();

        do {
            if (tokenizer.getCurrentToken() == IdlToken.RBRACKET) {
                break;
            } else {
                builder.withValue(expectAndSkipNode(loader));
                tokenizer.skipWsAndDocs();
            }
        } while (true);

        tokenizer.expect(IdlToken.RBRACKET);
        tokenizer.next();
        loader.decreaseNestingLevel();
        return builder.build();
    }

    private static ObjectNode parseObjectNode(IdlModelLoader loader, SourceLocation location) {
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWsAndDocs();
        loader.increaseNestingLevel();
        ObjectNode.Builder builder = ObjectNode.builder().sourceLocation(location);

        while (tokenizer.hasNext()) {
            if (tokenizer.expect(IdlToken.RBRACE, IdlToken.STRING, IdlToken.IDENTIFIER) == IdlToken.RBRACE) {
                break;
            }

            String key = loader.internString(tokenizer.getCurrentTokenStringSlice());
            SourceLocation keyLocation = tokenizer.getCurrentTokenLocation();
            tokenizer.next();
            tokenizer.skipWsAndDocs();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipWsAndDocs();

            Node value = expectAndSkipNode(loader);
            if (builder.hasMember(key)) {
                throw new ModelSyntaxException("Duplicate member: '" + key + '\'', keyLocation);
            }
            builder.withMember(key, value);
            tokenizer.skipWsAndDocs();
        }

        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
        loader.decreaseNestingLevel();
        return builder.build();
    }
}
