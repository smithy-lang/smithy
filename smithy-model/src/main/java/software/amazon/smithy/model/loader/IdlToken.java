/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
