/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Defines the tree type.
 *
 * <p>These types typically map 1:1 to a production in the Smithy IDL grammar, except that tree types bottom-out at
 * {@link IdlToken}.
 *
 * <p>For example:
 *
 * <ul>
 *     <li>The {@code Identifier} production is combined into a single {@link #IDENTIFIER} node.
 *     {@code IdentifierStart} and {@code IdentifierChars} are not exposed in the token tree.</li>
 *     <li>The {@code Number} production is combined into a single {@link #NUMBER} node. Productions like
 *     {@code DecimalPoint}, {@code Exp}, etc are not exposed in the token tree.</li>
 *     <li>The {@code QuotedText} production is combined into a single {@link #QUOTED_TEXT} node.
 *     <li>The {@code TextBlock} production is combined into a single {@link #TEXT_BLOCK} node.
 * </ul>
 */
public enum TreeType {
    IDL {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            optionalWs(tokenizer);
            CONTROL_SECTION.parse(tokenizer);
            METADATA_SECTION.parse(tokenizer);
            SHAPE_SECTION.parse(tokenizer);
            tokenizer.expect(IdlToken.EOF);
        }
    },

    CONTROL_SECTION {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                while (tokenizer.getCurrentToken() == IdlToken.DOLLAR) {
                    CONTROL_STATEMENT.parse(tokenizer);
                }
            });
        }
    },

    CONTROL_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.DOLLAR);
                tokenizer.next(); // $
                NODE_OBJECT_KEY.parse(tokenizer);
                optionalSpaces(tokenizer);
                tokenizer.expect(IdlToken.COLON);
                tokenizer.next();
                optionalSpaces(tokenizer);
                NODE_VALUE.parse(tokenizer);
                BR.parse(tokenizer);
            });
        }
    },

    METADATA_SECTION {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                while (tokenizer.isCurrentLexeme("metadata")) {
                    METADATA_STATEMENT.parse(tokenizer);
                }
            });
        }
    },

    METADATA_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.next(); // append metadata
                optionalSpaces(tokenizer);
                NODE_OBJECT_KEY.parse(tokenizer);
                optionalSpaces(tokenizer);
                tokenizer.expect(IdlToken.EQUAL);
                tokenizer.next();
                optionalSpaces(tokenizer);
                NODE_VALUE.parse(tokenizer);
                BR.parse(tokenizer);
            });
        }
    },

    SHAPE_SECTION {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                NAMESPACE_STATEMENT.parse(tokenizer);
                USE_SECTION.parse(tokenizer);
                SHAPE_STATEMENTS.parse(tokenizer);
            });
        }
    },

    NAMESPACE_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            if (tokenizer.isCurrentLexeme("namespace")) {
                tokenizer.withState(this, () -> {
                    tokenizer.next(); // skip "namespace"
                    SP.parse(tokenizer);
                    NAMESPACE.parse(tokenizer);
                    BR.parse(tokenizer);
                });
            } else if (tokenizer.hasNext()) {
                tokenizer.withState(this, () -> {
                    throw new ModelSyntaxException(
                            "Expected a namespace definition but found "
                                    + tokenizer.getCurrentToken().getDebug(tokenizer.getCurrentTokenLexeme()),
                            tokenizer.getCurrentTokenLocation());
                });
            }
        }
    },

    USE_SECTION {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                while (tokenizer.getCurrentToken() == IdlToken.IDENTIFIER) {
                    // Don't over-parse here for unions.
                    String keyword = tokenizer.internString(tokenizer.getCurrentTokenLexeme());
                    if (!keyword.equals("use")) {
                        break;
                    }
                    USE_STATEMENT.parse(tokenizer);
                }
            });
        }
    },

    USE_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.next(); // Skip "use"
                SP.parse(tokenizer);
                ABSOLUTE_ROOT_SHAPE_ID.parse(tokenizer);
                BR.parse(tokenizer);
            });
        }
    },

    SHAPE_STATEMENTS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                while (tokenizer.hasNext()) {
                    SHAPE_OR_APPLY_STATEMENT.parse(tokenizer);
                    BR.parse(tokenizer);
                }
            });
        }
    },

    SHAPE_OR_APPLY_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                if (tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.AT) == IdlToken.AT) {
                    SHAPE_STATEMENT.parse(tokenizer);
                } else if (tokenizer.isCurrentLexeme("apply")) {
                    APPLY_STATEMENT.parse(tokenizer);
                } else {
                    SHAPE_STATEMENT.parse(tokenizer);
                }
            });
        }
    },

    SHAPE_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                TRAIT_STATEMENTS.parse(tokenizer);
                SHAPE.parse(tokenizer);
            });
        }
    },

    SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                ShapeType type = ShapeType.fromString(tokenizer.internString(tokenizer.getCurrentTokenLexeme()))
                        .orElseThrow(() -> new ModelSyntaxException("Expected a valid shape type",
                                tokenizer.getCurrentTokenLocation()));
                switch (type) {
                    case ENUM:
                    case INT_ENUM:
                        ENUM_SHAPE.parse(tokenizer);
                        break;
                    case SERVICE:
                    case RESOURCE:
                        ENTITY_SHAPE.parse(tokenizer);
                        break;
                    case OPERATION:
                        OPERATION_SHAPE.parse(tokenizer);
                        break;
                    default:
                        switch (type.getCategory()) {
                            case SIMPLE:
                                SIMPLE_SHAPE.parse(tokenizer);
                                break;
                            case AGGREGATE:
                                AGGREGATE_SHAPE.parse(tokenizer);
                                break;
                            default:
                                throw new UnsupportedOperationException("Unexpected type: " + type);
                        }
                }
            });
        }
    },

    SIMPLE_SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                parseShapeTypeAndName(tokenizer, SIMPLE_TYPE_NAME);
                parseOptionalMixins(tokenizer);
            });
        }
    },

    SIMPLE_TYPE_NAME {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            // Assumes that the current token is a valid simple type name validated by SHAPE.
            tokenizer.withState(this, tokenizer::next);
        }
    },

    ENUM_SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                parseShapeTypeAndName(tokenizer, ENUM_TYPE_NAME);
                parseOptionalMixins(tokenizer);

                optionalWs(tokenizer);
                ENUM_SHAPE_MEMBERS.parse(tokenizer);
            });
        }
    },

    ENUM_TYPE_NAME {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            // Assumes that the current token is a valid enum type name validated by SHAPE.
            tokenizer.withState(this, tokenizer::next);
        }
    },

    ENUM_SHAPE_MEMBERS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.LBRACE);
                tokenizer.next();
                optionalWs(tokenizer);

                while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                    ENUM_SHAPE_MEMBER.parse(tokenizer);
                    optionalWs(tokenizer);
                }

                tokenizer.expect(IdlToken.RBRACE);
                tokenizer.next();
            });
        }
    },

    ENUM_SHAPE_MEMBER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                TRAIT_STATEMENTS.parse(tokenizer);
                IDENTIFIER.parse(tokenizer);
                parseOptionalValueAssignment(tokenizer);
            });
        }
    },

    AGGREGATE_SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                parseShapeTypeAndName(tokenizer, AGGREGATE_TYPE_NAME);
                parseSharedStructureBodyWithinInline(tokenizer);
            });
        }
    },

    AGGREGATE_TYPE_NAME {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            // Assumes that the current token is a valid simple type name validated by SHAPE.
            tokenizer.withState(this, tokenizer::next);
        }
    },

    // Don't use this directly. Instead, use parseOptionalForResource
    FOR_RESOURCE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.next(); // Skip "for"
                SP.parse(tokenizer);
                SHAPE_ID.parse(tokenizer);
            });
        }
    },

    SHAPE_MEMBERS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.LBRACE);
                tokenizer.next();
                optionalWs(tokenizer);

                while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                    SHAPE_MEMBER.parse(tokenizer);
                    optionalWs(tokenizer);
                }

                tokenizer.expect(IdlToken.RBRACE);
                tokenizer.next();
            });
        }
    },

    SHAPE_MEMBER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                TRAIT_STATEMENTS.parse(tokenizer);
                if (tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.DOLLAR) == IdlToken.DOLLAR) {
                    ELIDED_SHAPE_MEMBER.parse(tokenizer);
                } else {
                    EXPLICIT_SHAPE_MEMBER.parse(tokenizer);
                }
                parseOptionalValueAssignment(tokenizer);
            });
        }
    },

    EXPLICIT_SHAPE_MEMBER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                IDENTIFIER.parse(tokenizer);
                optionalSpaces(tokenizer);
                tokenizer.expect(IdlToken.COLON);
                tokenizer.next();
                optionalSpaces(tokenizer);
                SHAPE_ID.parse(tokenizer);
            });
        }
    },

    ELIDED_SHAPE_MEMBER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.DOLLAR);
                tokenizer.next();
                IDENTIFIER.parse(tokenizer);
            });
        }
    },

    ENTITY_SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            // Assumes that the shape type is a valid "service" or "resource".
            tokenizer.withState(this, () -> {
                parseShapeTypeAndName(tokenizer, ENTITY_TYPE_NAME);

                parseOptionalMixins(tokenizer);

                optionalWs(tokenizer);
                tokenizer.expect(IdlToken.LBRACE);
                NODE_OBJECT.parse(tokenizer);
            });
        }
    },

    ENTITY_TYPE_NAME {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            // Assumes that the current token is a valid entity type name validated by SHAPE.
            tokenizer.withState(this, tokenizer::next);
        }
    },

    OPERATION_SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                parseShapeTypeAndName(tokenizer);

                parseOptionalMixins(tokenizer);

                optionalWs(tokenizer);
                OPERATION_BODY.parse(tokenizer);
            });
        }
    },

    OPERATION_BODY {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.LBRACE);
                tokenizer.next();
                optionalWs(tokenizer);
                while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                    OPERATION_PROPERTY.parse(tokenizer);
                    optionalWs(tokenizer);
                }
                tokenizer.expect(IdlToken.RBRACE);
                tokenizer.next();
            });
        }
    },

    OPERATION_PROPERTY {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.IDENTIFIER);
                if (tokenizer.isCurrentLexeme("input")) {
                    OPERATION_INPUT.parse(tokenizer);
                } else if (tokenizer.isCurrentLexeme("output")) {
                    OPERATION_OUTPUT.parse(tokenizer);
                } else if (tokenizer.isCurrentLexeme("errors")) {
                    OPERATION_ERRORS.parse(tokenizer);
                } else {
                    throw new ModelSyntaxException("Expected 'input', 'output', or 'errors'. Found '"
                            + tokenizer.getCurrentTokenLexeme() + "'",
                            tokenizer.getCurrentTokenLocation());
                }
            });
        }
    },

    OPERATION_INPUT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.IDENTIFIER);
                tokenizer.next(); // skip "input"
                optionalWs(tokenizer);
                operationInputOutputDefinition(tokenizer);
            });
        }
    },

    OPERATION_OUTPUT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.IDENTIFIER);
                tokenizer.next(); // skip "output"
                optionalWs(tokenizer);
                operationInputOutputDefinition(tokenizer);
            });
        }
    },

    INLINE_AGGREGATE_SHAPE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.WALRUS);
                tokenizer.next();
                optionalWs(tokenizer);
                TRAIT_STATEMENTS.parse(tokenizer);
                parseSharedStructureBodyWithinInline(tokenizer);
            });
        }
    },

    OPERATION_ERRORS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.IDENTIFIER);
                tokenizer.next(); // skip "errors"
                optionalWs(tokenizer);
                tokenizer.expect(IdlToken.COLON);
                tokenizer.next();
                optionalWs(tokenizer);
                tokenizer.expect(IdlToken.LBRACKET);
                tokenizer.next();
                optionalWs(tokenizer);
                while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACKET) {
                    SHAPE_ID.parse(tokenizer);
                    optionalWs(tokenizer);
                }
                tokenizer.expect(IdlToken.RBRACKET);
                tokenizer.next();
            });
        }
    },

    // Mixins =
    //     [SP] %s"with" [WS] "[" [WS] 1*(ShapeId [WS]) "]"
    // Don't use this directly. Instead, use parseOptionalMixins
    MIXINS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.next(); // Skip "with"
                optionalWs(tokenizer);

                tokenizer.expect(IdlToken.LBRACKET);
                tokenizer.next();
                optionalWs(tokenizer);

                do {
                    SHAPE_ID.parse(tokenizer);
                    optionalWs(tokenizer);
                } while (tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.RBRACKET) == IdlToken.IDENTIFIER);

                optionalWs(tokenizer);
                tokenizer.expect(IdlToken.RBRACKET);
                tokenizer.next();
            });
        }
    },

    // Don't use this directly. Instead, use parseOptionalValueAssignment
    VALUE_ASSIGNMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                optionalSpaces(tokenizer);
                tokenizer.expect(IdlToken.EQUAL);
                tokenizer.next();
                optionalSpaces(tokenizer);
                NODE_VALUE.parse(tokenizer);

                optionalSpaces(tokenizer);
                if (tokenizer.getCurrentToken() == IdlToken.COMMA) {
                    COMMA.parse(tokenizer);
                }

                BR.parse(tokenizer);
            });
        }
    },

    TRAIT_STATEMENTS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                while (tokenizer.getCurrentToken() == IdlToken.AT) {
                    TRAIT.parse(tokenizer);
                    optionalWs(tokenizer);
                }
            });
        }
    },

    TRAIT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.AT);
                tokenizer.next();
                SHAPE_ID.parse(tokenizer);
                if (tokenizer.getCurrentToken() == IdlToken.LPAREN) {
                    TRAIT_BODY.parse(tokenizer);
                }
            });
        }
    },

    TRAIT_BODY {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.LPAREN);
                tokenizer.next();
                optionalWs(tokenizer);

                if (tokenizer.getCurrentToken() != IdlToken.RPAREN) {
                    tokenizer.expect(IdlToken.LBRACE,
                            IdlToken.LBRACKET,
                            IdlToken.TEXT_BLOCK,
                            IdlToken.STRING,
                            IdlToken.NUMBER,
                            IdlToken.IDENTIFIER);
                    switch (tokenizer.getCurrentToken()) {
                        case LBRACE:
                        case LBRACKET:
                        case TEXT_BLOCK:
                        case NUMBER:
                            TRAIT_NODE.parse(tokenizer);
                            break;
                        case STRING:
                        case IDENTIFIER:
                        default:
                            CapturedToken nextPastWs = tokenizer.peekWhile(1,
                                    token -> token.isWhitespace() || token == IdlToken.DOC_COMMENT);
                            if (nextPastWs.getIdlToken() == IdlToken.COLON) {
                                TRAIT_STRUCTURE.parse(tokenizer);
                            } else {
                                TRAIT_NODE.parse(tokenizer);
                            }
                    }
                }

                tokenizer.expect(IdlToken.RPAREN); // Expect and skip ")"
                tokenizer.next();
            });
        }
    },

    TRAIT_STRUCTURE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                do {
                    NODE_OBJECT_KVP.parse(tokenizer);
                    optionalWs(tokenizer);
                } while (tokenizer.getCurrentToken() != IdlToken.RPAREN && tokenizer.hasNext());
            });
        }
    },

    TRAIT_NODE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                // parse these as NODE_VALUE.
                NODE_VALUE.parse(tokenizer);
                optionalWs(tokenizer);
            });
        }
    },

    APPLY_STATEMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                // Try to see if this is a singular or block apply statement.
                IdlToken peek = tokenizer
                        .peekWhile(1, t -> t != IdlToken.AT && t != IdlToken.LBRACE)
                        .getIdlToken();
                if (peek == IdlToken.LBRACE) {
                    APPLY_STATEMENT_BLOCK.parse(tokenizer);
                } else {
                    APPLY_STATEMENT_SINGULAR.parse(tokenizer);
                }
            });
        }
    },

    APPLY_STATEMENT_SINGULAR {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.next(); // Skip "apply"
                SP.parse(tokenizer);
                SHAPE_ID.parse(tokenizer);
                WS.parse(tokenizer);
                TRAIT.parse(tokenizer);
            });
        }
    },

    APPLY_STATEMENT_BLOCK {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.next(); // Skip "apply"
                SP.parse(tokenizer);
                SHAPE_ID.parse(tokenizer);
                WS.parse(tokenizer);
                tokenizer.expect(IdlToken.LBRACE);
                tokenizer.next();
                optionalWs(tokenizer);
                TRAIT_STATEMENTS.parse(tokenizer);
                optionalWs(tokenizer);
                tokenizer.expect(IdlToken.RBRACE);
                tokenizer.next();
            });
        }
    },

    NODE_VALUE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                IdlToken token = tokenizer.expect(IdlToken.STRING,
                        IdlToken.TEXT_BLOCK,
                        IdlToken.NUMBER,
                        IdlToken.IDENTIFIER,
                        IdlToken.LBRACE,
                        IdlToken.LBRACKET);
                switch (token) {
                    case IDENTIFIER:
                        if (tokenizer.isCurrentLexeme("true") || tokenizer.isCurrentLexeme("false")
                                || tokenizer.isCurrentLexeme("null")) {
                            NODE_KEYWORD.parse(tokenizer);
                        } else {
                            NODE_STRING_VALUE.parse(tokenizer);
                        }
                        break;
                    case STRING:
                    case TEXT_BLOCK:
                        NODE_STRING_VALUE.parse(tokenizer);
                        break;
                    case NUMBER:
                        NUMBER.parse(tokenizer);
                        break;
                    case LBRACE:
                        NODE_OBJECT.parse(tokenizer);
                        break;
                    case LBRACKET:
                    default:
                        NODE_ARRAY.parse(tokenizer);
                        break;
                }
            });
        }
    },

    NODE_ARRAY {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.LBRACKET);
                tokenizer.next();
                optionalWs(tokenizer);
                do {
                    if (tokenizer.getCurrentToken() == IdlToken.RBRACKET) {
                        break;
                    }
                    NODE_VALUE.parse(tokenizer);
                    optionalWs(tokenizer);
                } while (tokenizer.hasNext());
                tokenizer.expect(IdlToken.RBRACKET);
                tokenizer.next();
            });
        }
    },

    NODE_OBJECT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.LBRACE);
                tokenizer.next();
                optionalWs(tokenizer);

                while (tokenizer.hasNext()) {
                    if (tokenizer.expect(IdlToken.RBRACE, IdlToken.STRING, IdlToken.IDENTIFIER) == IdlToken.RBRACE) {
                        break;
                    }
                    NODE_OBJECT_KVP.parse(tokenizer);
                    optionalWs(tokenizer);
                }

                tokenizer.expect(IdlToken.RBRACE);
                tokenizer.next();
            });
        }
    },

    NODE_OBJECT_KVP {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                NODE_OBJECT_KEY.parse(tokenizer);
                optionalWs(tokenizer);
                tokenizer.expect(IdlToken.COLON);
                tokenizer.next();
                optionalWs(tokenizer);
                NODE_VALUE.parse(tokenizer);
            });
        }
    },

    NODE_OBJECT_KEY {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                if (tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.STRING) == IdlToken.IDENTIFIER) {
                    IDENTIFIER.parse(tokenizer);
                } else {
                    QUOTED_TEXT.parse(tokenizer);
                }
            });
        }
    },

    NODE_KEYWORD {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            // Assumes that the tokenizer is on "true"|"false"|"null".
            tokenizer.withState(this, tokenizer::next);
        }
    },

    NODE_STRING_VALUE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                switch (tokenizer.expect(IdlToken.STRING, IdlToken.TEXT_BLOCK, IdlToken.IDENTIFIER)) {
                    case STRING:
                        QUOTED_TEXT.parse(tokenizer);
                        break;
                    case TEXT_BLOCK:
                        TEXT_BLOCK.parse(tokenizer);
                        break;
                    case IDENTIFIER:
                    default:
                        SHAPE_ID.parse(tokenizer);
                }
            });
        }
    },

    QUOTED_TEXT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.STRING);
                tokenizer.next();
            });
        }
    },

    TEXT_BLOCK {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.TEXT_BLOCK);
                tokenizer.next();
            });
        }
    },

    NUMBER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.NUMBER);
                tokenizer.next();
            });
        }
    },

    SHAPE_ID {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                ROOT_SHAPE_ID.parse(tokenizer);
                if (tokenizer.getCurrentToken() == IdlToken.DOLLAR) {
                    SHAPE_ID_MEMBER.parse(tokenizer);
                }
            });
        }
    },

    ROOT_SHAPE_ID {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                IdlToken after = tokenizer
                        .peekWhile(0, t -> t == IdlToken.DOT || t == IdlToken.IDENTIFIER)
                        .getIdlToken();
                if (after == IdlToken.POUND) {
                    ABSOLUTE_ROOT_SHAPE_ID.parse(tokenizer);
                } else {
                    IDENTIFIER.parse(tokenizer);
                }
            });
        }
    },

    ABSOLUTE_ROOT_SHAPE_ID {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                NAMESPACE.parse(tokenizer);
                tokenizer.expect(IdlToken.POUND);
                tokenizer.next();
                IDENTIFIER.parse(tokenizer);
            });
        }
    },

    SHAPE_ID_MEMBER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.DOLLAR);
                tokenizer.next();
                IDENTIFIER.parse(tokenizer);
            });
        }
    },

    NAMESPACE {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                IDENTIFIER.parse(tokenizer);
                while (tokenizer.getCurrentToken() == IdlToken.DOT) {
                    tokenizer.next();
                    IDENTIFIER.parse(tokenizer);
                }
            });
        }
    },

    IDENTIFIER {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.IDENTIFIER);
                tokenizer.next();
            });
        }
    },

    SP {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.SPACE);
                while (tokenizer.getCurrentToken() == IdlToken.SPACE) {
                    tokenizer.next();
                }
            });
        }
    },

    WS {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(WS_CHARS);
                do {
                    switch (tokenizer.getCurrentToken()) {
                        case SPACE:
                            SP.parse(tokenizer);
                            break;
                        case NEWLINE:
                            tokenizer.next();
                            break;
                        case COMMA:
                            COMMA.parse(tokenizer);
                            break;
                        case COMMENT:
                        case DOC_COMMENT:
                            COMMENT.parse(tokenizer);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unexpected WS token: "
                                    + tokenizer.getCurrentToken());
                    }
                } while (TreeType.isToken(tokenizer, WS_CHARS));
            });
        }
    },

    COMMENT {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.COMMENT, IdlToken.DOC_COMMENT);
                tokenizer.next();
            });
        }
    },

    BR {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                optionalSpaces(tokenizer);
                switch (tokenizer.expect(IdlToken.NEWLINE, IdlToken.COMMENT, IdlToken.DOC_COMMENT, IdlToken.EOF)) {
                    case COMMENT:
                    case DOC_COMMENT:
                        COMMENT.parse(tokenizer);
                        optionalWs(tokenizer);
                        break;
                    case NEWLINE:
                        tokenizer.next();
                        optionalWs(tokenizer);
                        break;
                    case EOF:
                        tokenizer.eof();
                        break;
                    default:
                        break;
                }
            });
        }
    },

    COMMA {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            tokenizer.withState(this, () -> {
                tokenizer.expect(IdlToken.COMMA);
                tokenizer.next();
            });
        }
    },

    /**
     * An ERROR tree is created when a parser error is encountered; that is, any parse tree that contains this node
     * is an invalid model.
     */
    ERROR {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            throw new UnsupportedOperationException();
        }
    },

    /**
     * The innermost node of the token tree that contains an actual token returned from {@link IdlTokenizer}.
     */
    TOKEN {
        @Override
        void parse(CapturingTokenizer tokenizer) {
            throw new UnsupportedOperationException();
        }
    };

    // For now, this also skips doc comments. We may later move doc comments out of WS.
    private static final IdlToken[] WS_CHARS = {IdlToken.SPACE,
            IdlToken.NEWLINE,
            IdlToken.COMMA,
            IdlToken.COMMENT,
            IdlToken.DOC_COMMENT};

    abstract void parse(CapturingTokenizer tokenizer);

    private static boolean isToken(CapturingTokenizer tokenizer, IdlToken... tokens) {
        IdlToken currentTokenType = tokenizer.getCurrentToken();

        for (IdlToken token : tokens) {
            if (currentTokenType == token) {
                return true;
            }
        }

        return false;
    }

    protected static void optionalWs(CapturingTokenizer tokenizer) {
        if (isToken(tokenizer, WS_CHARS)) {
            WS.parse(tokenizer);
        }
    }

    protected static void optionalSpaces(CapturingTokenizer tokenizer) {
        if (tokenizer.getCurrentToken() == IdlToken.SPACE) {
            TreeType.SP.parse(tokenizer);
        }
    }

    protected static void parseShapeTypeAndName(CapturingTokenizer tokenizer) {
        parseShapeTypeAndName(tokenizer, null);
    }

    protected static void parseShapeTypeAndName(CapturingTokenizer tokenizer, TreeType typeName) {
        if (typeName == null) {
            tokenizer.next();
        } else {
            typeName.parse(tokenizer); // Skip the shape type
        }
        optionalSpaces(tokenizer);
        IDENTIFIER.parse(tokenizer); // shape name
        optionalSpaces(tokenizer);
    }

    protected static void parseSharedStructureBodyWithinInline(CapturingTokenizer tokenizer) {
        parseOptionalForResource(tokenizer);
        parseOptionalMixins(tokenizer);

        optionalWs(tokenizer);
        SHAPE_MEMBERS.parse(tokenizer);
    }

    protected static void parseOptionalForResource(CapturingTokenizer tokenizer) {
        optionalSpaces(tokenizer);
        if (tokenizer.isCurrentLexeme("for")) {
            FOR_RESOURCE.parse(tokenizer);
        }
    }

    protected static void parseOptionalMixins(CapturingTokenizer tokenizer) {
        optionalSpaces(tokenizer);
        if (tokenizer.isCurrentLexeme("with")) {
            MIXINS.parse(tokenizer);
        }
    }

    protected static void parseOptionalValueAssignment(CapturingTokenizer tokenizer) {
        if (tokenizer.peekPastSpaces().getIdlToken() == IdlToken.EQUAL) {
            VALUE_ASSIGNMENT.parse(tokenizer);
        }
    }

    protected static void operationInputOutputDefinition(CapturingTokenizer tokenizer) {
        if (tokenizer.expect(IdlToken.COLON, IdlToken.WALRUS) == IdlToken.COLON) {
            tokenizer.next();
            optionalWs(tokenizer);
            SHAPE_ID.parse(tokenizer);
        } else {
            INLINE_AGGREGATE_SHAPE.parse(tokenizer);
        }
    }
}
