/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A specialized Tokenizer that adds skipping and documentation buffering.
 */
final class IdlInternalTokenizer extends DefaultTokenizer {

    private final CharSequence model;
    private final Deque<CharSequence> docCommentLines = new ArrayDeque<>();
    private final Consumer<ValidationEvent> validationEventListener;

    IdlInternalTokenizer(String filename, CharSequence model) {
        this(filename, model, event -> {});
    }

    IdlInternalTokenizer(String filename, CharSequence model, Consumer<ValidationEvent> validationEventListener) {
        super(filename, model);
        this.model = model;
        this.validationEventListener = validationEventListener;
    }

    @Override
    public IdlToken next() {
        IdlToken token = super.next();

        // Buffer documentation comments as they're encountered.
        if (token == IdlToken.DOC_COMMENT) {
            int start = getCurrentTokenStart();
            int span = getCurrentTokenSpan();
            int docStart;

            if (span <= 3) {
                // An empty doc comment.
                docStart = start;
            } else if (model.charAt(start + 3) == ' ') {
                // Skip single space.
                docStart = start + 4;
            } else {
                docStart = start + 3;
            }

            // Strip the \n and \r\n from the end.
            int end = getCurrentTokenEnd();
            if (model.charAt(end - 1) == '\n') {
                end--;
                if (end >= 0 && model.charAt(end - 1) == '\r') {
                    end--;
                }
            }

            docCommentLines.add(getModel(docStart, end));
        }

        return token;
    }

    void skipSpaces() {
        while (getCurrentToken() == IdlToken.SPACE) {
            next();
        }
    }

    void skipOptionalComma() {
        if (getCurrentToken() == IdlToken.COMMA) {
            next();
        }
    }

    void skipWs() {
        while (getCurrentToken().isWhitespace()) {
            next();
        }
    }

    void skipWsAndDocs() {
        IdlToken currentTokenType = getCurrentToken();
        while (currentTokenType.isWhitespace() || currentTokenType == IdlToken.DOC_COMMENT) {
            next();
            currentTokenType = getCurrentToken();
        }
    }

    void expectAndSkipSpaces() {
        expect(IdlToken.SPACE);
        skipSpaces();
    }

    void expectAndSkipWhitespace() {
        if (!getCurrentToken().isWhitespace()) {
            throw LoaderUtils.idlSyntaxError("Expected one or more whitespace characters, but found "
                    + getCurrentToken().getDebug(getCurrentTokenLexeme()),
                    getCurrentTokenLocation());
        }
        skipWsAndDocs();
    }

    /**
     * Expects that the current token is zero or more spaces/commas followed by a newline, comment, documentation
     * comment, or EOF.
     *
     * <p>If a documentation comment is detected, the current token remains the documentation comment. If an EOF is
     * detected, the current token remains the EOF token. If a comment, newline, or whitespace are detected, they are
     * all skipped, leaving the current token the next token after encountering the matched token. Other kinds of
     * tokens will raise an exception.
     *
     * <p>This method mimics the {@code br} production from the Smithy grammar. When this method is called, any
     * previously parsed and buffered documentation comments are discarded.
     *
     * @throws ModelSyntaxException if the current token is not a newline or followed by a newline.
     */
    void expectAndSkipBr() {
        skipSpaces();
        clearDocCommentLinesForBr();

        // The following tokens are allowed tokens that contain newlines.
        switch (getCurrentToken()) {
            case NEWLINE:
            case COMMENT:
            case DOC_COMMENT:
                next();
                skipWs();
                break;
            case EOF:
                break;
            default:
                throw LoaderUtils.idlSyntaxError(
                        "Expected a line break, but found "
                                + getCurrentToken().getDebug(getCurrentTokenLexeme()),
                        getCurrentTokenLocation());
        }
    }

    void clearDocCommentLinesForBr() {
        if (!docCommentLines.isEmpty()) {
            validationEventListener.accept(LoaderUtils.emitBadDocComment(getCurrentTokenLocation(),
                    removePendingDocCommentLines()));
        }
    }

    /**
     * Removes any buffered documentation comment lines, and returns a concatenated string value.
     *
     * @return Returns the combined documentation comment string for the given lines. Returns null if no lines.
     */
    String removePendingDocCommentLines() {
        if (docCommentLines.isEmpty()) {
            return null;
        } else {
            StringBuilder result = new StringBuilder();
            while (!docCommentLines.isEmpty()) {
                result.append(docCommentLines.removeFirst()).append("\n");
            }
            // Strip ending \n.
            result.setLength(result.length() - 1);
            return result.toString();
        }
    }
}
