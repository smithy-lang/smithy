/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import software.amazon.smithy.utils.Pair;

/**
 * Moves non-inline comments from VALUE_ASSIGNMENT BR nodes into sibling WS nodes in the enclosing member container.
 *
 * <p>The parser's BR production greedily consumes all trailing whitespace including comments. When a member has a
 * value assignment (e.g., {@code a: String = ""}), any comments on subsequent lines get captured inside the
 * VALUE_ASSIGNMENT's BR child, even when they semantically belong between members or before the closing brace.
 * This pass relocates those comments so that FormatVisitor can treat all inter-member comments uniformly as
 * WS-contained siblings rather than needing special handling for BR-trapped comments.
 *
 * <p>Same-line trailing comments (comments on the same line as the value) are left in the BR because the
 * FormatVisitor's BR visitor already handles them inline.
 */
final class RelocateMemberComments implements Function<TokenTree, TokenTree> {
    @Override
    public TokenTree apply(TokenTree tree) {
        TreeCursor root = tree.zipper();
        TreeCursor shapeSection = root.getFirstChild(TreeType.SHAPE_SECTION);
        if (shapeSection == null) {
            return tree;
        }

        TreeCursor shapeStatements = shapeSection.getFirstChild(TreeType.SHAPE_STATEMENTS);
        if (shapeStatements == null) {
            return tree;
        }

        // Process all member containers that can contain VALUE_ASSIGNMENT members. OPERATION_BODY
        // is intentionally excluded: its OPERATION_PROPERTY children parse to OPERATION_INPUT,
        // OPERATION_OUTPUT, or OPERATION_ERRORS, none of which produce VALUE_ASSIGNMENTs. Inline
        // operation inputs and outputs (e.g., `input := { ... }`) are reached recursively via
        // findChildrenByType when their nested SHAPE_MEMBERS container is visited.
        for (TreeCursor members : shapeStatements.findChildrenByType(
                TreeType.SHAPE_MEMBERS,
                TreeType.ENUM_SHAPE_MEMBERS)) {
            relocateInContainer(members);
        }

        return tree;
    }

    private void relocateInContainer(TreeCursor container) {
        // Determine the member type for this container.
        TreeType memberType;
        switch (container.getTree().getType()) {
            case SHAPE_MEMBERS:
                memberType = TreeType.SHAPE_MEMBER;
                break;
            case ENUM_SHAPE_MEMBERS:
                memberType = TreeType.ENUM_SHAPE_MEMBER;
                break;
            default:
                return;
        }

        for (TreeCursor member : container.getChildrenByType(memberType)) {
            TreeCursor va = member.getFirstChild(TreeType.VALUE_ASSIGNMENT);
            if (va == null) {
                continue;
            }
            TreeCursor br = va.getFirstChild(TreeType.BR);
            if (br == null) {
                continue;
            }

            // Collect non-inline comments from the BR node (comments on a different line than the BR start).
            // Each entry pairs the comment's immediate parent (left) with the comment itself (right) so the
            // detach step can remove from the recorded parent directly instead of re-searching the BR subtree.
            List<Pair<TokenTree, TokenTree>> toRelocate = new ArrayList<>();
            int brStartLine = br.getTree().getStartLine();
            // Comments may be direct children of BR or nested inside a WS child of BR.
            FormatUtils.collectNonInlineComments(br, brStartLine, toRelocate);

            if (toRelocate.isEmpty()) {
                continue;
            }

            FormatUtils.detachCollectedComments(toRelocate);

            // Clean up residual whitespace in the BR after removing comments. If the BR's WS child
            // no longer contains any COMMENT children (only SP/NEWLINE/COMMA tokens remain), remove
            // it entirely. This prevents the residual tokens from inflating the member's getEndLine(),
            // which would corrupt line-number heuristics in FormatVisitor.
            cleanupBrWhitespace(br.getTree());

            // Find or create the WS node that follows this member in the container.
            // The member's next sibling should be a WS node (inter-member whitespace) or the closing brace.
            TreeCursor nextSibling = member.getNextSibling();
            TokenTree wsTarget;
            if (nextSibling != null && nextSibling.getTree().getType() == TreeType.WS) {
                wsTarget = nextSibling.getTree();
            } else {
                // No WS sibling exists; create one and insert it after the member.
                wsTarget = TokenTree.of(TreeType.WS);
                insertAfter(container.getTree(), member.getTree(), wsTarget);
            }

            // Prepend the relocated comments to the WS node (before any existing content).
            // Insert in reverse order at position 0 to preserve original comment ordering.
            List<TokenTree> existingChildren = new ArrayList<>(wsTarget.getChildren());
            // Clear and rebuild: relocated comments first, then existing children.
            for (TokenTree child : existingChildren) {
                wsTarget.removeChild(child);
            }
            for (Pair<TokenTree, TokenTree> entry : toRelocate) {
                wsTarget.appendChild(entry.getRight());
            }
            for (TokenTree child : existingChildren) {
                wsTarget.appendChild(child);
            }
        }
    }

    // Remove WS children of BR that no longer contain any COMMENT nodes. This is safe because
    // downstream passes (FixBadDocComments, FormatVisitor) do not depend on BR WS structure for
    // non-comment tokens; they only inspect BR for comments via getComments() or findChildrenByType.
    private void cleanupBrWhitespace(TokenTree brTree) {
        List<TokenTree> wsChildren = new ArrayList<>();
        for (TokenTree child : brTree.getChildren()) {
            if (child.getType() == TreeType.WS) {
                wsChildren.add(child);
            }
        }
        for (TokenTree ws : wsChildren) {
            boolean hasComment = false;
            for (TokenTree wsChild : ws.getChildren()) {
                if (wsChild.getType() == TreeType.COMMENT) {
                    hasComment = true;
                    break;
                }
            }
            if (!hasComment) {
                brTree.removeChild(ws);
            }
        }
    }

    private void insertAfter(TokenTree parent, TokenTree target, TokenTree toInsert) {
        // TokenTree has no indexed insert API (only appendChild), so we snapshot the children,
        // clear the parent, and rebuild with the new node inserted at the correct position.
        List<TokenTree> snapshot = new ArrayList<>(parent.getChildren());
        int index = -1;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i) == target) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            parent.appendChild(toInsert);
            return;
        }
        // Clear and rebuild with the insertion.
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            parent.removeChild(snapshot.get(i));
        }
        for (int i = 0; i < snapshot.size(); i++) {
            parent.appendChild(snapshot.get(i));
            if (i == index) {
                parent.appendChild(toInsert);
            }
        }
    }
}
