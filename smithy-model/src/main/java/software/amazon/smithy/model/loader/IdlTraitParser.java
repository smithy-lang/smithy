/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

final class IdlTraitParser {

    private IdlTraitParser() {}

    // trait_statements = *(ws trait)
    static List<IdlModelParser.TraitEntry> parseTraits(IdlModelParser parser) {
        List<IdlModelParser.TraitEntry> entries = new ArrayList<>();
        while (parser.peek() == '@') {
            entries.add(parseTraitValue(parser));
            parser.ws();
        }

        return entries;
    }

    static IdlModelParser.TraitEntry parseTraitValue(IdlModelParser parser) {
        // "@" shape_id
        parser.expect('@');
        String id = ParserUtils.parseShapeId(parser);

        // No (): it's an annotation trait.
        if (parser.peek() != '(') {
            return new IdlModelParser.TraitEntry(id, new NullNode(parser.currentLocation()), true);
        }

        parser.expect('(');
        parser.ws();

        // (): it's also an annotation trait.
        if (parser.peek() == ')') {
            parser.expect(')');
            return new IdlModelParser.TraitEntry(id, new NullNode(parser.currentLocation()), true);
        }

        // The trait has a value between the '(' and ')'.
        Node value = parseTraitValueBody(parser);
        parser.ws();
        parser.expect(')');

        return new IdlModelParser.TraitEntry(id, value, false);
    }

    private static Node parseTraitValueBody(IdlModelParser parser) {
        SourceLocation keyLocation = parser.currentLocation();
        char c = parser.peek();
        switch (c) {
            case '{':
            case '[':
                // {} and [] are always node values.
                return IdlNodeParser.parseNode(parser);
            case '"': {
                // Text blocks are always node values.
                if (IdlNodeParser.peekTextBlock(parser)) {
                    return IdlNodeParser.parseTextBlock(parser);
                }
                // It's a quoted string, so check if it's a KVP key or a node_value.
                String key = IdlTextParser.parseQuotedString(parser);
                return parseTraitValueBodyIdentifierOrQuotedString(parser, keyLocation, key, false);
            } default: {
                // Parser numbers.
                if (c == '-' || ParserUtils.isDigit(c)) {
                    return parser.parseNumberNode();
                } else {
                    // Parse unquoted strings or possibly a structured trait.
                    String key = ParserUtils.parseIdentifier(parser);
                    return parseTraitValueBodyIdentifierOrQuotedString(parser, keyLocation, key, true);
                }
            }
        }
    }

    private static Node parseTraitValueBodyIdentifierOrQuotedString(
            IdlModelParser parser,
            SourceLocation location,
            String key,
            boolean unquoted
    ) {
        parser.ws();

        // If the next character is ':', this it's a KVP.
        if (parser.peek() == ':') {
            parser.expect(':');
            parser.ws();
            return parseStructuredTrait(parser, new StringNode(key, location));
        } else if (unquoted) {
            // It's a node_value that's either a keyword or shape ID.
            return IdlNodeParser.parseNodeTextWithKeywords(parser, location, key);
        } else {
            // It's a quoted string node_value.
            return new StringNode(key, location);
        }
    }

    private static ObjectNode parseStructuredTrait(IdlModelParser parser, StringNode startingKey) {
        Map<StringNode, Node> entries = new LinkedHashMap<>();
        Node firstValue = IdlNodeParser.parseNode(parser);
        // This put call can be done safely without checking for duplicates,
        // as it's always the first member of the trait.
        entries.put(startingKey, firstValue);
        parser.ws();

        while (!parser.eof() && parser.peek() != ')') {
            char c = parser.peek();
            if (ParserUtils.isIdentifierStart(c) || c == '"') {
                parseTraitStructureKvp(parser, entries);
            } else {
                throw parser.syntax("Unexpected object key character: '" + c + '\'');
            }
        }

        return new ObjectNode(entries, startingKey.getSourceLocation());
    }

    private static void parseTraitStructureKvp(IdlModelParser parser, Map<StringNode, Node> entries) {
        SourceLocation keyLocation = parser.currentLocation();
        String key = IdlNodeParser.parseNodeObjectKey(parser);
        StringNode nextKey = new StringNode(key, keyLocation);
        parser.ws();
        parser.expect(':');
        parser.ws();
        Node nextValue = IdlNodeParser.parseNode(parser);
        parser.ws();
        Node previous = entries.put(nextKey, nextValue);
        if (previous != null) {
            throw parser.syntax("Duplicate member of trait: '" + nextKey.getValue() + '\'');
        }
    }
}
