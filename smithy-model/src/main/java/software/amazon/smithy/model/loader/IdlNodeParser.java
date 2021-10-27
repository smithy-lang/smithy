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
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;

/**
 * Parses IDL nodes.
 */
final class IdlNodeParser {

    private static final String SYNTACTIC_SHAPE_ID_TARGET = "SyntacticShapeIdTarget";

    private IdlNodeParser() {}

    static Node parseNode(IdlModelParser parser) {
        char c = parser.peek();
        switch (c) {
            case '{':
                return parseObjectNode(parser, "object node");
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
                return parser.parseNumberNode();
            default: {
                SourceLocation location = parser.currentLocation();
                return parseNodeTextWithKeywords(parser, location, ParserUtils.parseShapeId(parser));
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
                parser.modelFile.addForwardReference(text, (id, typeFunction) -> {
                    if (typeFunction.apply(id) == null) {
                        parser.modelFile.events().add(ValidationEvent.builder()
                                .id(SYNTACTIC_SHAPE_ID_TARGET)
                                .severity(Severity.DANGER)
                                .message(String.format("Syntactic shape ID `%s` does not resolve to a valid shape ID: "
                                                       + "`%s`. Did you mean to quote this string? Are you missing a "
                                                       + "model file?", text, id))
                                .sourceLocation(location)
                                .build());
                    }
                    consumer.accept(id.toString());
                });
                return pair.left;
        }
    }

    static boolean peekTextBlock(IdlModelParser parser) {
        return parser.peek() == '"'
               && parser.peek(1) == '"'
               && parser.peek(2) == '"';
    }

    static Node parseTextBlock(IdlModelParser parser) {
        SourceLocation location = parser.currentLocation();
        parser.expect('"');
        parser.expect('"');
        parser.expect('"');
        return new StringNode(IdlTextParser.parseQuotedTextAndTextBlock(parser, true), location);
    }

    static ObjectNode parseObjectNode(IdlModelParser parser, String parent) {
        parser.increaseNestingLevel();
        SourceLocation location = parser.currentLocation();
        Map<StringNode, Node> entries = new LinkedHashMap<>();
        parser.expect('{');
        parser.ws();

        while (!parser.eof()) {
            char c = parser.peek();
            if (c == '}') {
                break;
            } else {
                SourceLocation keyLocation = parser.currentLocation();
                String key = parseNodeObjectKey(parser);
                parser.ws();
                parser.expect(':');
                if (parser.peek() == '=') {
                    throw parser.syntax("The `:=` syntax may only be used when defining inline operation input and "
                            + "output shapes.");
                }
                parser.ws();
                Node value = parseNode(parser);
                StringNode keyNode = new StringNode(key, keyLocation);
                Node previous = entries.put(keyNode, value);
                if (previous != null) {
                    throw parser.syntax("Duplicate member of " + parent + ": '" + keyNode.getValue() + '\'');
                }
                parser.ws();
            }
        }

        parser.expect('}');
        parser.decreaseNestingLevel();
        return new ObjectNode(entries, location);
    }

    static String parseNodeObjectKey(IdlModelParser parser) {
        if (parser.peek() == '"') {
            return IdlTextParser.parseQuotedString(parser);
        } else {
            return ParserUtils.parseIdentifier(parser);
        }
    }

    private static ArrayNode parseArrayNode(IdlModelParser parser) {
        parser.increaseNestingLevel();
        SourceLocation location = parser.currentLocation();
        List<Node> items = new ArrayList<>();
        parser.expect('[');
        parser.ws();

        while (!parser.eof()) {
            char c = parser.peek();
            if (c == ']') {
                break;
            } else {
                items.add(parseNode(parser));
                parser.ws();
            }
        }

        parser.expect(']');
        parser.decreaseNestingLevel();
        return new ArrayNode(items, location);
    }
}
