/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;

/**
 * Externally traverses a {@link TokenTree} to provide access to parents and siblings.
 *
 * @see TokenTree#zipper()
 */
public final class TreeCursor implements FromSourceLocation {

    private final TokenTree tree;
    private final TreeCursor parent;

    private TreeCursor(TokenTree tree, TreeCursor parent) {
        this.tree = tree;
        this.parent = parent;
    }

    /**
     * Create a TreeCursor from the given TokenTree, treating it as the root of the tree.
     *
     * @param tree Tree to create a cursor from.
     * @return Returns the created cursor.
     */
    public static TreeCursor of(TokenTree tree) {
        return new TreeCursor(tree, null);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return getTree().getSourceLocation();
    }

    /**
     * Get the wrapped {@link TokenTree}.
     *
     * @return Return the token tree.
     */
    public TokenTree getTree() {
        return tree;
    }

    /**
     * Get the parent cursor, or null if not present.
     *
     * @return Nullable parent cursor.
     */
    public TreeCursor getParent() {
        return parent;
    }

    /**
     * Get the root of the tree, returning itself if the tree has no parent.
     *
     * @return Non-nullable root tree.
     */
    public TreeCursor getRoot() {
        TreeCursor current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /**
     * Get a list of tree cursors that lead up to the current tree, starting from the root as the first element, and
     * including the current tree as the last element.
     *
     * @return Returns the path to the current tree from the root.
     */
    public List<TreeCursor> getPathToCursor() {
        List<TreeCursor> path = new ArrayList<>();
        TreeCursor current = this;
        do {
            path.add(current);
            current = current.parent;
        } while (current != null);
        Collections.reverse(path);
        return path;
    }

    /**
     * Get the previous sibling of this tree, if present.
     *
     * @return Return the nullable previous sibling.
     */
    public TreeCursor getPreviousSibling() {
        return getSibling(-1);
    }

    /**
     * Get the next sibling of this tree, if present.
     *
     * @return Return the nullable next sibling.
     */
    public TreeCursor getNextSibling() {
        return getSibling(1);
    }

    private TreeCursor getSibling(int offset) {
        if (parent == null) {
            return null;
        }

        List<TokenTree> siblings = parent.getTree().getChildren();
        int myPosition = siblings.indexOf(this.tree);

        if (myPosition == -1) {
            return null;
        }

        int target = myPosition + offset;
        if (target < 0 || target > siblings.size() - 1) {
            return null;
        }

        return new TreeCursor(siblings.get(target), parent);
    }

    /**
     * Get all children of the tree as a list of cursors.
     *
     * @return Return the cursors to each child.
     */
    public List<TreeCursor> getChildren() {
        List<TreeCursor> result = new ArrayList<>(getTree().getChildren().size());
        for (TokenTree child : tree.getChildren()) {
            result.add(new TreeCursor(child, this));
        }
        return result;
    }

    /**
     * Get a stream of child cursors.
     *
     * @return Returns children as a stream.
     */
    Stream<TreeCursor> children() {
        return getTree().getChildren().stream().map(child -> new TreeCursor(child, this));
    }

    /**
     * Get direct children from the current tree of a specific type.
     *
     * @param types Types of children to get.
     * @return Returns the collected children, or an empty list.
     */
    public List<TreeCursor> getChildrenByType(TreeType... types) {
        List<TreeCursor> result = new ArrayList<>();
        for (int i = 0; i < tree.getChildren().size(); i++) {
            TokenTree child = tree.getChildren().get(i);
            for (TreeType type : types) {
                if (child.getType() == type) {
                    result.add(new TreeCursor(child, this));
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Get the first child of the wrapped tree.
     *
     * @return Return the first child, or null if the tree has no children.
     */
    public TreeCursor getFirstChild() {
        if (tree.getChildren().isEmpty()) {
            return null;
        } else {
            return new TreeCursor(tree.getChildren().get(0), this);
        }
    }

    /**
     * Get the first child of the wrapped tree with the given type.
     *
     * @param type Child type to get.
     * @return Return the first child, or null if a matching child is not found.
     */
    public TreeCursor getFirstChild(TreeType type) {
        for (TokenTree child : getTree().getChildren()) {
            if (child.getType() == type) {
                return new TreeCursor(child, this);
            }
        }
        return null;
    }

    /**
     * Get the last child of the wrapped tree.
     *
     * @return Return the last child, or null if the tree has no children.
     */
    public TreeCursor getLastChild() {
        if (tree.getChildren().isEmpty()) {
            return null;
        } else {
            return new TreeCursor(tree.getChildren().get(tree.getChildren().size() - 1), this);
        }
    }

    /**
     * Get the last child of the wrapped tree with the given type.
     *
     * @param type Child type to get.
     * @return Return the last child, or null if a matching child is not found.
     */
    public TreeCursor getLastChild(TreeType type) {
        List<TokenTree> children = tree.getChildren();
        ListIterator<TokenTree> iterator = children.listIterator(children.size());
        while (iterator.hasPrevious()) {
            TokenTree child = iterator.previous();
            if (child.getType() == type) {
                return new TreeCursor(child, this);
            }
        }
        return null;
    }

    /**
     * Recursively find every node in the tree that has the given {@code TreeType}.
     *
     * @param types Types of children to return.
     * @return Returns the matching tree cursors.
     */
    public List<TreeCursor> findChildrenByType(TreeType... types) {
        return findChildren(c -> {
            TreeType treeType = c.getTree().getType();
            for (TreeType type : types) {
                if (treeType == type) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Recursively finds every node in the tree that matches the given predicate.
     *
     * @param predicate Predicate to test each recursive child against.
     * @return Returns the matching tree cursors, or an empty list if none are found.
     */
    private List<TreeCursor> findChildren(Predicate<TreeCursor> predicate) {
        List<TreeCursor> result = new ArrayList<>();
        recursiveFindChildren(this, result, predicate);
        return result;
    }

    private void recursiveFindChildren(TreeCursor cursor, List<TreeCursor> cursors, Predicate<TreeCursor> predicate) {
        for (TreeCursor tree : cursor.getChildren()) {
            if (predicate.test(tree)) {
                cursors.add(tree);
            }
            recursiveFindChildren(tree, cursors, predicate);
        }
    }

    /**
     * Find the innermost tree that contains the given coordinates.
     *
     * @param line   Line to find.
     * @param column Column to find.
     * @return Returns the innermost tree that contains the coordinates.
     */
    public TreeCursor findAt(int line, int column) {
        TreeCursor current = this;

        outer: while (true) {
            for (TreeCursor child : current.getChildren()) {
                TokenTree childTree = child.getTree();
                int startLine = childTree.getStartLine();
                int endLine = childTree.getEndLine();
                int startColumn = childTree.getStartColumn();
                int endColumn = childTree.getEndColumn();
                boolean isMatch = false;
                if (line == startLine && line == endLine) {
                    // Column span checks are exclusive to not match the ends of tokens.
                    isMatch = column >= startColumn && column < endColumn;
                } else if (line == startLine && column >= startColumn) {
                    isMatch = true;
                } else if (line == endLine && column < endColumn) {
                    isMatch = true;
                } else if (line > startLine && line < endLine) {
                    isMatch = true;
                }
                if (isMatch) {
                    current = child;
                    continue outer;
                }
            }
            return current;
        }
    }

    /**
     * @return All siblings after this cursor, in order.
     */
    public List<TreeCursor> remainingSiblings() {
        List<TreeCursor> remaining = new ArrayList<>();
        TreeCursor nextSibling = getNextSibling();
        while (nextSibling != null) {
            remaining.add(nextSibling);
            nextSibling = nextSibling.getNextSibling();
        }
        return remaining;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TreeCursor cursor = (TreeCursor) o;
        return getTree().equals(cursor.getTree()) && Objects.equals(getParent(), cursor.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTree(), getParent());
    }
}
