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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.StringUtils;

/**
 * Formats valid Smithy IDL models.
 *
 * <p>This formatter will by default sort use statements, remove unused use statements, and fix documentation
 * comments that should be normal comments.
 */
public final class Formatter {

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
        String result = new TreeVisitor(maxWidth).visit(root.zipper()).render(maxWidth).trim();
        StringBuilder builder = new StringBuilder();
        for (String line : result.split(System.lineSeparator())) {
            builder.append(StringUtils.stripEnd(line, " \t")).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static final class TreeVisitor {

        private static final Doc LINE_OR_COMMA = Doc.lineOr(Doc.text(", "));
        private static final Doc SPACE = Doc.text(" ");
        private static final Doc LINE_OR_SPACE = Doc.lineOrSpace();

        // width is needed since intermediate renders are used to detect when newlines are used in a statement.
        private final int width;

        // Used to handle extracting comments out of whitespace of prior statements.
        private Doc pendingComments = Doc.empty();

        private TreeVisitor(int width) {
            this.width = width;
        }

        private Doc visit(TreeCursor cursor) {
            if (cursor == null) {
                return Doc.empty();
            }

            TokenTree tree = cursor.getTree();

            switch (tree.getType()) {
                case IDL: {
                    return visit(cursor.getFirstChild(TreeType.WS))
                            .append(visit(cursor.getFirstChild(TreeType.CONTROL_SECTION)))
                            .append(visit(cursor.getFirstChild(TreeType.METADATA_SECTION)))
                            .append(visit(cursor.getFirstChild(TreeType.SHAPE_SECTION)))
                            .append(flushBrBuffer());
                }

                case CONTROL_SECTION: {
                    return section(cursor, TreeType.CONTROL_STATEMENT);
                }

                case METADATA_SECTION: {
                    return section(cursor, TreeType.METADATA_STATEMENT);
                }

                case SHAPE_SECTION: {
                    return Doc.intersperse(Doc.line(), cursor.children().map(this::visit));
                }

                case SHAPE_STATEMENTS: {
                    Doc result = Doc.empty();
                    Iterator<TreeCursor> childIterator = cursor.getChildren().iterator();
                    int i = 0;
                    while (childIterator.hasNext()) {
                        if (i++ > 0) {
                            result = result.append(Doc.line());
                        }
                        result = result.append(visit(childIterator.next())) // SHAPE
                                .append(visit(childIterator.next()))        // BR
                                .append(Doc.line());
                    }
                    return result;
                }

                case CONTROL_STATEMENT: {
                    return flushBrBuffer()
                            .append(Doc.text("$"))
                            .append(visit(cursor.getFirstChild(TreeType.NODE_OBJECT_KEY)))
                            .append(Doc.text(": "))
                            .append(visit(cursor.getFirstChild(TreeType.NODE_VALUE)))
                            .append(visit(cursor.getFirstChild(TreeType.BR)));
                }

                case METADATA_STATEMENT: {
                    return flushBrBuffer()
                            .append(Doc.text("metadata "))
                            .append(visit(cursor.getFirstChild(TreeType.NODE_OBJECT_KEY)))
                            .append(Doc.text(" = "))
                            .append(visit(cursor.getFirstChild(TreeType.NODE_VALUE)))
                            .append(visit(cursor.getFirstChild(TreeType.BR)));
                }

                case NAMESPACE_STATEMENT: {
                    return Doc.line()
                            .append(flushBrBuffer())
                            .append(Doc.text("namespace "))
                            .append(visit(cursor.getFirstChild(TreeType.NAMESPACE)))
                            .append(visit(cursor.getFirstChild(TreeType.BR)));
                }

                case USE_SECTION: {
                    return section(cursor, TreeType.USE_STATEMENT);
                }

                case USE_STATEMENT: {
                    return flushBrBuffer()
                            .append(Doc.text("use "))
                            .append(visit(cursor.getFirstChild(TreeType.ABSOLUTE_ROOT_SHAPE_ID)))
                            .append(visit(cursor.getFirstChild(TreeType.BR)));
                }

                case SHAPE_OR_APPLY_STATEMENT:
                case SHAPE:
                case OPERATION_PROPERTY:
                case APPLY_STATEMENT:
                case NODE_VALUE:
                case NODE_KEYWORD:
                case NODE_STRING_VALUE:
                case SIMPLE_TYPE_NAME:
                case ENUM_TYPE_NAME:
                case AGGREGATE_TYPE_NAME:
                case ENTITY_TYPE_NAME: {
                    return visit(cursor.getFirstChild());
                }

                case SHAPE_STATEMENT: {
                    return flushBrBuffer()
                            .append(visit(cursor.getFirstChild(TreeType.WS)))
                            .append(visit(cursor.getFirstChild(TreeType.TRAIT_STATEMENTS)))
                            .append(visit(cursor.getFirstChild(TreeType.SHAPE)));
                }

                case SIMPLE_SHAPE: {
                    return formatShape(cursor, visit(cursor.getFirstChild(TreeType.SIMPLE_TYPE_NAME)), null);
                }

                case ENUM_SHAPE: {
                    return skippedComments(cursor, false)
                            .append(formatShape(
                                    cursor,
                                    visit(cursor.getFirstChild(TreeType.ENUM_TYPE_NAME)),
                                    visit(cursor.getFirstChild(TreeType.ENUM_SHAPE_MEMBERS))));
                }

                case ENUM_SHAPE_MEMBERS: {
                    return renderMembers(cursor, TreeType.ENUM_SHAPE_MEMBER);
                }

                case ENUM_SHAPE_MEMBER: {
                    return visit(cursor.getFirstChild(TreeType.TRAIT_STATEMENTS))
                            .append(visit(cursor.getFirstChild(TreeType.IDENTIFIER)))
                            .append(visit(cursor.getFirstChild(TreeType.VALUE_ASSIGNMENT)));
                }

                case AGGREGATE_SHAPE: {
                    return skippedComments(cursor, false)
                            .append(formatShape(
                                    cursor,
                                    visit(cursor.getFirstChild(TreeType.AGGREGATE_TYPE_NAME)),
                                    visit(cursor.getFirstChild(TreeType.SHAPE_MEMBERS))));
                }

                case FOR_RESOURCE: {
                    return Doc.text("for ").append(visit(cursor.getFirstChild(TreeType.SHAPE_ID)));
                }

                case SHAPE_MEMBERS: {
                    return renderMembers(cursor, TreeType.SHAPE_MEMBER);
                }

                case SHAPE_MEMBER: {
                    return visit(cursor.getFirstChild(TreeType.TRAIT_STATEMENTS))
                            .append(visit(cursor.getFirstChild(TreeType.ELIDED_SHAPE_MEMBER)))
                            .append(visit(cursor.getFirstChild(TreeType.EXPLICIT_SHAPE_MEMBER)))
                            .append(visit(cursor.getFirstChild(TreeType.VALUE_ASSIGNMENT)));
                }

                case EXPLICIT_SHAPE_MEMBER: {
                    return visit(cursor.getFirstChild(TreeType.IDENTIFIER))
                            .append(Doc.text(": "))
                            .append(visit(cursor.getFirstChild(TreeType.SHAPE_ID)));
                }

                case ELIDED_SHAPE_MEMBER: {
                    return Doc.text("$").append(visit(cursor.getFirstChild(TreeType.IDENTIFIER)));
                }

                case ENTITY_SHAPE: {
                    return skippedComments(cursor, false)
                            .append(formatShape(
                                    cursor,
                                    visit(cursor.getFirstChild(TreeType.ENTITY_TYPE_NAME)),
                                    Doc.lineOrSpace().append(visit(cursor.getFirstChild(TreeType.NODE_OBJECT)))));
                }

                case OPERATION_SHAPE: {
                    return skippedComments(cursor, false)
                            .append(formatShape(cursor, Doc.text("operation"),
                                                visit(cursor.getFirstChild(TreeType.OPERATION_BODY))));
                }

                case OPERATION_BODY: {
                    return renderMembers(cursor, TreeType.OPERATION_PROPERTY);
                }

                case OPERATION_INPUT: {
                    TreeCursor simpleTarget = cursor.getFirstChild(TreeType.SHAPE_ID);
                    return skippedComments(cursor, false)
                            .append(Doc.text("input"))
                            .append(simpleTarget == null
                                    ? visit(cursor.getFirstChild(TreeType.INLINE_AGGREGATE_SHAPE))
                                    : Doc.text(": ")).append(visit(simpleTarget));
                }

                case OPERATION_OUTPUT: {
                    TreeCursor simpleTarget = cursor.getFirstChild(TreeType.SHAPE_ID);
                    return skippedComments(cursor, false)
                            .append(Doc.text("output"))
                            .append(simpleTarget == null
                                    ? visit(cursor.getFirstChild(TreeType.INLINE_AGGREGATE_SHAPE))
                                    : Doc.text(": ")).append(visit(simpleTarget));
                }

                case INLINE_AGGREGATE_SHAPE: {
                    boolean hasComment = hasComment(cursor);
                    boolean hasTraits = Optional.ofNullable(cursor.getFirstChild(TreeType.TRAIT_STATEMENTS))
                                                  .filter(c -> !c.getChildrenByType(TreeType.TRAIT).isEmpty())
                                                  .isPresent();
                    Doc memberDoc = visit(cursor.getFirstChild(TreeType.SHAPE_MEMBERS));
                    if (hasComment || hasTraits) {
                        return Doc.text(" :=")
                                .append(Doc.line())
                                .append(skippedComments(cursor, false))
                                .append(visit(cursor.getFirstChild(TreeType.TRAIT_STATEMENTS)))
                                .append(formatShape(cursor, Doc.empty(), memberDoc))
                                .indent(4);
                    }

                    return formatShape(cursor, Doc.text(" :="), memberDoc);
                }

                case OPERATION_ERRORS: {
                    return skippedComments(cursor, false)
                            .append(Doc.text("errors: "))
                            .append(bracketed("[", "]", cursor, TreeType.SHAPE_ID));
                }

                case MIXINS: {
                    return Doc.text("with ")
                            .append(bracketed("[", "]", cursor, cursor, child -> {
                                return child.getTree().getType() == TreeType.SHAPE_ID
                                       ? Stream.of(child)
                                       : Stream.empty();
                            }));
                }

                case VALUE_ASSIGNMENT: {
                    return Doc.text(" = ")
                            .append(visit(cursor.getFirstChild(TreeType.NODE_VALUE)))
                            .append(visit(cursor.getFirstChild(TreeType.BR)));
                }

                case TRAIT_STATEMENTS: {
                    return Doc.intersperse(
                            Doc.line(),
                            cursor.children()
                                    // Skip WS nodes that have no comments.
                                    .filter(c -> c.getTree().getType() == TreeType.TRAIT || hasComment(c))
                                    .map(this::visit))
                            .append(tree.isEmpty() ? Doc.empty() : Doc.line());
                }

                case TRAIT: {
                    return Doc.text("@")
                            .append(visit(cursor.getFirstChild(TreeType.SHAPE_ID)))
                            .append(visit(cursor.getFirstChild(TreeType.TRAIT_BODY)));
                }

                case TRAIT_BODY: {
                    TreeCursor structuredBody = cursor.getFirstChild(TreeType.TRAIT_STRUCTURE);
                    if (structuredBody != null) {
                        return bracketed("(", ")", cursor, cursor, child -> {
                            if (child.getTree().getType() == TreeType.TRAIT_STRUCTURE) {
                                // Split WS and NODE_OBJECT_KVP so that they appear on different lines.
                                return child.getChildrenByType(TreeType.NODE_OBJECT_KVP, TreeType.WS).stream();
                            }
                            return Stream.empty();
                        });
                    } else {
                        // Check the inner trait node for hard line breaks rather than the wrapper.
                        TreeCursor traitNode = cursor
                                .getFirstChild(TreeType.TRAIT_NODE)
                                .getFirstChild(TreeType.NODE_VALUE)
                                .getFirstChild(); // The actual node value.
                        return bracketed("(", ")", cursor, traitNode, child -> {
                            if (child.getTree().getType() == TreeType.TRAIT_NODE) {
                                // Split WS and NODE_VALUE so that they appear on different lines.
                                return child.getChildrenByType(TreeType.NODE_VALUE, TreeType.WS).stream();
                            } else {
                                return Stream.empty();
                            }
                        });
                    }
                }

                case TRAIT_NODE: {
                    return visit(cursor.getFirstChild()).append(visit(cursor.getFirstChild(TreeType.WS)));
                }

                case TRAIT_STRUCTURE: {
                    throw new UnsupportedOperationException("Use TRAIT_BODY");
                }

                case APPLY_STATEMENT_SINGULAR: {
                    // If there is an awkward comment before the TRAIT value, hoist it above the statement.
                    return flushBrBuffer()
                            .append(skippedComments(cursor, false))
                            .append(Doc.text("apply "))
                            .append(visit(cursor.getFirstChild(TreeType.SHAPE_ID)))
                            .append(SPACE)
                            .append(visit(cursor.getFirstChild(TreeType.TRAIT)));
                }

                case APPLY_STATEMENT_BLOCK: {
                    // TODO: This renders the "apply" block as a string so that we can trim the contents before adding
                    //   the trailing newline + closing bracket. Otherwise, we'll get a blank, indented line, before
                    //   the closing brace.
                    return flushBrBuffer()
                            .append(Doc.text(skippedComments(cursor, false)
                            .append(Doc.text("apply "))
                            .append(visit(cursor.getFirstChild(TreeType.SHAPE_ID)))
                            .append(Doc.text(" {"))
                            .append(Doc.line().append(visit(cursor.getFirstChild(TreeType.TRAIT_STATEMENTS))).indent(4))
                            .render(width)
                            .trim())
                            .append(Doc.line())
                            .append(Doc.text("}")));
                }

                case NODE_ARRAY: {
                    return bracketed("[", "]", cursor, TreeType.NODE_VALUE);
                }

                case NODE_OBJECT: {
                    return bracketed("{", "}", cursor, TreeType.NODE_OBJECT_KVP);
                }

                case NODE_OBJECT_KVP: {
                    // Since text blocks span multiple lines, when they are the NODE_VALUE for NODE_OBJECT_KVP,
                    // they have to be indented. Since we only format valid models, NODE_OBJECT_KVP is guaranteed to
                    // have a NODE_VALUE child.
                    TreeCursor nodeValue = cursor.getFirstChild(TreeType.NODE_VALUE);
                    boolean isTextBlock = Optional.ofNullable(nodeValue.getFirstChild(TreeType.NODE_STRING_VALUE))
                            .map(nodeString -> nodeString.getFirstChild(TreeType.TEXT_BLOCK))
                            .isPresent();
                    Doc nodeValueDoc = visit(nodeValue);
                    if (isTextBlock) {
                        nodeValueDoc = nodeValueDoc.indent(4);
                    }


                    // Hoist awkward comments in the KVP *before* the KVP rather than between the values and colon.
                    // If there is an awkward comment before the TRAIT value, hoist it above the statement.
                    return skippedComments(cursor, false)
                            .append(visit(cursor.getFirstChild(TreeType.NODE_OBJECT_KEY)))
                            .append(Doc.text(": "))
                            .append(nodeValueDoc);
                }

                case NODE_OBJECT_KEY: {
                    // Unquote object keys that can be unquoted.
                    CharSequence unquoted = Optional.ofNullable(cursor.getFirstChild(TreeType.QUOTED_TEXT))
                            .flatMap(quoted -> quoted.getTree().tokens().findFirst())
                            .map(token -> token.getLexeme().subSequence(1, token.getSpan() - 1))
                            .orElse("");
                    return ShapeId.isValidIdentifier(unquoted)
                           ? Doc.text(unquoted.toString())
                           : Doc.text(tree.concatTokens());
                }

                case TEXT_BLOCK: {
                    // Dispersing the lines of the text block preserves any indentation applied from formatting parent
                    // nodes.
                    List<Doc> lines = Arrays.stream(tree.concatTokens().split(System.lineSeparator()))
                            .map(String::trim)
                            .map(Doc::text)
                            .collect(Collectors.toList());
                    return Doc.intersperse(Doc.line(), lines);
                }

                case TOKEN:
                case QUOTED_TEXT:
                case NUMBER:
                case SHAPE_ID:
                case ROOT_SHAPE_ID:
                case ABSOLUTE_ROOT_SHAPE_ID:
                case SHAPE_ID_MEMBER:
                case NAMESPACE:
                case IDENTIFIER: {
                    return Doc.text(tree.concatTokens());
                }

                case COMMENT: {
                    // Ensure comments have a single space before their content.
                    String contents = tree.concatTokens().trim();
                    if (contents.startsWith("/// ") || contents.startsWith("// ")) {
                        return Doc.text(contents);
                    } else if (contents.startsWith("///")) {
                        return Doc.text("/// " + contents.substring(3));
                    } else {
                        return Doc.text("// " + contents.substring(2));
                    }
                }

                case WS: {
                    // Ignore all whitespace except for comments and doc comments.
                    return Doc.intersperse(
                        Doc.line(),
                        cursor.getChildrenByType(TreeType.COMMENT).stream().map(this::visit)
                    );
                }

                case BR: {
                    pendingComments = Doc.empty();
                    Doc result = Doc.empty();
                    List<TreeCursor> comments = getComments(cursor);
                    for (TreeCursor comment : comments) {
                        if (comment.getTree().getStartLine() == tree.getStartLine()) {
                            result = result.append(SPACE.append(visit(comment)));
                        } else {
                            pendingComments = pendingComments.append(visit(comment)).append(Doc.line());
                        }
                    }
                    return result;
                }

                default: {
                    return Doc.empty();
                }
            }
        }

        private Doc formatShape(TreeCursor cursor, Doc type, Doc members) {
            List<Doc> docs = new EmptyIgnoringList();
            docs.add(type);
            docs.add(visit(cursor.getFirstChild(TreeType.IDENTIFIER)));
            docs.add(visit(cursor.getFirstChild(TreeType.FOR_RESOURCE)));
            docs.add(visit(cursor.getFirstChild(TreeType.MIXINS)));
            Doc result = Doc.intersperse(SPACE, docs);
            return members != null ? result.append(Doc.group(members)) : result;
        }

        private static final class EmptyIgnoringList extends ArrayList<Doc> {
            @Override
            public boolean add(Doc doc) {
                return doc != Doc.empty() && super.add(doc);
            }
        }

        private Doc flushBrBuffer() {
            Doc result = pendingComments;
            pendingComments = Doc.empty();
            return result;
        }

        // Check if a cursor contains direct child comments or a direct child WS that contains comments.
        private boolean hasComment(TreeCursor cursor) {
            return !getComments(cursor).isEmpty();
        }

        // Get direct child comments from a cursor, or from direct WS children that have comments.
        private List<TreeCursor> getComments(TreeCursor cursor) {
            List<TreeCursor> result = new ArrayList<>();
            for (TreeCursor wsOrComment : cursor.getChildrenByType(TreeType.COMMENT, TreeType.WS)) {
                if (wsOrComment.getTree().getType() == TreeType.WS) {
                    result.addAll(wsOrComment.getChildrenByType(TreeType.COMMENT));
                } else {
                    result.add(wsOrComment);
                }
            }
            return result;
        }

        // Concatenate all comments in a tree into a single line delimited Doc.
        private Doc skippedComments(TreeCursor cursor, boolean leadingLine) {
            List<TreeCursor> comments = getComments(cursor);
            if (comments.isEmpty()) {
                return Doc.empty();
            }
            List<Doc> docs = new ArrayList<>(comments.size());
            comments.forEach(c -> docs.add(visit(c).append(Doc.line())));
            return (leadingLine ? Doc.line() : Doc.empty()).append(Doc.fold(docs, Doc::append));
        }

        // Brackets children of childType between open and closed brackets. If the children can fit together
        // on a single line, they are comma separated. If not, they are split onto multiple lines with no commas.
        private Doc bracketed(String open, String close, TreeCursor cursor, TreeType childType) {
            return bracketed(open, close, cursor, cursor, child -> child.getTree().getType() == childType
                                                                   ? Stream.of(child) : Stream.empty());
        }

        private Doc bracketed(String open, String close, TreeCursor cursor, TreeCursor hardLineSubject,
                Function<TreeCursor, Stream<TreeCursor>> childExtractor) {
            Stream<Doc> children = cursor.children()
                    .flatMap(c -> {
                        TreeType type = c.getTree().getType();
                        return type == TreeType.WS || type == TreeType.COMMENT
                               ? Stream.of(c)
                               : childExtractor.apply(c);
                    })
                    .flatMap(c -> {
                        // If the child extracts WS, then filter it down to just comments.
                        return c.getTree().getType() == TreeType.WS
                               ? c.getChildrenByType(TreeType.COMMENT).stream()
                               : Stream.of(c);
                    })
                    .map(this::visit)
                    .filter(doc -> doc != Doc.empty()); // empty lines add extra lines we don't need.

            if (!hasHardLine(hardLineSubject)) {
                return Doc.intersperse(LINE_OR_COMMA, children).bracket(4, Doc.lineOrEmpty(), open, close);
            } else {
                return renderBlock(Doc.text(open), close, Doc.intersperse(Doc.line(), children));
            }
        }

        // Check if the given tree has any hard lines. Nested arrays and objects are always considered hard lines.
        private static boolean hasHardLine(TreeCursor cursor) {
            List<TreeCursor> children = cursor.findChildrenByType(
                    TreeType.COMMENT, TreeType.TEXT_BLOCK, TreeType.NODE_ARRAY, TreeType.NODE_OBJECT,
                    TreeType.QUOTED_TEXT);
            for (TreeCursor child : children) {
                if (child.getTree().getType() != TreeType.QUOTED_TEXT) {
                    return true;
                } else if (child.getTree().getStartLine() != child.getTree().getEndLine()) {
                    // Detect strings with line breaks.
                    return true;
                }
            }
            return false;
        }

        // Renders "members" in braces, grouping related comments and members together.
        private Doc renderMembers(TreeCursor container, TreeType memberType) {
            boolean noComments = container.findChildrenByType(TreeType.COMMENT, TreeType.TRAIT).isEmpty();
            // Separate members by a single line if none have traits or docs, and two lines if any do.
            Doc separator = noComments ? Doc.line() : Doc.line().append(Doc.line());
            List<TreeCursor> members = container.getChildrenByType(memberType, TreeType.WS);
            // Remove WS we don't care about.
            members.removeIf(c -> c.getTree().getType() == TreeType.WS && !hasComment(c));
            // Empty structures render as "{}".
            if (noComments && members.isEmpty()) {
                return Doc.group(LINE_OR_SPACE.append(Doc.text("{}")));
            }

            // Group consecutive comments and members together, and add a new line after each member.
            List<Doc> memberDocs = new ArrayList<>();
            // Start the current result with a buffered comment, if any, or an empty Doc.
            Doc current = flushBrBuffer();
            boolean newLineNeededAfterComment = false;

            for (TreeCursor member : members) {
                if (member.getTree().getType() == TreeType.WS) {
                    newLineNeededAfterComment = true;
                    current = current.append(visit(member));
                } else {
                    if (newLineNeededAfterComment) {
                        current = current.append(Doc.line());
                        newLineNeededAfterComment = false;
                    }
                    current = current.append(visit(member));
                    memberDocs.add(current);
                    current = flushBrBuffer();
                }
            }

            if (current != Doc.empty()) {
                memberDocs.add(current);
            }

            Doc open = LINE_OR_SPACE.append(Doc.text("{"));
            return renderBlock(open, "}", Doc.intersperse(separator, memberDocs));
        }

        // Renders members and anything bracketed that are known to need expansion on multiple lines.
        private Doc renderBlock(Doc open, String close, Doc contents) {
            return open
                    .append(Doc.line().append(contents).indent(4))
                    .append(Doc.line())
                    .append(Doc.text(close));
        }

        // Renders control, metadata, and use sections so that each statement has a leading and trailing newline
        // IFF the statement spans multiple lines (i.e., long value that wraps, comments, etc).
        private Doc section(TreeCursor cursor, TreeType childType) {
            List<TreeCursor> children = cursor.getChildrenByType(childType);

            // Empty sections emit no code.
            if (children.isEmpty()) {
                return Doc.empty();
            }

            // Tracks when a line was just written.
            // Initialized to false since there's no need to ever add a leading line in a section of statements.
            boolean justWroteTrailingLine = true;

            // Sections need a new line to separate them from the previous content.
            // Note: even though this emits a leading newline in every generated model, a top-level String#trim() is
            // used to clean this up.
            Doc result = Doc.line();

            for (int i = 0; i < children.size(); i++) {
                boolean isLast = i == children.size() - 1;
                TreeCursor child = children.get(i);

                // Render the child to a String to detect if a newline was rendered. This is fine to do here since all
                // statements that use this method are rooted at column 0 with no indentation. This rendered text is
                // also used as part of the generated Doc since there's no need to re-analyze each statement.
                String rendered = visit(child).render(width);

                if (rendered.contains(System.lineSeparator())) {
                    if (!justWroteTrailingLine) {
                        result = result.append(Doc.line());
                    }
                    result = result.append(Doc.text(rendered));
                    if (!isLast) {
                        result = result.append(Doc.line());
                        justWroteTrailingLine = true;
                    }
                } else {
                    result = result.append(Doc.text(rendered));
                    justWroteTrailingLine = false;
                }

                result = result.append(Doc.line());
            }

            return result;
        }
    }
}
