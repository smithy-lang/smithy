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

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.NumberNode;

/**
 * Parses IDL numbers.
 */
final class IdlNumberParser {

    private IdlNumberParser() {}

    static NumberNode parse(IdlModelParser parser) {
        SourceLocation location = parser.currentLocation();
        String lexeme = parseNumber(parser);
        if (lexeme.contains("e") || lexeme.contains(".")) {
            return new NumberNode(Double.valueOf(lexeme), location);
        } else {
            return new NumberNode(Long.parseLong(lexeme), location);
        }
    }

    // -?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?
    private static String parseNumber(IdlModelParser parser) {
        int startPosition = parser.position();

        char current = parser.charPeek();

        if (current == '-') {
            parser.skip();
            if (!IdlShapeIdParser.isDigit(parser.charPeek())) {
                throw parser.syntax(createInvalidString(
                        parser, startPosition, "'-' must be followed by a digit"));
            }
        }

        parser.consumeUntilNoLongerMatches(IdlShapeIdParser::isDigit);

        // Consume decimals.
        char peek = parser.charPeek();
        if (peek == '.') {
            parser.skip();
            if (parser.consumeUntilNoLongerMatches(IdlShapeIdParser::isDigit) == 0) {
                throw parser.syntax(createInvalidString(
                        parser, startPosition, "'.' must be followed by a digit"));
            }
        }

        // Consume scientific notation.
        peek = parser.charPeek();
        if (peek == 'e' || peek == 'E') {
            parser.skip();
            peek = parser.charPeek();
            if (peek == '+' || peek == '-') {
                parser.skip();
            }
            if (parser.consumeUntilNoLongerMatches(IdlShapeIdParser::isDigit) == 0) {
                throw parser.syntax(createInvalidString(
                        parser, startPosition, "'e', '+', and '-' must be followed by a digit"));
            }
        }

        return parser.sliceFrom(startPosition);
    }

    private static String createInvalidString(IdlModelParser parser, int startPosition, String message) {
        String lexeme = parser.sliceFrom(startPosition);
        return String.format("Invalid number '%s': %s", lexeme, message);
    }
}
