/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.List;
import java.util.function.Function;
import software.amazon.smithy.model.loader.IdlToken;

/**
 * Rewrites invalid documentation comments that don't come directly before a shape or member to be a normal comment.
 *
 * <p>TODO: This does not remove every possible invalid documentation comment (e.g., doc comments after traits).
 */
final class FixBadDocComments implements Function<TokenTree, TokenTree> {
    @Override
    public TokenTree apply(TokenTree tree) {
        TreeCursor root = tree.zipper();

        // These sections can never have doc comments.
        updateDirectChildren(root.getFirstChild(TreeType.WS));
        updateNestedChildren(root.getFirstChild(TreeType.CONTROL_SECTION));
        updateNestedChildren(root.getFirstChild(TreeType.METADATA_SECTION));

        // These sections should always be present in every model.
        TreeCursor shapeSection = root.getFirstChild(TreeType.SHAPE_SECTION);
        if (shapeSection == null) {
            return tree;
        }

        TreeCursor useSection = shapeSection.getFirstChild(TreeType.USE_SECTION);
        if (useSection == null) {
            return tree;
        }

        // Remove doc comments from NAMESPACE_STATEMENT if there are use statements (that is, no way a doc comment
        // is applied to the next shape statement).
        if (!useSection.getTree().isEmpty()) {
            updateNestedChildren(shapeSection.getFirstChild(TreeType.NAMESPACE_STATEMENT));

            // Remove doc comments from all but the last use statement.
            List<TreeCursor> useStatements = useSection.getChildrenByType(TreeType.USE_STATEMENT);
            for (int i = 0; i < useStatements.size() - 1; i++) {
                updateNestedChildren(useStatements.get(i));
            }
        }

        // Doc comments are never allowed in NODE_VALUE values.
        for (TreeCursor cursor : shapeSection.findChildrenByType(TreeType.NODE_VALUE)) {
            updateNestedChildren(cursor);
        }

        // Doc comments are never allowed in TRAIT values.
        for (TreeCursor cursor : shapeSection.findChildrenByType(TreeType.TRAIT)) {
            updateNestedChildren(cursor);
        }

        // Doc comments should not appear in TRAIT_STATEMENTS.
        for (TreeCursor cursor : shapeSection.findChildrenByType(TreeType.TRAIT_STATEMENTS)) {
            updateNestedChildren(cursor);
        }

        // Fix doc comments that come before apply statements.
        TreeCursor shapeStatements = shapeSection.getFirstChild(TreeType.SHAPE_STATEMENTS);
        if (shapeStatements != null) {
            // Find BRs in shape statements and look at the next sibling.
            for (TreeCursor br : shapeStatements.getChildrenByType(TreeType.BR)) {
                TreeCursor nextSibling = br.getNextSibling();
                if (nextSibling == null
                        || nextSibling.getFirstChild(TreeType.APPLY_STATEMENT) != null
                        || hasBlankLineBeforeNextStatement(br, nextSibling)) {
                    updateNestedChildren(br);
                }
            }
            // Fix any trailing doc comments in member bodies (structures, enums, and operations).
            for (TreeCursor members : shapeStatements.findChildrenByType(
                    TreeType.SHAPE_MEMBERS,
                    TreeType.ENUM_SHAPE_MEMBERS,
                    TreeType.OPERATION_BODY)) {
                TreeCursor closeBrace = members.getLastChild();
                List<TreeCursor> wsNodes = members.getChildrenByType(TreeType.WS);
                for (TreeCursor ws : wsNodes) {
                    // Trailing WS right before closing brace: all doc comments are invalid.
                    if (closeBrace != null && ws.getNextSibling() != null
                            && ws.getNextSibling().getTree() == closeBrace.getTree()) {
                        updateDirectChildren(ws);
                        continue;
                    }
                    // Between-member WS: only same-line trailing doc comments are invalid.
                    // Use the NODE_VALUE end line for members with VALUE_ASSIGNMENT to avoid
                    // the BR's NEWLINE token inflating the member's end line.
                    TreeCursor prevSibling = ws.getPreviousSibling();
                    if (prevSibling != null) {
                        int prevEndLine = FormatUtils.valueEndLine(prevSibling);
                        for (TreeCursor comment : ws.getChildrenByType(TreeType.COMMENT)) {
                            if (comment.getTree().getStartLine() == prevEndLine && isDocComment(comment)) {
                                updateComment(comment);
                            }
                        }
                    }
                }
            }

            // Fix same-line trailing doc comments inside VALUE_ASSIGNMENT BR nodes
            // (e.g., `a: String = "" /// comment`). Non-inline comments have already been
            // relocated to WS siblings by RelocateMemberComments, so only same-line comments
            // remain in the BR.
            for (TreeCursor valueAssignment : shapeStatements.findChildrenByType(TreeType.VALUE_ASSIGNMENT)) {
                TreeCursor nodeValue = valueAssignment.getFirstChild(TreeType.NODE_VALUE);
                if (nodeValue == null) {
                    continue;
                }
                int valueEndLine = nodeValue.getTree().getEndLine();
                for (TreeCursor comment : valueAssignment.findChildrenByType(TreeType.COMMENT)) {
                    if (isDocComment(comment) && comment.getTree().getStartLine() == valueEndLine) {
                        updateComment(comment);
                    }
                }
            }
        }

        return tree;
    }

    private void updateDirectChildren(TreeCursor container) {
        if (container != null) {
            updateChildren(container.getChildrenByType(TreeType.COMMENT));
        }
    }

    private void updateNestedChildren(TreeCursor container) {
        if (container != null) {
            updateChildren(container.findChildrenByType(TreeType.COMMENT));
        }
    }

    private void updateChildren(List<TreeCursor> children) {
        for (TreeCursor comment : children) {
            if (isDocComment(comment)) {
                updateComment(comment);
            }
        }
    }

    private boolean isDocComment(TreeCursor cursor) {
        return cursor.getTree()
                .tokens()
                .findFirst()
                .filter(token -> token.getIdlToken() == IdlToken.DOC_COMMENT)
                .isPresent();
    }

    // A banner is a comment whose lexeme begins with 4 or more slashes (e.g., `//////` or
    // `////// Shapes`). Banners are visual section dividers; their extra slashes are content,
    // not prefix, and must survive demotion intact.
    private static boolean isBannerLexeme(CharSequence lexeme) {
        return lexeme.length() >= 4
                && lexeme.charAt(0) == '/'
                && lexeme.charAt(1) == '/'
                && lexeme.charAt(2) == '/'
                && lexeme.charAt(3) == '/';
    }

    // True iff the cursor wraps a DOC_COMMENT token whose lexeme is a banner.
    private boolean isBannerDocComment(TreeCursor cursor) {
        if (!isDocComment(cursor)) {
            return false;
        }
        return cursor.getTree().tokens().findFirst().map(t -> isBannerLexeme(t.getLexeme())).orElse(false);
    }

    // Detect a blank line between a doc-comment block in a BR and the start of the next shape
    // statement, and decide whether the block should be demoted. The rule is asymmetric:
    //
    //   - A standard `///` doc comment separated from its shape by a blank line is still a
    //     doc comment for that shape; the formatter normalizes the blank line elsewhere and the
    //     doc comment survives.
    //
    //   - A banner doc comment (`////` or longer) separated from its shape by a blank line is
    //     a section divider, not documentation. Section dividers can't span blank-line gaps, so
    //     the entire block of doc comments preceding that gap is demoted to regular comments.
    //
    // This method returns true when both conditions hold: a blank line exists between the BR's
    // last comment and the next shape, AND at least one of the BR's doc comments is a banner.
    private boolean hasBlankLineBeforeNextStatement(TreeCursor br, TreeCursor nextStatement) {
        List<TreeCursor> comments = br.findChildrenByType(TreeType.COMMENT);
        if (comments.isEmpty()) {
            return false;
        }
        // A comment lexeme includes its trailing newline, so a comment on source line N has
        // end line N+1. The strict greater-than therefore fires only when the next statement
        // starts at N+2 or later, i.e., when there is at least one blank source line between
        // the last comment and the next statement.
        int lastCommentEndLine = comments.get(comments.size() - 1).getTree().getEndLine();
        if (nextStatement.getTree().getStartLine() <= lastCommentEndLine) {
            return false;
        }
        for (TreeCursor comment : comments) {
            if (isBannerDocComment(comment)) {
                return true;
            }
        }
        return false;
    }

    private void updateComment(TreeCursor cursor) {
        cursor.getTree().tokens().findFirst().ifPresent(token -> {
            CharSequence lexeme = token.getLexeme();
            // Banners (4+ leading slashes like `////// Banner`) preserve their full lexeme so the
            // extra slashes survive demotion as content of a regular comment. The renderer keys
            // off the IdlToken type, so we mark the comment as COMMENT and let the formatter
            // produce `// <rest of lexeme>`. Standard `/// X` doc comments still drop one slash
            // here, producing the conventional `// X` lexeme expected by the rest of the pipeline.
            CapturedToken.Builder builder = token.toBuilder().token(IdlToken.COMMENT);
            if (!isBannerLexeme(lexeme)) {
                // Trim the first "/" from the lexeme. Note that this does make the spans inaccurate.
                builder.lexeme(lexeme.subSequence(1, lexeme.length()));
            }
            CapturedToken updatedToken = builder.build();
            TokenTree updatedTree = TokenTree.of(TreeType.COMMENT);
            updatedTree.appendChild(TokenTree.of(updatedToken));
            cursor.getParent().getTree().replaceChild(cursor.getTree(), updatedTree);
        });
    }
}
