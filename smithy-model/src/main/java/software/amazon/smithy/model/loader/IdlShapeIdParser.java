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

/**
 * Parses IDL shape IDs.
 */
final class IdlShapeIdParser {

    private IdlShapeIdParser() {}

    static String parseShapeId(IdlModelParser parser) {
        int start = parser.position();
        consumeShapeId(parser);
        return parser.sliceFrom(start);
    }

    static String parseIdentifier(IdlModelParser parser) {
        int start = parser.position();
        consumeIdentifier(parser);
        return parser.sliceFrom(start);
    }

    // identifier = (ALPHA / "_") *(ALPHA / DIGIT / "_")
    static void consumeIdentifier(IdlModelParser parser) {
        // (ALPHA / "_")
        if (!isIdentifierStart(parser.charPeek())) {
            throw parser.syntax("Expected a valid identifier character, but found '"
                                + parser.peekSingleCharForMessage() + '\'');
        }

        // *(ALPHA / DIGIT / "_")
        parser.consumeUntilNoLongerMatches(IdlShapeIdParser::isValidIdentifierCharacter);
    }

    private static boolean isValidIdentifierCharacter(char c) {
        return isIdentifierStart(c) || isDigit(c);
    }

    static String parseNamespace(IdlModelParser parser) {
        int start = parser.position();
        consumeNamespace(parser);
        return parser.sliceFrom(start);
    }

    static boolean isIdentifierStart(char c) {
        return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    static void consumeNamespace(IdlModelParser parser) {
        consumeIdentifier(parser);
        while (parser.charPeek() == '.') {
            parser.skip();
            consumeIdentifier(parser);
        }
    }

    private static void consumeShapeId(IdlModelParser parser) {
        consumeNamespace(parser);

        if (parser.charPeek() == '#') {
            parser.skip();
            consumeIdentifier(parser);
        }

        if (parser.charPeek() == '$') {
            parser.skip();
            consumeIdentifier(parser);
        }
    }
}
