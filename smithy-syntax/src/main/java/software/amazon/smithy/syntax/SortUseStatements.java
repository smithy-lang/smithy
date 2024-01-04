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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.loader.IdlToken;

/**
 * Sorts use statements in models alphabetically, case-sensitively.
 */
final class SortUseStatements implements Function<TokenTree, TokenTree> {
    // One of the complications of this transformation is that comments that might document a use statement are
    // captured in the BR production that trails a use statement. So moving a use statement needs to move any trailing
    // comments from its previous sibling to the next previous sibling it's moved after. To make this even more
    // complicated, comments on the same line as a statement should stay with the statement rather than moved.
    @Override
    public TokenTree apply(TokenTree tree) {
        TreeCursor useSection = Objects.requireNonNull(tree.zipper().getFirstChild(TreeType.SHAPE_SECTION))
                .getFirstChild(TreeType.USE_SECTION);

        // All trees should have a USE_SECTION. If the section is empty, then no use statements need to be sorted.
        // Note that if it isn't empty, then there is a guaranteed NAMESPACE_STATEMENT too.
        if (useSection == null || useSection.getTree().isEmpty()) {
            return tree;
        }

        UseDataModel dataModel = new UseDataModel(tree.zipper(), useSection);

        // Remove the original statements since they get sorted and re-added.
        useSection.getChildrenByType(TreeType.USE_STATEMENT)
                .forEach(s -> useSection.getTree().removeChild(s.getTree()));

        dataModel.addFirstUse(useSection);
        dataModel.addSubsequentUse(useSection);

        return tree;
    }

    private static final class UseDataModel {
        List<TokenTree> ids = new ArrayList<>();
        Map<TokenTree, TreeCursor> lineComments = new HashMap<>();
        Map<TokenTree, List<TreeCursor>> leadingComments = new HashMap<>();
        List<TreeCursor> endComments = new ArrayList<>();
        TreeCursor namespaceBrWs;

        UseDataModel(TreeCursor root, TreeCursor useSection) {
            List<TreeCursor> shapeIds = useSection.findChildrenByType(TreeType.ABSOLUTE_ROOT_SHAPE_ID);
            List<TreeCursor> comments = useSection.findChildrenByType(TreeType.COMMENT);

            int commentIndex = 0;
            for (TreeCursor id : shapeIds) {
                ids.add(id.getTree());
                while (commentIndex < comments.size()) {
                    TreeCursor comment = comments.get(commentIndex);
                    if (comment.getTree().getStartLine() == id.getTree().getStartLine()) {
                        lineComments.put(id.getTree(), comment);
                    } else if (comment.getTree().getStartLine() < id.getTree().getStartLine()) {
                        putLeadingComment(id.getTree(), comment);
                    } else {
                        break;
                    }
                    commentIndex++;
                }
            }

            // Any remaining comments are "sticky" since they are on the next section, not the use statement.
            while (commentIndex < comments.size()) {
                TreeCursor comment = comments.get(commentIndex);
                endComments.add(comment);
                commentIndex++;
            }

            findOrCreateNamespaceBrWs(root);

            ids.sort(Comparator.comparing(TokenTree::concatTokens));
        }

        private void findOrCreateNamespaceBrWs(TreeCursor root) {
            // The first use statement could have leading comments that are part of the previous section. If this
            // statement is moved, then the comments from the previous section need to be removed and moved into this
            // section, and potentially replaced with comments from USE_SECTION.
            TreeCursor br = root.findChildrenByType(TreeType.NAMESPACE_STATEMENT)
                    .get(0)
                    .findChildrenByType(TreeType.BR)
                    .get(0);

            namespaceBrWs = br.getFirstChild(TreeType.WS);

            if (namespaceBrWs != null) {
                List<TreeCursor> firstComments = namespaceBrWs.getChildrenByType(TreeType.COMMENT);
                putLeadingComments(ids.get(0), firstComments);
                // Remove these first comments since they get recreated when the use statements are sorted.
                firstComments.forEach(c -> namespaceBrWs.getTree().removeChild(c.getTree()));
            } else {
                // Create an empty WS node since we might need to add comments here later.
                br.getTree().appendChild(TokenTree.of(TreeType.WS));
                namespaceBrWs = br.getFirstChild(TreeType.WS);
            }
        }

        private void putLeadingComment(TokenTree id, TreeCursor comment) {
            putLeadingComments(id, Collections.singletonList(comment));
        }

        private void putLeadingComments(TokenTree id, List<TreeCursor> comments) {
            leadingComments.computeIfAbsent(id, i -> new ArrayList<>()).addAll(comments);
        }

        private List<TreeCursor> getTrailingComments(int index) {
            if (index == ids.size() - 1) {
                return endComments;
            } else {
                return leadingComments.computeIfAbsent(ids.get(index + 1), id -> new ArrayList<>());
            }
        }

        private void addFirstUse(TreeCursor useSection) {
            // Add the first statement. Swap out its leading comments from the preceding NAMESPACE_STATEMENT's BR WS.
            TokenTree id = ids.get(0);
            TokenTree useStatement = createUseStatement(id, lineComments.get(id), getTrailingComments(0));

            // Add any leading comments to the namespace BR WS so they aren't trailing comments.
            if (leadingComments.get(id) != null) {
                leadingComments.get(id).forEach(comment -> namespaceBrWs.getTree().appendChild(comment.getTree()));
            }

            useSection.getTree().appendChild(useStatement);
        }

        private void addSubsequentUse(TreeCursor useSection) {
            for (int i = 1; i < ids.size(); i++) {
                TokenTree id = ids.get(i);
                TokenTree useStatement = createUseStatement(id, lineComments.get(id), getTrailingComments(i));
                useSection.getTree().appendChild(useStatement);
            }
        }

        private TokenTree createUseStatement(TokenTree id, TreeCursor lineComment, List<TreeCursor> trailingComments) {
            TokenTree result = TokenTree.of(TreeType.USE_STATEMENT);

            CapturedToken useToken = CapturedToken.builder().token(IdlToken.IDENTIFIER).lexeme("use").build();
            result.appendChild(TokenTree.of(useToken));

            TokenTree sp = TokenTree.of(TreeType.SP);
            sp.appendChild(TokenTree.of(CapturedToken.builder().token(IdlToken.SPACE).lexeme(" ").build()));
            result.appendChild(sp);

            result.appendChild(id);

            TokenTree br = TokenTree.of(TreeType.BR);
            result.appendChild(br);

            if (lineComment != null) {
                br.appendChild(sp);
                br.appendChild(lineComment.getTree());
            } else {
                br.appendChild(TokenTree.of(CapturedToken.builder()
                        .token(IdlToken.NEWLINE)
                        .lexeme("\n")
                        // Set the start line to be the start line of the use statement
                        // so the formatter  knows it's a line comment.
                        .startLine(id.getStartLine())
                        .endLine(id.getStartLine() + 1)
                        .build()));
            }

            TokenTree ws = TokenTree.of(TreeType.WS);
            trailingComments.forEach(tc -> ws.appendChild(tc.getTree()));
            br.appendChild(ws);

            return result;
        }
    }
}
