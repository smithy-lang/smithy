/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
                .append(" (")
                .append(getStartLine())
                .append(", ")
                .append(getStartColumn())
                .append(") - (")
                .append(getEndLine())
                .append(", ")
                .append(getEndColumn())
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
        // Start line of 0 indicates an empty child, so ignore it.
        for (TokenTree child : getChildren()) {
            int startLine = child.getStartLine();
            if (startLine > 0) {
                return startLine;
            }
        }
        return 0;
    }

    @Override
    public final int getStartColumn() {
        // Start column of 0 indicates an empty child, so ignore it.
        for (TokenTree child : getChildren()) {
            int startColumn = child.getStartColumn();
            if (startColumn > 0) {
                return startColumn;
            }
        }
        return 0;
    }

    @Override
    public final int getEndLine() {
        // End line of 0 indicates an empty child, so ignore it.
        for (int i = children.size() - 1; i >= 0; i--) {
            int endLine = children.get(i).getEndLine();
            if (endLine > 0) {
                return endLine;
            }
        }
        return 0;
    }

    @Override
    public final int getEndColumn() {
        // End column of 0 indicates an empty child, so ignore it.
        for (int i = children.size() - 1; i >= 0; i--) {
            int endColumn = children.get(i).getEndColumn();
            if (endColumn > 0) {
                return endColumn;
            }
        }
        return 0;
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
