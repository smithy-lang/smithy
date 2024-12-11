/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.nio.CharBuffer;
import java.util.Iterator;
import software.amazon.smithy.model.SourceLocation;

/**
 * Iterates over a Smithy IDL model as a series of tokens.
 */
public interface IdlTokenizer extends Iterator<IdlToken> {

    /**
     * Create a tokenizer for the given model.
     *
     * @param model IDL model contents to parse.
     * @return      Returns the tokenizer.
     */
    static IdlTokenizer create(CharSequence model) {
        return create(SourceLocation.NONE.getFilename(), model);
    }

    /**
     * Create a tokenizer for the given filename and model.
     *
     * @param filename Filename being parsed.
     * @param model    IDL model contents to parse.
     * @return         Returns the tokenizer.
     */
    static IdlTokenizer create(String filename, CharSequence model) {
        return new DefaultTokenizer(filename, model);
    }

    /**
     * Get the filename of the content being tokenized.
     *
     * @return Returns the filename used in source locations.
     */
    String getSourceFilename();

    /**
     * Get the model being tokenized.
     *
     * @return Returns the model.
     */
    CharSequence getModel();

    /**
     * Get a borrowed slice of the model being tokenized.
     *
     * @param start Start position to get, inclusive.
     * @param end   End position to stop at, exclusive.
     * @return Returns the slice.
     */
    default CharSequence getModel(int start, int end) {
        return CharBuffer.wrap(getModel(), start, end);
    }

    /**
     * Get the current position of the tokenizer.
     *
     * @return Returns the absolute position.
     */
    int getPosition();

    /**
     * Get the current line number of the tokenizer, starting at 1.
     *
     * @return Get the current line number.
     */
    int getLine();

    /**
     * Get the current column number of the tokenizer, starting at 1.
     *
     * @return Get the current column number.
     */
    int getColumn();

    /**
     * Get the current {@link IdlToken}.
     *
     * @return Return the current token type.
     */
    IdlToken getCurrentToken();

    /**
     * Get the line of the current token.
     *
     * @return Return the line of the current token.
     */
    int getCurrentTokenLine();

    /**
     * Get the column of the current token.
     *
     * @return Return the column of the current token.
     */
    int getCurrentTokenColumn();

    /**
     * Get the start position of the current token.
     *
     * @return Return the 0-based start position of the current token.
     */
    int getCurrentTokenStart();

    /**
     * Get the end position of the curren token.
     *
     * @return Return the 0-based end position of the current token.
     */
    int getCurrentTokenEnd();

    /**
     * Get the length of the current token.
     *
     * @return Return the current token span.
     */
    default int getCurrentTokenSpan() {
        return getCurrentTokenEnd() - getCurrentTokenStart();
    }

    /**
     * Get the source location of the current token.
     *
     * @return Return the current token source location.
     */
    default SourceLocation getCurrentTokenLocation() {
        return new SourceLocation(getSourceFilename(), getCurrentTokenLine(), getCurrentTokenColumn());
    }

    /**
     * Get the lexeme of the current token.
     *
     * @return Returns the lexeme of the current token.
     */
    default CharSequence getCurrentTokenLexeme() {
        return getModel(getCurrentTokenStart(), getCurrentTokenEnd());
    }

    /**
     * If the current token is a string or text block, get the parsed content as a CharSequence.
     * If the current token is an identifier, the lexeme of the identifier is returned.
     *
     * @return Returns the parsed string content associated with the current token.
     * @throws ModelSyntaxException if the current token is not a string, text block, or identifier.
     */
    CharSequence getCurrentTokenStringSlice();

    /**
     * If the current token is a number, get the associated parsed number.
     *
     * @return Returns the parsed number associated with the current token.
     * @throws ModelSyntaxException if the current token is not a number.
     */
    Number getCurrentTokenNumberValue();

    /**
     * If the current token is an error, get the error message associated with the token.
     *
     * @return Returns the associated error message.
     * @throws ModelSyntaxException if the current token is not an error.
     */
    String getCurrentTokenError();

    /**
     * Assert that the current token is {@code token}.
     *
     * <p>The tokenizer is not advanced after validating the current token.</p>
     *
     * @param token Token to expect.
     * @throws ModelSyntaxException if the current token is unexpected.
     */
    default void expect(IdlToken token) {
        if (getCurrentToken() != token) {
            throw LoaderUtils.idlSyntaxError(LoaderUtils.idlExpectMessage(this, token), getCurrentTokenLocation());
        }
    }

    /**
     * Assert that the current token is one of {@code tokens}.
     *
     * <p>The tokenizer is not advanced after validating the current token.</p>
     *
     * @param tokens Assert that the current token is one of these tokens.
     * @return Returns the current token.
     * @throws ModelSyntaxException if the current token is unexpected.
     */
    default IdlToken expect(IdlToken... tokens) {
        IdlToken currentTokenType = getCurrentToken();

        for (IdlToken token : tokens) {
            if (currentTokenType == token) {
                return token;
            }
        }

        throw LoaderUtils.idlSyntaxError(LoaderUtils.idlExpectMessage(this, tokens), getCurrentTokenLocation());
    }

    /**
     * Test if the current token lexeme is equal to the give {@code chars}.
     *
     * @param chars Characters to compare the current lexeme against.
     * @return Returns true if the current lexeme is equal to {@code chars}.
     */
    default boolean isCurrentLexeme(CharSequence chars) {
        CharSequence lexeme = getCurrentTokenLexeme();
        int testLength = chars.length();
        if (lexeme.length() != testLength) {
            return false;
        }
        for (int i = 0; i < testLength; i++) {
            if (lexeme.charAt(i) != chars.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
