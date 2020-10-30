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
