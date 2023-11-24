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
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.smithy.model.SourceLocation;

class TokenTreeNode implements TokenTree {

    private final TreeType treeType;
    private final List<TokenTree> children = new ArrayList<>();

    TokenTreeNode(TreeType treeType) {
        this.treeType = treeType;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return getChildren().isEmpty() ? SourceLocation.NONE : getChildren().get(0).getSourceLocation();
    }

    @Override
    public final TreeType getType() {
        return treeType;
    }

    @Override
    public final Stream<CapturedToken> tokens() {
        return children.stream().flatMap(TokenTree::tokens);
    }

    @Override
    public String concatTokens() {
        StringBuilder result = new StringBuilder();
        tokens().forEach(token -> result.append(token.getLexeme()));
        return result.toString();
    }

    @Override
    public final List<TokenTree> getChildren() {
        return children;
    }

    @Override
    public boolean isEmpty() {
        return getChildren().isEmpty();
    }

    @Override
    public final void appendChild(TokenTree tree) {
        children.add(tree);
    }

    @Override
    public boolean removeChild(TokenTree tree) {
        return children.removeIf(c -> c == tree);
    }

    @Override
    public boolean replaceChild(TokenTree find, TokenTree replacement) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == find) {
                children.set(i, replacement);
                return true;
            }
        }
        return false;
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
        return getChildren().isEmpty() ? 0 : getChildren().get(0).getStartPosition();
    }

    @Override
    public final int getStartLine() {
        return getChildren().isEmpty() ? 0 : getChildren().get(0).getStartLine();
    }

    @Override
    public final int getStartColumn() {
        return getChildren().isEmpty() ? 0 : getChildren().get(0).getStartColumn();
    }

    @Override
    public final int getEndLine() {
        return children.isEmpty() ? getStartLine() : children.get(children.size() - 1).getEndLine();
    }

    @Override
    public final int getEndColumn() {
        return children.isEmpty() ? getStartColumn() : children.get(children.size() - 1).getEndColumn();
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
