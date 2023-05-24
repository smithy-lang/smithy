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

// Trait =
//     "@" ShapeId [TraitBody]
// TraitBody =
//     "(" [WS] [TraitBodyValue] [WS] ")"
// TraitBodyValue =
//     TraitStructure / NodeValue
// TraitStructure =
//     TraitStructureKvp *([WS] TraitStructureKvp)
// TraitStructureKvp =
//     NodeObjectKey [WS] ":" [WS] NodeValue
final class TraitParser {

    private final CapturingTokenizer tokenizer;
    private final NodeParser nodeParser;

    TraitParser(CapturingTokenizer tokenizer, NodeParser nodeParser) {
        this.tokenizer = tokenizer;
        this.nodeParser = nodeParser;
    }

    void parse() {
        tokenizer.withState(TreeType.TRAIT, () -> {
            tokenizer.expect(IdlToken.AT);
            tokenizer.next();
            tokenizer.expectAndSkipShapeId(false);
            parseTraitValue();
        });
    }

    private TokenTree parseTraitValue() {
        return tokenizer.withState(TreeType.TRAIT_BODY, () -> {
            if (tokenizer.getCurrentToken() != IdlToken.LPAREN) {
                return;
            }

            tokenizer.next(); // skip "("
            tokenizer.skipWs();

            if (tokenizer.getCurrentToken() != IdlToken.RPAREN) {
                tokenizer.withState(TreeType.TRAIT_BODY_VALUE, this::parseTraitValueBody);
                tokenizer.skipWs();
            }

            tokenizer.expect(IdlToken.RPAREN); // Expect and skip ")"
            tokenizer.next();
        });
    }

    private void parseTraitValueBody(TokenTree tree) {
        tokenizer.expect(IdlToken.LBRACE, IdlToken.LBRACKET, IdlToken.TEXT_BLOCK, IdlToken.STRING,
                         IdlToken.NUMBER, IdlToken.IDENTIFIER);

        switch (tokenizer.getCurrentToken()) {
            case LBRACE:
            case LBRACKET:
            case TEXT_BLOCK:
            case NUMBER:
                // parse these as NODE_VALUE.
                nodeParser.parse();
                break;
            case STRING:
            case IDENTIFIER:
            default:
                parseTraitStructureOrNodeValue();
        }
    }

    private void parseTraitStructureOrNodeValue() {
        if (tokenizer.peekPastWs(1).getIdlToken() != IdlToken.COLON) {
            // It's a standalone string / identifier.
            if (tokenizer.getCurrentToken() == IdlToken.IDENTIFIER) {
                nodeParser.parseIdentifier();
            } else {
                tokenizer.next();
            }
        } else {
            tokenizer.withState(TreeType.TRAIT_STRUCTURE, () -> {
                do {
                    parseTraitStructureKvp();
                    tokenizer.skipWs();
                } while (tokenizer.getCurrentToken() != IdlToken.RPAREN && tokenizer.hasNext());
            });
        }
    }

    private void parseTraitStructureKvp() {
        tokenizer.withState(TreeType.TRAIT_STRUCTURE_KVP, () -> {
            nodeParser.parseNodeObjectKey();
            tokenizer.skipWs();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipWs();
            nodeParser.parse();
        });
    }
}
