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

package software.amazon.smithy.syntax;

import java.util.function.Function;
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;

/**
 * A persisted token captured from a {@link IdlTokenizer}.
 *
 * <p>For performance, {@code IdlTokenizer} does not create new tokens types for each encountered token. Instead, it
 * updates the current state of the tokenizer and allows the caller to inspect the tokenizer for information about each
 * token. Because smithy-syntax needs to create a token-tree rather than go directly to an AST, it requires arbitrary
 * lookahead of tokens, which means it needs to persist tokens in memory, using this {@code CapturedToken}.
 */
public final class CapturedToken {

    private final IdlToken token;
    private final int position;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final CharSequence lexeme;
    private final String stringContents;
    private final String errorMessage;
    private final Number numberValue;

    private CapturedToken(
            IdlToken token,
            int position,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn,
            CharSequence lexeme,
            String stringContents,
            Number numberValue,
            String errorMessage
    ) {
        this.token = token;
        this.position = position;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.lexeme = lexeme;
        this.stringContents = stringContents;
        this.numberValue = numberValue;
        this.errorMessage = errorMessage;
    }

    /**
     * Persist the current token of an {@link IdlTokenizer}.
     *
     * @param tokenizer Tokenizer to capture.
     * @return Returns the persisted token.
     */
    public static CapturedToken from(IdlTokenizer tokenizer) {
        return from(tokenizer, CharSequence::toString);
    }

    /**
     * Persist the current token of an {@link IdlTokenizer}.
     *
     * @param tokenizer   Tokenizer to capture.
     * @param stringTable String table that caches previously created strings.
     * @return Returns the persisted token.
     */
    public static CapturedToken from(IdlTokenizer tokenizer, Function<CharSequence, String> stringTable) {
        IdlToken token = tokenizer.getCurrentToken();
        CharSequence lexeme = tokenizer.getCurrentTokenLexeme();
        String stringContents = token == IdlToken.STRING || token == IdlToken.TEXT_BLOCK || token == IdlToken.IDENTIFIER
                                ? stringTable.apply(tokenizer.getCurrentTokenStringSlice())
                                : null;
        String errorMessage = token == IdlToken.ERROR ? tokenizer.getCurrentTokenError() : null;
        Number numberValue = token == IdlToken.NUMBER ? tokenizer.getCurrentTokenNumberValue() : null;
        return new CapturedToken(token,
                                 tokenizer.getCurrentTokenStart(),
                                 tokenizer.getCurrentTokenLine(),
                                 tokenizer.getCurrentTokenColumn(),
                                 tokenizer.getLine(),
                                 tokenizer.getColumn(),
                                 lexeme,
                                 stringContents,
                                 numberValue,
                                 errorMessage);
    }

    /**
     * Get the token IDL token of the captured token.
     *
     * @return Returns the underlying token type.
     */
    public IdlToken getIdlToken() {
        return token;
    }

    public int getPosition() {
        return position;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public int getSpan() {
        return lexeme.length();
    }

    /**
     * Get the raw lexeme of the current token.
     *
     * @return Returns the underlying lexeme of the token.
     */
    public CharSequence getLexeme() {
        return lexeme;
    }

    /**
     * Get the associated String contents of the token if it's a string, text block, or identifier.
     *
     * @return Returns the string contents of the lexeme, or null if not a string|text block|identifier.
     */
    public String getStringContents() {
        return stringContents;
    }

    /**
     * Gets the associated error message with the token if it's an error.
     *
     * @return Returns the error message or null if not an error.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get the computed {@link Number} of the current token if it's a number.
     *
     * @return Returns the computed Number or null if not a number.
     */
    public Number getNumberValue() {
        return numberValue;
    }
}
