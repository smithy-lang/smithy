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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.smithy.model.SourceLocation;

final class TokenTreeLeaf implements TokenTree {

    private final CapturedToken token;

    TokenTreeLeaf(CapturedToken token) {
        this.token = token;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return token.getSourceLocation();
    }

    @Override
    public Stream<CapturedToken> tokens() {
        return Stream.of(token);
    }

    @Override
    public String concatTokens() {
        return token.getLexeme().toString();
    }

    @Override
    public TreeType getType() {
        return TreeType.TOKEN;
    }

    @Override
    public List<TokenTree> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void appendChild(TokenTree tree) {
        throw new UnsupportedOperationException("Cannot append a child to a leaf node");
    }

    @Override
    public boolean removeChild(TokenTree tree) {
        return false;
    }

    @Override
    public boolean replaceChild(TokenTree find, TokenTree replacement) {
        return false;
    }

    @Override
    public String toString() {
        if (token.getErrorMessage() != null) {
            return token.getIdlToken() + "(" + token.getErrorMessage() + ')';
        } else {
            return token.getIdlToken().getDebug(token.getLexeme());
        }
    }

    @Override
    public int getStartPosition() {
        return token.getPosition();
    }

    @Override
    public int getStartLine() {
        return token.getStartLine();
    }

    @Override
    public int getStartColumn() {
        return token.getStartColumn();
    }

    @Override
    public int getEndLine() {
        return token.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return token.getEndColumn();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            return this.token.equals(((TokenTreeLeaf) o).token);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
