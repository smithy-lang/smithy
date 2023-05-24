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
 * Provides common abstractions for parsing whitespace.
 */
@SmithyUnstableApi
public final class IdlWhitespaceParser {

    private final IdlTokenizer tokenizer;

    public IdlWhitespaceParser(IdlTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Skip spaces.
     */
    public void skipSpaces() {
        while (tokenizer.getCurrentToken() == IdlToken.SPACE) {
            tokenizer.next();
        }
    }

    /**
     * Skip until the current token is not {@link IdlToken#SPACE}, {@link IdlToken#COMMA}, {@link IdlToken#COMMENT},
     * or {@link IdlToken#NEWLINE}.
     */
    public void skipWs() {
        while (tokenizer.getCurrentToken().isWhitespace()) {
            tokenizer.next();
        }
    }

    /**
     * Skip until the current token is not {@link IdlToken#SPACE}, {@link IdlToken#COMMA}, {@link IdlToken#COMMENT},
     * or {@link IdlToken#NEWLINE}, or {@link IdlToken#DOC_COMMENT}.
     */
    public void skipWsAndDocs() {
        IdlToken currentTokenType = tokenizer.getCurrentToken();
        while (currentTokenType.isWhitespace() || currentTokenType == IdlToken.DOC_COMMENT) {
            tokenizer.next();
            currentTokenType = tokenizer.getCurrentToken();
        }
    }

    /**
     * Expect that one or more whitespace characters or documentation comments are found at the current token, and
     * skip over them.
     *
     * @throws ModelSyntaxException if the current token is not whitespace.
     */
    public void expectAndSkipWhitespace() {
        if (!tokenizer.getCurrentToken().isWhitespace()) {
            throw LoaderUtils.idlSyntaxError("Expected one or more whitespace characters, but found "
                                             + tokenizer.getCurrentToken().getDebug(tokenizer.getCurrentTokenLexeme()),
                                             tokenizer.getCurrentTokenLocation());
        }
        skipWsAndDocs();
    }

    /**
     * Expects that the current token is zero or more spaces/commas followed by a newline, comment, documentation
     * comment, or EOF.
     *
     * @throws ModelSyntaxException if the current token is not a newline or followed by a newline.
     */
    public void expectAndSkipBr() {
        // The following tokens are allowed tokens that contain newlines.
        switch (tokenizer.getCurrentToken()) {
            case NEWLINE:
            case COMMENT:
            case DOC_COMMENT:
                tokenizer.next();
                skipWs();
                break;
            case EOF:
                break;
            default:
                throw LoaderUtils.idlSyntaxError(
                        "Expected a line break, but found "
                        + tokenizer.getCurrentToken().getDebug(tokenizer.getCurrentTokenLexeme()),
                        tokenizer.getCurrentTokenLocation());
        }
    }
}
