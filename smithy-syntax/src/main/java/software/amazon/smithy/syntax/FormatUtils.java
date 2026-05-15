/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

/**
 * Shared tree-navigation helpers used by the formatter pipeline passes.
 */
final class FormatUtils {

    private FormatUtils() {}

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
