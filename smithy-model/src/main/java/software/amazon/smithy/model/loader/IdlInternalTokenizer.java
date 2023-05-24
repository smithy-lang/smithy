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
    private final IdlWhitespaceParser whitespaceParser;

    IdlInternalTokenizer(String filename, CharSequence model) {
        this(filename, model, event -> { });
    }

    IdlInternalTokenizer(String filename, CharSequence model, Consumer<ValidationEvent> validationEventListener) {
        super(filename, model);
        this.model = model;
        this.validationEventListener = validationEventListener;
        this.whitespaceParser = new IdlWhitespaceParser(this);
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

            docCommentLines.add(getModel(docStart, getCurrentTokenEnd()));
        }

        return token;
    }

    void skipSpaces() {
        whitespaceParser.skipSpaces();
    }

    void skipOptionalComma() {
        if (getCurrentToken() == IdlToken.COMMA) {
            next();
        }
    }

    void skipWs() {
        whitespaceParser.skipWs();
    }

    void skipWsAndDocs() {
        whitespaceParser.skipWsAndDocs();
    }

    void expectAndSkipSpaces() {
        expect(IdlToken.SPACE);
        whitespaceParser.skipSpaces();
    }

    void expectAndSkipWhitespace() {
        whitespaceParser.expectAndSkipWhitespace();
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
        whitespaceParser.skipSpaces();
        clearDocCommentLinesForBr();
        whitespaceParser.expectAndSkipBr();
    }

    private void clearDocCommentLinesForBr() {
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
                result.append(docCommentLines.removeFirst());
            }
            if (result.charAt(result.length()  - 1) == '\n') {
                result.setLength(result.length() - 1);
            }
            if (result.charAt(result.length()  - 1) == '\r') {
                result.setLength(result.length() - 1);
            }
            return result.toString();
        }
    }
}
