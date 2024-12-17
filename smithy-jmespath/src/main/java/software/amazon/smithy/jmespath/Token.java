/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import software.amazon.smithy.jmespath.ast.LiteralExpression;

final class Token {

    /** The type of token. */
    final TokenType type;

    /** The nullable value contained in the token (e.g., a number or string). */
    final LiteralExpression value;

    /** The line where the token was parsed. */
    final int line;

    /** The column in the line where the token was parsed. */
    final int column;

    Token(TokenType type, LiteralExpression value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        if (value != null) {
            return '\'' + value.getValue().toString().replace("'", "\\'") + '\'';
        } else {
            return type.toString();
        }
    }
}
