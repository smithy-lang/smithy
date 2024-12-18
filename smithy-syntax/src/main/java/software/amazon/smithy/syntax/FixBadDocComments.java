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
                if (nextSibling == null || nextSibling.getFirstChild(TreeType.APPLY_STATEMENT) != null) {
                    updateNestedChildren(br);
                }
            }
            // Fix any trailing doc comments in shape bodies
            for (TreeCursor members : shapeStatements.findChildrenByType(TreeType.SHAPE_MEMBERS)) {
                TreeCursor closeBrace = members.getLastChild();
                if (closeBrace != null) {
                    TreeCursor possibleTrailingWs = closeBrace.getPreviousSibling();
                    if (possibleTrailingWs != null && possibleTrailingWs.getTree().getType() == TreeType.WS) {
                        updateDirectChildren(possibleTrailingWs);
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

    private void updateComment(TreeCursor cursor) {
        cursor.getTree().tokens().findFirst().ifPresent(token -> {
            CapturedToken updatedToken = token.toBuilder()
                    // Trim the first "/" from the lexeme. Note that this does make the spans inaccurate.
                    .lexeme(token.getLexeme().subSequence(1, token.getLexeme().length()))
                    .build();
            TokenTree updatedTree = TokenTree.of(TreeType.COMMENT);
            updatedTree.appendChild(TokenTree.of(updatedToken));
            cursor.getParent().getTree().replaceChild(cursor.getTree(), updatedTree);
        });
    }
}
