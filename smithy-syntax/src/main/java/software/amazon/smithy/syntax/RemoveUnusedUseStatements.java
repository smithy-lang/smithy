/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Pair;

final class RemoveUnusedUseStatements implements Function<TokenTree, TokenTree> {

    private static final Logger LOGGER = Logger.getLogger(RemoveUnusedUseStatements.class.getName());

    @Override
    public TokenTree apply(TokenTree tree) {
        TreeCursor root = tree.zipper();
        Map<String, TreeCursor> useShapeNames = parseShapeIds(root);

        if (useShapeNames.isEmpty()) {
            return tree;
        }

        // SHAPE_SECTION is always present at this point if there are detected use statements.
        TreeCursor shapeSection = Objects.requireNonNull(root.getFirstChild(TreeType.SHAPE_SECTION));
        TreeCursor shapeStatements = shapeSection.getFirstChild(TreeType.SHAPE_STATEMENTS);

        if (shapeStatements == null) {
            return tree;
        }

        // Remove every shape id referenced in the model file from the map of use
        // statements. Anything left after this loop is unreferenced.
        for (TreeCursor identifier : shapeStatements.findChildrenByType(TreeType.SHAPE_ID)) {
            String name = identifier.getTree().concatTokens();

            // Absolute shape IDs don't interact with use statements.
            if (name.contains("#")) {
                continue;
            }

            // Remove the member if found.
            if (name.contains("$")) {
                name = name.substring(0, name.indexOf("$"));
            }

            useShapeNames.remove(name);
        }

        if (useShapeNames.isEmpty()) {
            return tree;
        }

        // Before removing an unused use statement, we need to handle its trailing whitespace.
        // A use statement's trailing BR greedily captures every comment written above the next
        // statement, including any doc comments that document following shapes. Removing the
        // statement wholesale would delete those comments and silently change the model, so the
        // comments that belong to surviving statements are preserved before removal.
        handleAllTrailingComments(shapeSection, useShapeNames.values());

        // Anything left in the map needs to be removed from the tree.
        for (TreeCursor unused : useShapeNames.values()) {
            LOGGER.fine(() -> "Removing unused use statement: "
                    + unused.getFirstChild(TreeType.ABSOLUTE_ROOT_SHAPE_ID)
                            .getTree()
                            .concatTokens());
            unused.getParent().getTree().removeChild(unused.getTree());
        }

        return tree;
    }

    // Preserve or discard the comments captured by the BRs of the use statements being removed.
    // A comment belongs to the statement directly below it, so each BR holds the comments that
    // lead its successor (the next use statement, or the following shape for the last import).
    private void handleAllTrailingComments(TreeCursor shapeSection, Collection<TreeCursor> removedStatements) {
        // We already know there's a use section, or we wouldn't be here.
        TreeCursor useSection = Objects.requireNonNull(shapeSection.getFirstChild(TreeType.USE_SECTION));

        // The defer target is the trailing BR of the most recent surviving statement.
        // This is where comments will be moved to if their holder is being removed but
        // the comments themselves need to be kept. It starts at the namespace statement's
        // BR, which always survives.
        TreeCursor namespaceStatement =
                Objects.requireNonNull(shapeSection.getFirstChild(TreeType.NAMESPACE_STATEMENT));
        TreeCursor deferTarget = Objects.requireNonNull(namespaceStatement.getFirstChild(TreeType.BR));

        // Create an identity set to be able to easily identify removed statements while
        // iterating. An identity set in particular is used because nodes are mutable and
        // are mutated, so their hashes can and do change.
        Set<TreeCursor> removed = Collections.newSetFromMap(new IdentityHashMap<>());
        removed.addAll(removedStatements);

        // Go through each use statement, removing or preserving its comments as necessary.
        TreeCursor holderBr = deferTarget;
        boolean holderRemoved = false;
        for (TreeCursor useStatement : useSection.getChildrenByType(TreeType.USE_STATEMENT)) {
            boolean successorRemoved = removedStatements.contains(useStatement);
            handleTrailingComments(holderBr, holderRemoved, successorRemoved, deferTarget);
            if (!holderRemoved) {
                deferTarget = holderBr;
            }
            holderBr = Objects.requireNonNull(useStatement.getFirstChild(TreeType.BR));
            holderRemoved = successorRemoved;
        }

        // The last use statement's BR leads the following shape, which always survives.
        handleTrailingComments(holderBr, holderRemoved, false, deferTarget);
    }

    private void handleTrailingComments(
            TreeCursor holderBr,
            boolean holderRemoved,
            boolean successorRemoved,
            TreeCursor deferTarget
    ) {
        if (holderBr == null) {
            return;
        }

        List<Pair<TokenTree, TokenTree>> comments = new ArrayList<>();
        FormatUtils.collectNonInlineComments(holderBr, holderBr.getTree().getStartLine(), comments);
        if (comments.isEmpty()) {
            return;
        }

        if (successorRemoved) {
            // If the successor is removed, the comments that precede it also need to be removed.
            // If the statement that those comments are attached to is also going to be removed,
            // the comments will get removed when that whole tree is removed. If the holding
            // statement isn't going to be removed, we need to do it manually.
            if (!holderRemoved) {
                FormatUtils.detachCollectedComments(comments);
            }
        } else if (holderRemoved) {
            // If the successor isn't being removed, but the statement holding the comments is,
            // we need to add those comments up to the most recent surviving statement.
            TokenTree targetWs = findOrCreateWs(deferTarget.getTree());
            comments.forEach(entry -> targetWs.appendChild(entry.getRight()));
        }
        // Otherwise the successor and its holder both survive, so the comments stay where they are.
    }

    private TokenTree findOrCreateWs(TokenTree br) {
        for (TokenTree child : br.getChildren()) {
            if (child.getType() == TreeType.WS) {
                return child;
            }
        }
        TokenTree ws = TokenTree.of(TreeType.WS);
        br.appendChild(ws);
        return ws;
    }

    // Create a map of shape name to the TreeCursor of the use statement.
    private Map<String, TreeCursor> parseShapeIds(TreeCursor root) {
        List<TreeCursor> useStatements = Optional.ofNullable(root.getFirstChild(TreeType.SHAPE_SECTION))
                .flatMap(shapeSection -> Optional.ofNullable(shapeSection.getFirstChild(TreeType.USE_SECTION)))
                .map(useSection -> useSection.getChildrenByType(TreeType.USE_STATEMENT))
                .orElse(Collections.emptyList());

        Map<String, TreeCursor> result = new HashMap<>(useStatements.size());
        for (TreeCursor useStatement : useStatements) {
            TreeCursor idCursor = useStatement.getFirstChild(TreeType.ABSOLUTE_ROOT_SHAPE_ID);
            if (idCursor != null) {
                result.put(ShapeId.from(idCursor.getTree().concatTokens()).getName(), useStatement);
            }
        }

        return result;
    }
}
