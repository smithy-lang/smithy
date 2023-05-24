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

package software.amazon.smithy.syntax;

import java.util.function.Function;
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.shapes.ShapeType;

// ShapeStatements =
//     ShapeOrApplyStatement *(BR ShapeOrApplyStatement)
// ShapeOrApplyStatement =
//     ShapeStatement / ApplyStatement
// ShapeStatement =
//     TraitStatements ShapeBody
final class ShapeStatementsParser {

    private final CapturingTokenizer tokenizer;
    private final TraitParser traitParser;
    private final NodeParser nodeParser;

    ShapeStatementsParser(CapturingTokenizer tokenizer, NodeParser nodeParser) {
        this.tokenizer = tokenizer;
        this.traitParser = new TraitParser(tokenizer, nodeParser);
        this.nodeParser = nodeParser;
    }

    void parse() {
        tokenizer.withState(TreeType.SHAPE_STATEMENTS, tree -> {
            while (tokenizer.hasNext()) {
                TokenTree traits = parseTraits();
                tree.removeChild(traits);
                if (tokenizer.getCurrentToken() == IdlToken.IDENTIFIER) {
                    parseApplyOrShape(tree, traits);
                } else if (!traits.getChildren().isEmpty()) {
                    TokenTree errorTree = TokenTree.error("Found traits attached to nothing");
                    errorTree.appendChild(traits);
                    tree.appendChild(errorTree);
                }
            }
        });
    }

    private TokenTree parseTraits() {
        return tokenizer.withState(TreeType.TRAIT_STATEMENTS, () -> {
            while (tokenizer.getCurrentToken() == IdlToken.AT) {
                traitParser.parse();
                tokenizer.skipWs();
            }
        });
    }

    private void parseApplyOrShape(TokenTree tree, TokenTree traits) {
        String keyword = tokenizer.internString(tokenizer.getCurrentTokenLexeme());
        if (keyword.equals("apply")) {
            if (!traits.getChildren().isEmpty()) {
                TokenTree errorTree = TokenTree.error("Traits applied to apply statement");
                errorTree.appendChild(traits);
                tree.appendChild(errorTree);
            }
            parseApplyStatement();
        } else {
            parseShapeStatement(traits, ShapeType.fromString(keyword).orElseThrow(() -> {
                return new ModelSyntaxException("Expected a valid shape type",
                                                tokenizer.getCurrentTokenLocation());
            }));
        }
    }

    private void parseShapeStatement(TokenTree traits, ShapeType type) {
        tokenizer.withState(TreeType.SHAPE_OR_APPLY_STATEMENT, () -> {
            tokenizer.withState(TreeType.SHAPE_STATEMENT, shapeTree -> {
                // Add any previously parsed trait tree into the shape after doc comments.
                if (!traits.getChildren().isEmpty()) {
                    shapeTree.appendChild(traits);
                }

                tokenizer.withState(TreeType.SHAPE_BODY, () -> {
                    switch (type) {
                        case LIST:
                        case SET:
                            parseList();
                            break;
                        case MAP:
                            parseMapStatement();
                            break;
                        case STRUCTURE:
                            parseStructure();
                            break;
                        case UNION:
                            parseUnion();
                            break;
                        case SERVICE:
                            parseService();
                            break;
                        case RESOURCE:
                            parseResource();
                            break;
                        case OPERATION:
                            parseOperation();
                            break;
                        case ENUM:
                            parseEnumShape(TreeType.ENUM_STATEMENT);
                            break;
                        case INT_ENUM:
                            parseEnumShape(TreeType.INT_ENUM_STATEMENT);
                            break;
                        default:
                            if (type.getCategory() == ShapeType.Category.SIMPLE) {
                                parseSimpleShapeStatement();
                            } else {
                                throw new UnsupportedOperationException("Unexpected type: " + type);
                            }
                    }
                });
            });
        });

        tokenizer.expectAndSkipBr();
    }

    private void parseShapeTypeAndName() {
        tokenizer.next(); // skip the shape type / "apply"
        tokenizer.skipSpaces();
        tokenizer.skipRelativeRootShapeId(false);
        tokenizer.skipSpaces();
    }

    private void parseApplyStatement() {
        tokenizer.withState(TreeType.APPLY_STATEMENT, () -> {
            // Try to see if this is a singular or block apply statement.
            IdlToken peek = tokenizer.peekWhile(1, t -> t != IdlToken.EOF && t != IdlToken.AT && t != IdlToken.LBRACE)
                    .getIdlToken();
            TreeType subType = peek == IdlToken.LBRACE
                    ? TreeType.APPLY_STATEMENT_BLOCK
                    : TreeType.APPLY_STATEMENT_SINGULAR;
            tokenizer.withState(subType, () -> {
                tokenizer.next();
                tokenizer.expectAndSkipSpaces();
                tokenizer.expectAndSkipShapeId(true);
                tokenizer.expectAndSkipWhitespace();
                if (tokenizer.getCurrentToken() == IdlToken.LBRACE) {
                    tokenizer.next();
                    tokenizer.skipWs();
                    parseTraits();
                    tokenizer.skipWs();
                    tokenizer.expect(IdlToken.RBRACE);
                    tokenizer.next();
                } else {
                    traitParser.parse();
                }
            });
        });
        tokenizer.expectAndSkipBr();
    }

    // SimpleShapeStatement =
    //     SimpleTypeName SP Identifier [Mixins]
    private void parseSimpleShapeStatement() {
        tokenizer.withState(TreeType.SIMPLE_SHAPE_STATEMENT, () -> {
            parseShapeTypeAndName();
            parseMixins();
        });
    }

    // Mixins =
    //     [SP] %s"with" [WS] "[" [WS] 1*(ShapeId [WS]) "]"
    private void parseMixins() {
        if (tokenizer.isCurrentLexeme("with")) {
            tokenizer.withState(TreeType.SHAPE_MIXINS, () -> {
                tokenizer.next(); // skip with
                tokenizer.skipWs();

                tokenizer.expect(IdlToken.LBRACKET);
                tokenizer.next();
                tokenizer.skipWs();

                do {
                    tokenizer.expectAndSkipShapeId(false);
                    tokenizer.skipWs();
                } while (tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.RBRACKET) == IdlToken.IDENTIFIER);

                tokenizer.skipWs();
                tokenizer.expect(IdlToken.RBRACKET);
                tokenizer.next();
            });
        }
    }

    // ListStatement =
    //     %s"list" SP Identifier [Mixins] [WS] ListMembers
    //
    // ListMembers =
    //     "{" [WS] [ListMember] [WS] "}"
    //
    // ListMember =
    //     TraitStatements (ElidedListMember / ExplicitListMember)
    //
    // ElidedListMember =
    //     %s"$member"
    //
    // ExplicitListMember =
    //     %s"member" [SP] ":" [SP] ShapeId
    private void parseList() {
        tokenizer.withState(TreeType.LIST_STATEMENT, () -> {
            parseShapeTypeAndName();
            tokenizer.withState(TreeType.LIST_MEMBERS, () -> {
                openNonStructure();
                tokenizer.withState(TreeType.LIST_MEMBER, () -> {
                    parsePossiblyElidedMember(expectMemberFunctor("member", TreeType.EXPLICIT_LIST_MEMBER),
                                              expectMemberFunctor("member", TreeType.ELIDED_LIST_MEMBER),
                                              MemberType.LIST);
                });
                closeBracedShape();
            });
        });
    }

    private Function<String, TreeType> expectMemberFunctor(String expected, TreeType tree) {
        return actual -> {
            if (expected.equals(actual)) {
                return tree;
            }
            throw new ModelSyntaxException("Expected a member named '" + expected + "', but found '" + actual + "'",
                                           tokenizer.getCurrentTokenLocation());
        };
    }

    private void openNonStructure() {
        parseMixins();
        tokenizer.skipWs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWs();
    }

    private void closeBracedShape() {
        tokenizer.skipWs();
        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
    }

    private enum MemberType {
        LIST(false),
        MAP_MEMBER(false),
        UNION(false),
        STRUCTURE(true);

        private final boolean allowsAssignment;

        MemberType(boolean allowsAssignment) {
            this.allowsAssignment = allowsAssignment;
        }
    }

    private static final class MemberHolder {
        private String member;
    }

    private String parsePossiblyElidedMember(
            Function<String, TreeType> explicitType,
            Function<String, TreeType> elidedType,
            MemberType memberType
    ) {
        MemberHolder holder = new MemberHolder();

        tokenizer.withState(TreeType.SHAPE_MEMBER, () -> {
            parseTraits();

            if (tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.DOLLAR) == IdlToken.DOLLAR) {
                String memberName = tokenizer.internString(tokenizer.peek(1).getLexeme());
                holder.member = memberName;
                tokenizer.withState(elidedType.apply(memberName), () -> {
                    tokenizer.next(); // skip "$"
                    tokenizer.withState(TreeType.SHAPE_MEMBER_NAME, () -> {
                        tokenizer.expect(IdlToken.IDENTIFIER);
                        tokenizer.next();
                    });
                });
            } else {
                String memberName = tokenizer.internString(tokenizer.getCurrentTokenLexeme());
                holder.member = memberName;
                tokenizer.withState(explicitType.apply(memberName), () -> {
                    tokenizer.withState(TreeType.SHAPE_MEMBER_NAME, tokenizer::next);
                    tokenizer.skipSpaces();
                    tokenizer.expect(IdlToken.COLON);
                    tokenizer.next();
                    tokenizer.skipSpaces();
                    tokenizer.expectAndSkipShapeId(false);

                    if (memberType.allowsAssignment && tokenizer.peekPastSpaces(0).getIdlToken() == IdlToken.EQUAL) {
                        parseShapeMemberValueAssignment();
                    } else {
                        tokenizer.skipWs();
                    }
                });
            }
        });

        return holder.member;
    }

    // MapStatement =
    //     %s"map" SP Identifier [Mixins] [WS] MapMembers
    //
    // MapMembers =
    //     "{" [WS] [MapKey / MapValue / (MapKey WS MapValue)] [WS] "}"
    //
    // MapKey =
    //     TraitStatements (ElidedMapKey / ExplicitMapKey)
    //
    // MapValue =
    //     TraitStatements (ElidedMapValue / ExplicitMapValue)
    //
    // ElidedMapKey =
    //     %s"$key"
    //
    // ExplicitMapKey =
    //     %s"key" [SP] ":" [SP] ShapeId
    //
    // ElidedMapValue =
    //     %s"$value"
    //
    // ExplicitMapValue =
    //     %s"value" [SP] ":" [SP] ShapeId
    private void parseMapStatement() {
        tokenizer.withState(TreeType.MAP_STATEMENT, () -> {
            parseShapeTypeAndName();
            tokenizer.withState(TreeType.MAP_MEMBERS, () -> {
                openNonStructure();
                if (tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                    String member = parsePossiblyElidedMember(
                        this::getExplicitMapKeyOrValue,
                        this::getElidedMapKeyOrValue,
                        MemberType.MAP_MEMBER
                    );
                    tokenizer.skipWs();
                    if (tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                        // If a key was defined, then there can be no more members. Key and value members are ordered.
                        if (member.equals("value")) {
                            tokenizer.expect(IdlToken.RBRACE);
                        } else {
                            // The member was a key, so only a value can be provided now.
                            parsePossiblyElidedMember(
                                    expectMemberFunctor("value", TreeType.EXPLICIT_MAP_VALUE),
                                    expectMemberFunctor("value", TreeType.ELIDED_MAP_VALUE),
                                    MemberType.MAP_MEMBER);
                            tokenizer.skipWs();
                        }
                    }
                }
                closeBracedShape();
            });
        });
    }

    private TreeType getExplicitMapKeyOrValue(String member) {
        switch (member) {
            case "key":
                return TreeType.EXPLICIT_MAP_KEY;
            case "value":
                return TreeType.EXPLICIT_MAP_VALUE;
            default:
                throw new ModelSyntaxException("Invalid map member. Expected 'key' or 'value', found '" + member + "'",
                                               tokenizer.getCurrentTokenLocation());
        }
    }

    private TreeType getElidedMapKeyOrValue(String member) {
        switch (member) {
            case "key":
                return TreeType.ELIDED_MAP_KEY;
            case "value":
                return TreeType.ELIDED_MAP_VALUE;
            default:
                throw new ModelSyntaxException("Invalid map member. Expected 'key' or 'value', found '" + member + "'",
                                               tokenizer.getCurrentTokenLocation());
        }
    }

    // UnionStatement =
    //     %s"union" SP Identifier [Mixins] [WS] UnionMembers
    //
    // UnionMembers =
    //     "{" [WS] *(TraitStatements UnionMember [WS]) "}"
    //
    // UnionMember =
    //     (ExplicitStructureMember / ElidedStructureMember)
    private void parseUnion() {
        tokenizer.withState(TreeType.UNION_STATEMENT, () -> {
            parseShapeTypeAndName();
            openNonStructure();
            while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                tokenizer.withState(TreeType.UNION_MEMBER, () -> {
                    parsePossiblyElidedMember(any -> TreeType.EXPLICIT_STRUCTURE_MEMBER,
                                              any -> TreeType.ELIDED_STRUCTURE_MEMBER,
                                              MemberType.UNION);
                });
            }
            closeBracedShape();
        });
    }

    private void parseStructure() {
        tokenizer.withState(TreeType.STRUCTURE_STATEMENT, () -> {
            parseShapeTypeAndName();
            tokenizer.skipSpaces();
            parseSharedStructureBodyWithinInline();
        });
    }

    private void parseSharedStructureBodyWithinInline() {
        if (tokenizer.isCurrentLexeme("for")) {
            tokenizer.withState(TreeType.STRUCTURE_RESOURCE, () -> {
                tokenizer.next();
                tokenizer.expectAndSkipSpaces();
                tokenizer.expectAndSkipShapeId(false);
            });
            tokenizer.skipSpaces();
        }

        parseMixins();
        tokenizer.skipWs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWs();

        while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
            tokenizer.withState(TreeType.STRUCTURE_MEMBER, () -> {
                parsePossiblyElidedMember(
                        any -> TreeType.EXPLICIT_STRUCTURE_MEMBER,
                        any -> TreeType.ELIDED_STRUCTURE_MEMBER,
                        MemberType.STRUCTURE);
            });
        }

        closeBracedShape();
    }

    private void parseShapeMemberValueAssignment() {
        tokenizer.withState(TreeType.VALUE_ASSIGNMENT, () -> {
            tokenizer.skipSpaces();
            tokenizer.expect(IdlToken.EQUAL);
            tokenizer.next();
            tokenizer.skipSpaces();
            nodeParser.parse();
            skipOptionalComma();
            tokenizer.expectAndSkipBr();
        });
    }

    private void skipOptionalComma() {
        tokenizer.skipSpaces();
        if (tokenizer.getCurrentToken() == IdlToken.COMMA) {
            tokenizer.withState(TreeType.COMMA, tokenizer::next);
        }
        tokenizer.skipSpaces();
    }

    // ServiceStatement =
    //     %s"service" SP Identifier [Mixins] [WS] NodeObject
    private void parseService() {
        tokenizer.withState(TreeType.SERVICE_STATEMENT, () -> {
            parseShapeTypeAndName();
            parseServiceAndResource();
        });
    }

    // ResourceStatement =
    //     %s"resource" SP Identifier [Mixins] [WS] NodeObject
    private void parseResource() {
        tokenizer.withState(TreeType.RESOURCE_STATEMENT, () -> {
            parseShapeTypeAndName();
            parseServiceAndResource();
        });
    }

    private void parseServiceAndResource() {
        parseMixins();
        tokenizer.skipWs();
        tokenizer.expect(IdlToken.LBRACE);
        nodeParser.parse(); // parses an object
    }

    // This parser differs from the grammar in that enum shapes are wrapped in an ENUM_SHAPE_STATEMENT tree whereas
    // intEnum shapes are wrapped in an INT_ENUM_STATEMENT tree.
    //
    // EnumStatement =
    //     EnumTypeName SP Identifier [Mixins] [WS] EnumShapeMembers
    //
    // EnumTypeName =
    //     %s"enum" / %s"intEnum"
    //
    // EnumShapeMembers =
    //     "{" [WS] 1*(TraitStatements Identifier [ValueAssignment] [WS]) "}"
    private void parseEnumShape(TreeType type) {
        tokenizer.withState(type, () -> {
            parseShapeTypeAndName();
            openNonStructure();

            while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                tokenizer.withState(TreeType.SHAPE_MEMBER, () -> {
                    parseTraits();
                    tokenizer.expect(IdlToken.IDENTIFIER);
                    tokenizer.withState(TreeType.SHAPE_MEMBER_NAME, tokenizer::next);
                    if (tokenizer.peekPastSpaces(0).getIdlToken() == IdlToken.EQUAL) {
                        parseShapeMemberValueAssignment();
                    } else {
                        tokenizer.skipWs();
                    }
                });
            }

            closeBracedShape();
        });
    }

    // OperationStatement =
    //     %s"operation" SP Identifier [Mixins] [WS] OperationBody
    //
    // OperationBody =
    //     "{" [WS]
    //       *(OperationInput / OperationOutput / OperationErrors)
    //       [WS] "}"
    //       ; only one of each property can be specified.
    //
    // OperationInput =
    //     %s"input" [WS] (InlineStructure / (":" [WS] ShapeId))
    //
    // OperationOutput =
    //     %s"output" [WS] (InlineStructure / (":" [WS] ShapeId))
    //
    // OperationErrors =
    //     %s"errors" [WS] ":" [WS] "[" [WS] *(ShapeId [WS]) "]"
    //
    // InlineStructure =
    //     ":=" [WS] TraitStatements [StructureResource]
    //           [Mixins] [WS] StructureMembers
    private void parseOperation() {
        tokenizer.withState(TreeType.OPERATION_STATEMENT, () -> {
            parseShapeTypeAndName();
            openNonStructure();
            while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
                tokenizer.expect(IdlToken.IDENTIFIER);
                if (tokenizer.isCurrentLexeme("input")) {
                    parseOperationInput();
                } else if (tokenizer.isCurrentLexeme("output")) {
                    parseOperationOutput();
                } else if (tokenizer.isCurrentLexeme("errors")) {
                    parseOperationErrors();
                } else {
                    throw new ModelSyntaxException("Expected 'input', 'output', or 'errors'. Found '"
                                                   + tokenizer.getCurrentTokenLexeme() + "'",
                                                   tokenizer.getCurrentTokenLocation());
                }
                tokenizer.skipWs();
            }
            closeBracedShape();
        });
    }

    private void parseOperationInput() {
        tokenizer.withState(TreeType.OPERATION_INPUT, this::parseInputOrOutput);
    }

    private void parseOperationOutput() {
        tokenizer.withState(TreeType.OPERATION_OUTPUT, this::parseInputOrOutput);
    }

    private void parseInputOrOutput() {
        tokenizer.next();
        tokenizer.skipWs();
        if (tokenizer.expect(IdlToken.COLON, IdlToken.WALRUS) == IdlToken.WALRUS) {
            tokenizer.withState(TreeType.INLINE_STRUCTURE, () -> {
                tokenizer.next(); // skip ":="
                tokenizer.skipWs();
                parseSharedStructureBodyWithinInline();
            });
        } else {
            tokenizer.next();
            tokenizer.skipWs();
            tokenizer.expectAndSkipShapeId(false);
        }
    }

    private void parseOperationErrors() {
        tokenizer.withState(TreeType.OPERATION_ERRORS, () -> {
            tokenizer.next(); // skip "errors"
            tokenizer.skipWs();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipWs();
            tokenizer.expect(IdlToken.LBRACKET);
            tokenizer.next();
            tokenizer.skipWs();
            while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACKET) {
                tokenizer.expectAndSkipShapeId(false);
                tokenizer.skipWs();
            }
            tokenizer.expect(IdlToken.RBRACKET);
            tokenizer.next();
        });
    }
}
