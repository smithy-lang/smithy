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

import software.amazon.smithy.model.loader.IdlToken;

// NodeValue =
//     NodeArray
//   / NodeObject
//   / Number
//   / NodeKeyword
//   / NodeStringValue
//
// Number = <TOKEN>
//
// NodeKeyword =
//     %s"true" / %s"false" / %s"null"
//
// NodeStringValue =
//     ShapeId / TextBlock / QuotedText
//
// QuotedText = <TOKEN>
//
// TextBlock = <TOKEN>
final class NodeParser {

    private final CapturingTokenizer tokenizer;

    NodeParser(CapturingTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    TokenTree parse() {
        return tokenizer.withState(TreeType.NODE_VALUE, () -> {
            IdlToken token = tokenizer.expect(IdlToken.STRING, IdlToken.TEXT_BLOCK, IdlToken.NUMBER,
                                              IdlToken.IDENTIFIER, IdlToken.LBRACE, IdlToken.LBRACKET);
            switch (token) {
                case IDENTIFIER:
                    // Wrap identifier keywords in KEYWORD nodes.
                    parseIdentifier();
                    break;
                case STRING:
                    tokenizer.withState(TreeType.NODE_STRING_VALUE, () -> {
                        tokenizer.withState(TreeType.QUOTED_TEXT, tokenizer::next);
                    });
                    break;
                case TEXT_BLOCK:
                    tokenizer.withState(TreeType.NODE_STRING_VALUE, () -> {
                        tokenizer.withState(TreeType.TEXT_BLOCK, tokenizer::next);
                    });
                    break;
                case NUMBER:
                    tokenizer.withState(TreeType.NUMBER, tokenizer::next);
                    break;
                case LBRACE:
                    parseNodeObject();
                    break;
                case LBRACKET:
                default:
                    parseNodeArray();
                    break;
            }
        });
    }

    void parseIdentifier() {
        tokenizer.expect(IdlToken.IDENTIFIER);
        if (tokenizer.isCurrentLexeme("true")
                || tokenizer.isCurrentLexeme("false")
                || tokenizer.isCurrentLexeme("null")) {
            tokenizer.withState(TreeType.NODE_KEYWORD, tokenizer::next);
        } else {
            tokenizer.withState(TreeType.NODE_STRING_VALUE, () -> {
                tokenizer.expectAndSkipShapeId(true);
            });
        }
    }

    // NodeArray =
    //     "[" [WS] *(NodeValue [WS]) "]"
    void parseNodeArray() {
        tokenizer.withState(TreeType.NODE_ARRAY, () -> {
            tokenizer.expect(IdlToken.LBRACKET);
            tokenizer.next();
            tokenizer.skipWs();
            do {
                if (tokenizer.getCurrentToken() == IdlToken.RBRACKET) {
                    break;
                } else if (parse().hasChild(TreeType.ERROR)) {
                    // Stop trying to parse the array if an error occurred parsing the value.
                    return;
                } else {
                    tokenizer.skipWs();
                }
            } while (tokenizer.hasNext());
            tokenizer.expect(IdlToken.RBRACKET);
            tokenizer.next();
        });
    }

    // NodeObject =
    //     "{" [WS] [NodeObjectKvp *(WS NodeObjectKvp)] [WS] "}"
    //
    // NodeObjectKvp =
    //     NodeObjectKey [WS] ":" [WS] NodeValue
    void parseNodeObject() {
        tokenizer.withState(TreeType.NODE_OBJECT, () -> {
            tokenizer.expect(IdlToken.LBRACE);
            tokenizer.next();
            tokenizer.skipWs();

            while (tokenizer.hasNext()) {
                if (tokenizer.expect(IdlToken.RBRACE, IdlToken.STRING, IdlToken.IDENTIFIER) == IdlToken.RBRACE) {
                    break;
                }
                TokenTree kvp = tokenizer.withState(TreeType.NODE_OBJECT_KVP, () -> {
                    parseNodeObjectKey();
                    tokenizer.skipWs();
                    tokenizer.expect(IdlToken.COLON);
                    tokenizer.next();
                    tokenizer.skipWs();
                    parse();
                });
                if (kvp.hasChild(TreeType.ERROR)) {
                    // Stop trying to parse if an error occurred while parsing the kvp.
                    return;
                }
                tokenizer.skipWs();
            }

            tokenizer.expect(IdlToken.RBRACE);
            tokenizer.next();
        });
    }

    // NodeObjectKey =
    //     QuotedText / Identifier
    TokenTree parseNodeObjectKey() {
        return tokenizer.withState(TreeType.NODE_OBJECT_KEY, keyTree -> {
            tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.STRING);
            tokenizer.next();
        });
    }
}
