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

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A persisted token captured from an {@link IdlTokenizer}.
 *
 * <p>For performance, {@code IdlTokenizer} does not create new tokens types for each encountered token. Instead, it
 * updates the current state of the tokenizer and allows the caller to inspect the tokenizer for information about each
 * token. Because smithy-syntax needs to create a token-tree rather than go directly to an AST, it requires arbitrary
 * lookahead of tokens, requiring it to persist tokens in memory.
 */
public final class CapturedToken implements FromSourceLocation, ToSmithyBuilder<CapturedToken> {

    private final IdlToken token;
    private final String filename;
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
            String filename,
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
        this.token = Objects.requireNonNull(token, "Missing required token");
        this.lexeme = Objects.requireNonNull(lexeme, "Missing required lexeme");
        this.filename = filename == null ? SourceLocation.none().getFilename() : filename;
        this.position = position;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;

        if (stringContents == null
                && (token == IdlToken.IDENTIFIER || token == IdlToken.STRING || token == IdlToken.TEXT_BLOCK)) {
            this.stringContents = lexeme.toString();
        } else {
            this.stringContents = stringContents;
        }

        if (errorMessage == null && token == IdlToken.ERROR) {
            this.errorMessage = "";
        } else {
            this.errorMessage = errorMessage;
        }

        if (numberValue == null && token == IdlToken.NUMBER) {
            this.numberValue = new BigDecimal(lexeme.toString());
        } else {
            this.numberValue = numberValue;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements SmithyBuilder<CapturedToken> {
        private IdlToken token;
        private String filename;
        private int position;
        private int startLine;
        private int startColumn;
        private int endLine;
        private int endColumn;
        private CharSequence lexeme;
        private String stringContents;
        private String errorMessage;
        private Number numberValue;

        private Builder() {}

        @Override
        public CapturedToken build() {
            return new CapturedToken(
                token,
                filename,
                position,
                startLine,
                startColumn,
                endLine,
                endColumn,
                lexeme,
                stringContents,
                numberValue,
                errorMessage
            );
        }

        public Builder token(IdlToken token) {
            this.token = token;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder position(int position) {
            this.position = position;
            return this;
        }

        public Builder startLine(int startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder startColumn(int startColumn) {
            this.startColumn = startColumn;
            return this;
        }

        public Builder endLine(int endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder endColumn(int endColumn) {
            this.endColumn = endColumn;
            return this;
        }

        public Builder lexeme(CharSequence lexeme) {
            this.lexeme = lexeme;
            return this;
        }

        public Builder stringContents(String stringContents) {
            this.stringContents = stringContents;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder numberValue(Number numberValue) {
            this.numberValue = numberValue;
            return this;
        }
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
        IdlToken tok = tokenizer.getCurrentToken();
        return builder()
                .token(tok)
                .filename(tokenizer.getSourceFilename())
                .position(tokenizer.getCurrentTokenStart())
                .startLine(tokenizer.getCurrentTokenLine())
                .startColumn(tokenizer.getCurrentTokenColumn())
                .endLine(tokenizer.getLine())
                .endColumn(tokenizer.getColumn())
                .lexeme(tokenizer.getCurrentTokenLexeme())
                .stringContents(tok == IdlToken.STRING || tok == IdlToken.TEXT_BLOCK || tok == IdlToken.IDENTIFIER
                                ? stringTable.apply(tokenizer.getCurrentTokenStringSlice())
                                : null)
                .numberValue(tok == IdlToken.NUMBER ? tokenizer.getCurrentTokenNumberValue() : null)
                .errorMessage(tok == IdlToken.ERROR ? tokenizer.getCurrentTokenError() : null)
                .build();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .token(token)
                .filename(filename)
                .position(position)
                .startLine(startLine)
                .startColumn(startColumn)
                .endLine(endLine)
                .endColumn(endColumn)
                .lexeme(lexeme)
                .errorMessage(errorMessage)
                .numberValue(numberValue)
                .stringContents(stringContents);
    }

    /**
     * Get the token IDL token of the captured token.
     *
     * @return Returns the underlying token type.
     */
    public IdlToken getIdlToken() {
        return token;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return new SourceLocation(getFilename(), getStartLine(), getStartColumn());
    }

    public String getFilename() {
        return filename;
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
