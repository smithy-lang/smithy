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

    SPACE(" ") {
        @Override
        public boolean isWhitespace() {
            return true;
        }
    },
    NEWLINE("\\n") {
        @Override
        public boolean isWhitespace() {
            return true;
        }
    },
    COMMA(",") {
        @Override
        public boolean isWhitespace() {
            return true;
        }
    },
    COMMENT("//") {
        @Override
        public boolean isWhitespace() {
            return true;
        }
    },
    DOC_COMMENT("///"),
    AT("@"),
    STRING("\""),
    TEXT_BLOCK("\"\"\""),
    COLON(":"),
    WALRUS(":="),
    IDENTIFIER(""),
    DOT("."),
    POUND("#"),
    DOLLAR("$"),
    NUMBER(""),
    LBRACE("{"),
    RBRACE("}"),
    LBRACKET("["),
    RBRACKET("]"),
    LPAREN("("),
    RPAREN(")"),
    EQUAL("="),
    EOF(""),
    ERROR("");

    private final String exampleLexeme;

    IdlToken(String exampleLexeme) {
        this.exampleLexeme = exampleLexeme;
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
        return false;
    }
}
