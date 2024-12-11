/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
