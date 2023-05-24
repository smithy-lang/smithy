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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class TokenTreeNode implements TokenTree {

    private final TreeType treeType;
    private final List<TokenTree> children = new ArrayList<>();
    private transient List<CapturedToken> tokenCache;

    TokenTreeNode(TreeType treeType) {
        this.treeType = treeType;
    }

    @Override
    public final TreeType getType() {
        return treeType;
    }

    @Override
    public final List<CapturedToken> getTokens() {
        List<CapturedToken> captures = tokenCache;
        if (captures == null) {
            captures = new ArrayList<>();
            for (TokenTree tree : children) {
                captures.addAll(tree.getTokens());
            }
            tokenCache = captures;
        }
        return Collections.unmodifiableList(captures);
    }

    @Override
    public final List<TokenTree> getChildren() {
        return children;
    }

    @Override
    public final void appendChild(TokenTree tree) {
        tokenCache = null;
        children.add(tree);
    }

    @Override
    public boolean removeChild(TokenTree tree) {
        tokenCache = null;
        return children.removeIf(c -> c == tree);
    }

    @Override
    public final String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getType())
                .append(" (").append(getStartLine()).append(", ").append(getStartColumn())
                .append(") - (")
                .append(getEndLine()).append(", ").append(getEndColumn())
                .append(") {")
                .append('\n');
        if (getError() != null) {
            result.append("    ").append(getError()).append("\n    ---\n");
        }
        for (TokenTree child : children) {
            result.append("    ").append(child.toString().replace("\n", "\n    ")).append('\n');
        }
        result.append('}');
        return result.toString();
    }

    @Override
    public final int getStartPosition() {
        List<CapturedToken> tokens = getTokens();
        return tokens.isEmpty() ? 0 : tokens.get(0).getPosition();
    }

    @Override
    public final int getStartLine() {
        List<CapturedToken> tokens = getTokens();
        return tokens.isEmpty() ? 0 : tokens.get(0).getStartLine();
    }

    @Override
    public final int getStartColumn() {
        List<CapturedToken> tokens = getTokens();
        return tokens.isEmpty() ? 0 : tokens.get(0).getStartColumn();
    }

    @Override
    public final int getEndLine() {
        return children.isEmpty() ? getStartLine() : getLastToken().getEndLine();
    }

    @Override
    public final int getEndColumn() {
        return children.isEmpty() ? getStartColumn() : getLastToken().getEndColumn();
    }

    private CapturedToken getLastToken() {
        List<CapturedToken> capturedTokens = getTokens();
        return capturedTokens.get(capturedTokens.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            TokenTreeNode other = (TokenTreeNode) o;
            return treeType == other.treeType && children.equals(other.children);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(treeType, children);
    }
}
