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

/**
 * Parses Shape ID lexemes from a {@link IdlInternalTokenizer}.
 */
final class IdlShapeIdParser {

    private IdlShapeIdParser() { }

    /**
     * Expects that the current token and subsequent tokens make up a Smithy namespace production (e.g., "foo.bar").
     *
     * <p>After consuming the namespace, the tokenizer is moved to the next token.
     *
     * @param tokenizer Tokenizer to consume and advance.
     * @return Returns the sequence of characters that make up a namespace.
     * @throws ModelSyntaxException if the tokenizer is unable to parse a valid namespace.
     */
    static CharSequence expectAndSkipShapeIdNamespace(IdlInternalTokenizer tokenizer) {
        int startPosition = tokenizer.getCurrentTokenStart();
        int endOffset = skipShapeIdNamespace(tokenizer);
        return sliceFrom(tokenizer, startPosition, endOffset);
    }

    /**
     * Expects that the tokenizer is on an absolute shape ID, skips over it, and returns a borrowed slice of the
     * shape ID.
     *
     * @param tokenizer Tokenizer to consume and advance.
     * @return Returns the borrowed shape ID.
     */
    static CharSequence expectAndSkipAbsoluteShapeId(IdlInternalTokenizer tokenizer) {
        int startPosition = tokenizer.getCurrentTokenStart();
        int endOffset = skipAbsoluteShapeId(tokenizer);
        return sliceFrom(tokenizer, startPosition, endOffset);
    }

    /**
     * Expects that the tokenizer is on a relative or absolute shape ID, skips over it, and returns a borrowed slice
     * of the shape ID.
     *
     * @param tokenizer Tokenizer to consume and advance.
     * @return Returns the borrowed shape ID.
     */
    static CharSequence expectAndSkipShapeId(IdlInternalTokenizer tokenizer) {
        int startPosition = tokenizer.getCurrentTokenStart();
        int offset = skipShapeIdNamespace(tokenizer);
        // Keep parsing if we find a $ or a #.
        if (tokenizer.getCurrentToken() != IdlToken.DOLLAR && tokenizer.getCurrentToken() != IdlToken.POUND) {
            return sliceFrom(tokenizer, startPosition, offset);
        }
        tokenizer.next();
        offset = skipRelativeRootShapeId(tokenizer);
        return sliceFrom(tokenizer, startPosition, offset);
    }

    private static CharSequence sliceFrom(IdlInternalTokenizer tokenizer, int startPosition, int endOffset) {
        return tokenizer.getModel(startPosition, tokenizer.getPosition() - endOffset);
    }

    private static int skipShapeIdNamespace(IdlInternalTokenizer tokenizer) {
        tokenizer.expect(IdlToken.IDENTIFIER);
        tokenizer.next();
        // Keep track of how many characters from the end to omit (don't include "#" or whatever is next in the slice).
        int endOffset = tokenizer.getCurrentTokenSpan();
        while (tokenizer.getCurrentToken() == IdlToken.DOT) {
            tokenizer.next();
            tokenizer.expect(IdlToken.IDENTIFIER);
            tokenizer.next();
            endOffset = tokenizer.getCurrentTokenSpan();
        }
        return endOffset;
    }

    private static int skipAbsoluteShapeId(IdlInternalTokenizer tokenizer) {
        skipShapeIdNamespace(tokenizer);
        tokenizer.expect(IdlToken.POUND);
        tokenizer.next();
        return skipRelativeRootShapeId(tokenizer);
    }

    private static int skipRelativeRootShapeId(IdlInternalTokenizer tokenizer) {
        tokenizer.expect(IdlToken.IDENTIFIER);
        tokenizer.next();
        // Don't include whatever character comes next in the slice.
        int endOffset = tokenizer.getCurrentTokenSpan();
        if (tokenizer.getCurrentToken() == IdlToken.DOLLAR) {
            tokenizer.next();
            tokenizer.expect(IdlToken.IDENTIFIER);
            tokenizer.next();
            // It had a member, so update the offset to not include.
            endOffset = tokenizer.getCurrentTokenSpan();
        }
        return endOffset;
    }
}
