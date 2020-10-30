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

package software.amazon.smithy.jmespath;

enum TokenType {

    EOF(null, -1),
    IDENTIFIER("A-Z|a-z|_", 0),
    LITERAL("`", 0),
    RBRACKET("]", 0),
    RPAREN(")", 0),
    COMMA(",", 0),
    RBRACE("]", 0),
    NUMBER("-|0-9", 0),
    CURRENT("@", 0),
    EXPREF("&", 0),
    COLON(":", 0),
    PIPE("|", 1),
    OR("||", 2),
    AND("&&", 3),
    EQUAL("==", 5),
    GREATER_THAN(">", 5),
    LESS_THAN("<", 5),
    GREATER_THAN_EQUAL(">=", 5),
    LESS_THAN_EQUAL("<=", 5),
    NOT_EQUAL("!=", 5),
    FLATTEN("[]", 9),

    // All tokens above stop a projection.
    STAR("*", 20),
    FILTER("[?", 21),
    DOT(".", 40),
    NOT("!", 45),
    LBRACE("{", 50),
    LBRACKET("[", 55),
    LPAREN("(", 60);

    final int lbp;
    final String lexeme;

    TokenType(String lexeme, int lbp) {
        this.lexeme = lexeme;
        this.lbp = lbp;
    }

    @Override
    public String toString() {
        if (lexeme != null) {
            return '\'' + lexeme.replace("'", "\\'") + '\'';
        } else {
            return super.toString();
        }
    }
}
