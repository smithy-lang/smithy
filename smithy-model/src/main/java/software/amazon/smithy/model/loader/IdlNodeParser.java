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
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.Pair;

/**
 * Parses IDL nodes.
 */
final class IdlNodeParser {

    private IdlNodeParser() {}

    static Node parseNode(IdlModelParser parser) {
        char c = parser.charPeek();
        switch (c) {
            case '{':
                return parseObjectNode(parser);
            case '[':
                return parseArrayNode(parser);
            case '"': {
                if (peekTextBlock(parser)) {
                    return parseTextBlock(parser);
                } else {
                    SourceLocation location = parser.currentLocation();
                    return new StringNode(IdlTextParser.parseQuotedString(parser), location);
                }
            }
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
                return IdlNumberParser.parse(parser);
            default: {
                SourceLocation location = parser.currentLocation();
                return parseNodeTextWithKeywords(parser, location, IdlShapeIdParser.parseShapeId(parser));
            }
        }
    }

    static Node parseNodeTextWithKeywords(IdlModelParser parser, SourceLocation location, String text) {
        switch (text) {
            case "true":
                return new BooleanNode(true, location);
            case "false":
                return new BooleanNode(false, location);
            case "null":
                return new NullNode(location);
            default:
                // Unquoted node values syntactically are assumed to be references
                // to shapes. A lazy string node is used because the shape ID may
                // not be able to be resolved until after the entire model is loaded.
                Pair<StringNode, Consumer<String>> pair = StringNode.createLazyString(text, location);
                Consumer<String> consumer = pair.right;
                parser.onShapeTarget(text, location, id -> consumer.accept(id.toString()));
                return pair.left;
        }
    }

    static boolean peekTextBlock(IdlModelParser parser) {
        return parser.charPeek() == '"'
               && parser.charPeek(1) == '"'
               && parser.charPeek(2) == '"';
    }

    static Node parseTextBlock(IdlModelParser parser) {
        SourceLocation location = parser.currentLocation();
        parser.expect('"');
        parser.expect('"');
        parser.expect('"');
        return new StringNode(IdlTextParser.parseQuotedTextAndTextBlock(parser, true), location);
    }

    static ObjectNode parseObjectNode(IdlModelParser parser) {
        SourceLocation location = parser.currentLocation();
        Map<StringNode, Node> entries = new LinkedHashMap<>();
        parser.expect('{');
        parser.ws();

        while (!parser.eof()) {
            char c = parser.charPeek();
            if (c == '}') {
                break;
            } else {
                SourceLocation keyLocation = parser.currentLocation();
                String key = parseNodeObjectKey(parser);
                parser.ws();
                parser.expect(':');
                parser.ws();
                Node value = parseNode(parser);
                entries.put(new StringNode(key, keyLocation), value);
                parser.ws();
                if (parser.charPeek() == ',') {
                    parser.skip();
                    parser.ws();
                } else {
                    break;
                }
            }
        }

        parser.expect('}');
        return new ObjectNode(entries, location);
    }

    static String parseNodeObjectKey(IdlModelParser parser) {
        if (parser.charPeek() == '"') {
            return IdlTextParser.parseQuotedString(parser);
        } else {
            return IdlShapeIdParser.parseIdentifier(parser);
        }
    }

    private static ArrayNode parseArrayNode(IdlModelParser parser) {
        SourceLocation location = parser.currentLocation();
        List<Node> items = new ArrayList<>();
        parser.expect('[');
        parser.ws();

        while (!parser.eof()) {
            char c = parser.charPeek();
            if (c == ']') {
                break;
            } else {
                items.add(parseNode(parser));
                parser.ws();
                if (parser.charPeek() == ',') {
                    parser.skip();
                    parser.ws();
                } else {
                    break;
                }
            }
        }

        parser.expect(']');
        return new ArrayNode(items, location);
    }
}
