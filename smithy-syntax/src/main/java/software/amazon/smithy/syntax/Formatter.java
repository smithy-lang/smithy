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

import com.opencastsoftware.prettier4j.Doc;
import java.util.List;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.utils.StringUtils;

/**
 * Formats valid Smithy IDL models.
 *
 * <p>This formatter will by default sort use statements, remove unused use statements, and fix documentation
 * comments that should be normal comments.
 */
public final class Formatter {

    static final Doc LBRACE = Doc.text("{");
    static final Doc RBRACE = Doc.text("}");
    static final Doc LBRACKET = Doc.text("[");
    static final Doc RBRACKET = Doc.text("]");
    static final Doc LPAREN = Doc.text("(");
    static final Doc RPAREN = Doc.text(")");
    static final Doc LINE_OR_COMMA = Doc.lineOr(Doc.text(", "));
    static final Doc SPACE = Doc.text(" ");
    static final Doc LINE_OR_SPACE = Doc.lineOrSpace();

    private Formatter() {}

    /**
     * Formats the given token tree, wrapping lines at 120 characters.
     *
     * @param root Root {@link TreeType#IDL} tree node to format.
     * @return Returns the formatted model as a string.
     * @throws ModelSyntaxException if the model contains errors.
     */
    public static String format(TokenTree root) {
        return format(root, 120);
    }

    /**
     * Formats the given token tree.
     *
     * @param root     Root {@link TreeType#IDL} tree node to format.
     * @param maxWidth Maximum line width.
     * @return Returns the formatted model as a string.
     * @throws ModelSyntaxException if the model contains errors.
     */
    public static String format(TokenTree root, int maxWidth) {
        List<TreeCursor> errors = root.zipper().findChildrenByType(TreeType.ERROR);

        if (!errors.isEmpty()) {
            throw new ModelSyntaxException("Cannot format invalid models: " + errors.get(0).getTree().getError(),
                                           errors.get(0));
        }

        root = new SortUseStatements().apply(root);
        root = new FixBadDocComments().apply(root);
        root = new RemoveUnusedUseStatements().apply(root);

        // Strip trailing spaces from each line.
        String result = new FormatVisitor(maxWidth).visit(root.zipper()).render(maxWidth).trim();
        StringBuilder builder = new StringBuilder();
        for (String line : result.split(System.lineSeparator())) {
            builder.append(StringUtils.stripEnd(line, " \t")).append(System.lineSeparator());
        }
        return builder.toString();
    }
}
