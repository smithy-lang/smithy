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

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Represents a parsed token from the Smithy IDL.
 */
@SmithyUnstableApi
public enum IdlToken {

    SPACE(" ", true),
    NEWLINE("\\n", true),
    COMMA(",", true),
    COMMENT("//", true),
    DOC_COMMENT("///", false),
    AT("@", false),
    STRING("\"", false),
    TEXT_BLOCK("\"\"\"", false),
    COLON(":", false),
    WALRUS(":=", false),
    IDENTIFIER("", false),
    DOT(".", false),
    POUND("#", false),
    DOLLAR("$", false),
    NUMBER("", false),
    LBRACE("{", false),
    RBRACE("}", false),
    LBRACKET("[", false),
    RBRACKET("]", false),
    LPAREN("(", false),
    RPAREN(")", false),
    EQUAL("=", false),
    EOF("", false),
    ERROR("", false);

    private final String exampleLexeme;
    private final boolean isWhitespace;

    IdlToken(String exampleLexeme, boolean isWhitespace) {
        this.exampleLexeme = exampleLexeme;
        this.isWhitespace = isWhitespace;
    }

    public String getDebug() {
        return getDebug(exampleLexeme);
    }

    public String getDebug(CharSequence lexeme) {
        if (lexeme.length() > 0) {
            return this + "('" + lexeme + "')";
        }
        return toString();
    }

    public boolean isWhitespace() {
        return isWhitespace;
    }
}
