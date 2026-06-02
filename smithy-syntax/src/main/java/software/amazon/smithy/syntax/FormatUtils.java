/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.List;
import software.amazon.smithy.utils.Pair;

/**
 * Shared tree-navigation helpers used by the formatter pipeline passes.
 */
final class FormatUtils {

    private FormatUtils() {}

    /**
     * Collect non-inline comments from a BR node into the given result list.
     *
     * <p>The parser's BR production greedily consumes all trailing whitespace including comments.
     * Comments on a different line than the BR's start are not inline trailing comments of the BR's
     * owning construct. They belong to whatever follows and are collected here so callers can
     * relocate or remove them. Comments may be direct children of the BR or nested inside a WS child
     * of the BR. Each entry pairs the comment's immediate parent (left) with the comment itself
     * (right) so the caller can detach it from the recorded parent directly instead of re-searching
     * the BR subtree.
     *
     * @param brCursor the BR cursor to inspect.
     * @param brStartLine the BR's start line, used to distinguish inline from following comments.
     * @param result the list to collect (parent, comment) pairs into.
     */
    static void collectNonInlineComments(
            TreeCursor brCursor,
            int brStartLine,
            List<Pair<TokenTree, TokenTree>> result
    ) {
        TokenTree brTree = brCursor.getTree();
        for (TokenTree child : brTree.getChildren()) {
            if (child.getType() == TreeType.COMMENT && child.getStartLine() != brStartLine) {
                result.add(Pair.of(brTree, child));
            } else if (child.getType() == TreeType.WS) {
                // Comments inside a WS child of BR.
                for (TokenTree wsChild : child.getChildren()) {
                    if (wsChild.getType() == TreeType.COMMENT && wsChild.getStartLine() != brStartLine) {
                        result.add(Pair.of(child, wsChild));
                    }
                }
            }
        }
    }

    /**
     * Detach each collected comment from its recorded parent.
     *
     * <p>Operates on the (parent, comment) pairs produced by {@link #collectNonInlineComments}.
     * Because the parent reference is captured at collection time, removal cannot fail to locate the
     * node and the same comment is never attached to two parents. A failed removal indicates the
     * tree shape changed underneath the caller and is reported as an {@link IllegalStateException}.
     *
     * @param comments the (parent, comment) pairs to detach.
     */
    static void detachCollectedComments(List<Pair<TokenTree, TokenTree>> comments) {
        for (Pair<TokenTree, TokenTree> entry : comments) {
            if (!entry.getLeft().removeChild(entry.getRight())) {
                throw new IllegalStateException(
                        "Could not detach comment from its recorded parent: " + entry.getRight().concatTokens());
            }
        }
    }

    /**
     * Get the end line of a member's visible value content for same-line comment detection.
     *
     * <p>For members with a VALUE_ASSIGNMENT, the BR production's NEWLINE token can inflate
     * {@code getEndLine()} past the actual value line (because the NEWLINE spans to the next
     * line). This method returns the NODE_VALUE's end line instead, giving the line where the
     * member's visible content actually ends. For members without a VALUE_ASSIGNMENT, this is
     * just the cursor's own end line.
     *
     * @param cursor the cursor to inspect, or null.
     * @return the value end line, or -1 if cursor is null.
     */
    static int valueEndLine(TreeCursor cursor) {
        if (cursor == null) {
            return -1;
        }
        TreeCursor va = cursor.getFirstChild(TreeType.VALUE_ASSIGNMENT);
        if (va != null) {
            TreeCursor nodeValue = va.getFirstChild(TreeType.NODE_VALUE);
            if (nodeValue != null) {
                return nodeValue.getTree().getEndLine();
            }
        }
        return cursor.getTree().getEndLine();
    }

    /**
     * Check whether a member cursor is the last member in its container before the closing brace.
     *
     * <p>Walks the cursor's subsequent siblings, skipping WS nodes. Returns true when the next
     * non-WS sibling is the container's closing brace token (TOKEN type) or there is no next
     * sibling at all.
     *
     * @param cursor the member cursor to inspect.
     * @return true if no further member follows in the container.
     */
    static boolean isLastMemberBeforeBrace(TreeCursor cursor) {
        TreeCursor next = cursor.getNextSibling();
        while (next != null && next.getTree().getType() == TreeType.WS) {
            next = next.getNextSibling();
        }
        return next == null || next.getTree().getType() == TreeType.TOKEN;
    }
}
