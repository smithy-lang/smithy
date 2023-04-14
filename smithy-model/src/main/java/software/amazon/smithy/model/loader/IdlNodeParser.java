/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Parses Node values from a {@link IdlTokenizer}.
 */
final class IdlNodeParser {

    private static final String SYNTACTIC_SHAPE_ID_TARGET = "SyntacticShapeIdTarget";

    private IdlNodeParser() { }

    /**
     * Expects that the current token is a valid Node, and parses it into a {@link Node} value.
     *
     * <p>The tokenizer is advanced to the next token after parsing the Node value.</p>
     *
     * @param tokenizer Tokenizer to consume and advance.
     * @param resolver  Forward reference resolver.
     * @return Returns the parsed node value.
     * @throws ModelSyntaxException if the Node is not well-formed.
     */
    static Node expectAndSkipNode(IdlTokenizer tokenizer, IdlReferenceResolver resolver) {
        return expectAndSkipNode(tokenizer, resolver, tokenizer.getCurrentTokenLocation());
    }

    /**
     * Expects that the current token is a valid Node, parses it into a {@link Node} value, and assigns it a custom
     * {@link SourceLocation}.
     *
     * <p>The tokenizer is advanced to the next token after parsing the Node value.</p>
     *
     * @param tokenizer Tokenizer to consume and advance.
     * @param resolver  Forward reference resolver.
     * @param location Source location to assign to the node.
     * @return Returns the parsed node value.
     * @throws ModelSyntaxException if the Node is not well-formed.
     */
    static Node expectAndSkipNode(IdlTokenizer tokenizer, IdlReferenceResolver resolver, SourceLocation location) {
        IdlToken token = tokenizer.expect(IdlToken.STRING, IdlToken.TEXT_BLOCK, IdlToken.NUMBER, IdlToken.IDENTIFIER,
                                          IdlToken.LBRACE, IdlToken.LBRACKET);

        switch (token) {
            case STRING:
            case TEXT_BLOCK:
                Node result = new StringNode(tokenizer.getCurrentTokenStringSlice().toString(), location);
                tokenizer.next();
                return result;
            case IDENTIFIER:
                String shapeId = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
                return parseIdentifier(resolver, shapeId, location);
            case NUMBER:
                Number number = tokenizer.getCurrentTokenNumberValue();
                tokenizer.next();
                return new NumberNode(number, location);
            case LBRACE:
                return parseObjectNode(tokenizer, resolver, location);
            case LBRACKET:
            default:
                return parseArrayNode(tokenizer, resolver, location);
        }
    }

    /**
     * Parse a Node identifier String, taking into account keywords and forward references.
     *
     * @param resolver   Forward reference resolver.
     * @param identifier Identifier to parse.
     * @param location   Source location to assign to the identifier.
     * @return Returns the parsed identifier.
     */
    static Node parseIdentifier(
            IdlReferenceResolver resolver,
            String identifier,
            SourceLocation location
    ) {
        Keyword keyword = Keyword.from(identifier);
        return keyword == null
               ? parseSyntacticShapeId(resolver, identifier, location)
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

    private static Node parseSyntacticShapeId(
            IdlReferenceResolver resolver,
            String identifier,
            SourceLocation location
    ) {
        // Unquoted node values syntactically are assumed to be references to shapes. A lazy string node is
        // used because the shape ID may not be able to be resolved until after the entire model is loaded.
        Pair<StringNode, Consumer<String>> pair = StringNode.createLazyString(identifier, location);
        Consumer<String> consumer = pair.right;
        resolver.resolve(identifier, (id, type) -> {
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

    private static ArrayNode parseArrayNode(
            IdlTokenizer tokenizer,
            IdlReferenceResolver resolver,
            SourceLocation location
    ) {
        tokenizer.increaseNestingLevel();
        ArrayNode.Builder builder = ArrayNode.builder().sourceLocation(location);

        tokenizer.expect(IdlToken.LBRACKET);
        tokenizer.next();
        tokenizer.skipWsAndDocs();

        do {
            if (tokenizer.getCurrentToken() == IdlToken.RBRACKET) {
                break;
            } else {
                builder.withValue(expectAndSkipNode(tokenizer, resolver));
                tokenizer.skipWsAndDocs();
            }
        } while (true);

        tokenizer.expect(IdlToken.RBRACKET);
        tokenizer.next();
        tokenizer.decreaseNestingLevel();
        return builder.build();
    }

    private static ObjectNode parseObjectNode(
            IdlTokenizer tokenizer,
            IdlReferenceResolver resolver,
            SourceLocation location
    ) {
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWsAndDocs();
        tokenizer.increaseNestingLevel();
        ObjectNode.Builder builder = ObjectNode.builder().sourceLocation(location);

        while (tokenizer.hasNext()) {
            if (tokenizer.expect(IdlToken.RBRACE, IdlToken.STRING, IdlToken.IDENTIFIER) == IdlToken.RBRACE) {
                break;
            }

            String key = tokenizer.internString(tokenizer.getCurrentTokenStringSlice());
            SourceLocation keyLocation = tokenizer.getCurrentTokenLocation();
            tokenizer.next();
            tokenizer.skipWsAndDocs();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipWsAndDocs();

            Node value = expectAndSkipNode(tokenizer, resolver);
            if (builder.hasMember(key)) {
                throw new ModelSyntaxException("Duplicate member: '" + key + '\'', keyLocation);
            }
            builder.withMember(key, value);
            tokenizer.skipWsAndDocs();
        }

        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
        tokenizer.decreaseNestingLevel();
        return builder.build();
    }
}
