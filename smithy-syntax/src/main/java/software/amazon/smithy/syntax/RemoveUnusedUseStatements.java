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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.model.shapes.ShapeId;

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
        TreeCursor shapeStatements = Objects.requireNonNull(root.getFirstChild(TreeType.SHAPE_SECTION))
                .getFirstChild(TreeType.SHAPE_STATEMENTS);

        if (shapeStatements == null) {
            return tree;
        }

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

        // Anything left in the map needs to be removed from the tree.
        for (TreeCursor unused : useShapeNames.values()) {
            LOGGER.fine(() -> "Removing unused use statement: "
                              + unused.getFirstChild(TreeType.ABSOLUTE_ROOT_SHAPE_ID)
                                      .getTree().concatTokens());
            unused.getParent().getTree().removeChild(unused.getTree());
        }

        return tree;
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
