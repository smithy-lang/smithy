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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.Trait;

final class IdlTraitParser {

    // A pending trait that also doesn't yet have a resolved trait shape ID.
    static final class Result {
        private final CharSequence traitName;
        private final Node value;
        private final TraitType traitType;

        Result(CharSequence traitName, Node value, TraitType traitType) {
            this.traitName = traitName;
            this.value = value;
            this.traitType = traitType;
        }

        CharSequence getTraitName() {
            return traitName;
        }

        Node getValue() {
            return value;
        }

        TraitType getTraitType() {
            return traitType;
        }
    }

    enum TraitType {
        VALUE, ANNOTATION, DOC_COMMENT
    }

    private IdlTraitParser() { }

    /**
     * Assumes that the tokenizer is potentially before a shape or member definition, and parses out documentation
     * comments and applied traits into a list of {@link Result} values that can later be turned into instances of
     * {@link Trait}s to apply to shapes.
     *
     * @param loader IDL parser.
     * @return Return the parsed traits.
     */
    static List<Result> parseDocsAndTraitsBeforeShape(IdlModelLoader loader) {
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        tokenizer.skipWs();

        Result docComment = null;

        // Mark the position of where documentation comments start if on a doc comment.
        if (tokenizer.getCurrentToken() == IdlToken.DOC_COMMENT) {
            SourceLocation documentationLocation = tokenizer.getCurrentTokenLocation();
            tokenizer.skipWsAndDocs();
            docComment = parseDocComment(tokenizer, documentationLocation);
        } else {
            tokenizer.skipWsAndDocs();
        }

        // Parse traits, if any.
        tokenizer.skipWsAndDocs();
        List<Result> traits = expectAndSkipTraits(loader);
        if (docComment != null) {
            traits.add(docComment);
        }
        tokenizer.skipWsAndDocs();

        return traits;
    }

    private static Result parseDocComment(IdlInternalTokenizer tokenizer, SourceLocation location) {
        String result = tokenizer.removePendingDocCommentLines();
        if (result == null) {
            return null;
        } else {
            Node value = new StringNode(result, location);
            return new Result(DocumentationTrait.ID.toString(), value, TraitType.DOC_COMMENT);
        }
    }

    /**
     * Parse all traits before a shape or member, or inside an {@code apply} block.
     *
     * @param loader   IDL parser.
     * @return Returns the parsed traits.
     */
    static List<Result> expectAndSkipTraits(IdlModelLoader loader) {
        List<Result> results = new ArrayList<>();
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        while (tokenizer.getCurrentToken() == IdlToken.AT) {
            results.add(expectAndSkipTrait(loader));
            tokenizer.skipWsAndDocs();
        }
        return results;
    }

    /**
     * Parses a single trait: "@" trait-id [(trait-body)].
     *
     * @param loader   IDL parser.
     * @return Returns the parsed trait.
     */
    static Result expectAndSkipTrait(IdlModelLoader loader) {
        // "@" shape_id
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        SourceLocation location = tokenizer.getCurrentTokenLocation();
        tokenizer.expect(IdlToken.AT);
        tokenizer.next();
        CharSequence id = IdlShapeIdParser.expectAndSkipShapeId(tokenizer);

        // No (): it's an annotation trait.
        if (tokenizer.getCurrentToken() != IdlToken.LPAREN) {
            return new Result(id, new NullNode(location), TraitType.ANNOTATION);
        }

        tokenizer.next();
        tokenizer.skipWsAndDocs();

        // (): it's also an annotation trait.
        if (tokenizer.getCurrentToken() == IdlToken.RPAREN) {
            tokenizer.next();
            return new Result(id, new NullNode(location), TraitType.ANNOTATION);
        }

        // The trait has a value between the '(' and ')'.
        Node value = parseTraitValueBody(loader, location);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.RPAREN);
        tokenizer.next();

        return new Result(id, value, TraitType.VALUE);
    }

    private static Node parseTraitValueBody(IdlModelLoader loader, SourceLocation location) {
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        tokenizer.expect(IdlToken.LBRACE, IdlToken.LBRACKET, IdlToken.TEXT_BLOCK, IdlToken.STRING,
                         IdlToken.NUMBER, IdlToken.IDENTIFIER);

        switch (tokenizer.getCurrentToken()) {
            case LBRACE:
            case LBRACKET:
                Node result = IdlNodeParser.expectAndSkipNode(loader, location);
                tokenizer.skipWsAndDocs();
                return result;
            case TEXT_BLOCK:
                Node textBlockResult = new StringNode(tokenizer.getCurrentTokenStringSlice().toString(), location);
                tokenizer.next();
                tokenizer.skipWsAndDocs();
                return textBlockResult;
            case NUMBER:
                Number number = tokenizer.getCurrentTokenNumberValue();
                tokenizer.next();
                tokenizer.skipWsAndDocs();
                return new NumberNode(number, location);
            case STRING:
                String stringValue = tokenizer.getCurrentTokenStringSlice().toString();
                StringNode stringNode = new StringNode(stringValue, location);
                tokenizer.next();
                tokenizer.skipWsAndDocs();
                if (tokenizer.getCurrentToken() == IdlToken.COLON) {
                    tokenizer.next();
                    tokenizer.skipWsAndDocs();
                    return parseStructuredTrait(loader, stringNode);
                } else {
                    return stringNode;
                }
            case IDENTIFIER:
            default:
                // Handle: `foo`, `foo$bar`, `foo.bar#baz`, `foo.bar#baz$bam`, `foo: bam`
                String identifier = loader.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
                tokenizer.skipWsAndDocs();
                if (tokenizer.getCurrentToken() == IdlToken.RPAREN || isItDefinitelyShapeId(identifier)) {
                    return IdlNodeParser.createIdentifier(loader, identifier, location);
                } else {
                    tokenizer.expect(IdlToken.COLON);
                    tokenizer.next();
                    tokenizer.skipWsAndDocs();
                    return parseStructuredTrait(loader, new StringNode(identifier, location));
                }
        }
    }

    private static boolean isItDefinitelyShapeId(String identifier) {
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (c == '.' || c == '$' || c == '#') {
                return true;
            }
        }
        return false;
    }

    private static ObjectNode parseStructuredTrait(IdlModelLoader loader, StringNode firstKey) {
        IdlInternalTokenizer tokenizer = loader.getTokenizer();
        loader.increaseNestingLevel();
        Map<StringNode, Node> entries = new LinkedHashMap<>();
        Node firstValue = IdlNodeParser.expectAndSkipNode(loader);

        // This put call can be done safely without checking for duplicates,
        // as it's always the first member of the trait.
        entries.put(firstKey, firstValue);
        tokenizer.skipWsAndDocs();

        while (tokenizer.getCurrentToken() != IdlToken.RPAREN) {
            tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.STRING);
            String key = loader.internString(tokenizer.getCurrentTokenStringSlice());
            StringNode keyNode = new StringNode(key, tokenizer.getCurrentTokenLocation());
            tokenizer.next();
            tokenizer.skipWsAndDocs();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipWsAndDocs();
            Node nextValue = IdlNodeParser.expectAndSkipNode(loader);
            Node previous = entries.put(keyNode, nextValue);
            if (previous != null) {
                throw new ModelSyntaxException("Duplicate member of trait: '" + keyNode.getValue() + '\'', keyNode);
            }
            tokenizer.skipWsAndDocs();
        }

        loader.decreaseNestingLevel();
        return new ObjectNode(entries, firstKey.getSourceLocation());
    }
}
