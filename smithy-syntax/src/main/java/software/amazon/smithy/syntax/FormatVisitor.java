/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.syntax;

import com.opencastsoftware.prettier4j.Doc;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.StringUtils;

final class FormatVisitor {

    // width is needed since intermediate renders are used to detect when newlines are used in a statement.
    private final int width;

    // Used to handle extracting comments out of whitespace of prior statements.
    private Doc pendingComments = Doc.empty();

    FormatVisitor(int width) {
        this.width = width;
    }

    // Renders members and anything bracketed that are known to need expansion on multiple lines.
    static Doc renderBlock(Doc open, Doc close, Doc contents) {
        return open
                .append(Doc.line().append(contents).indent(4))
                .append(Doc.line())
                .append(close);
    }

    Doc visit(TreeCursor cursor) {
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
                            .append(visit(childIterator.next())) // BR
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
                Doc skippedComments = skippedComments(cursor, false);
                Doc entityType = visit(cursor.getFirstChild(TreeType.ENTITY_TYPE_NAME));
                TreeCursor nodeCursor = cursor.getFirstChild(TreeType.NODE_OBJECT);
                Function<TreeCursor, Doc> visitor = new EntityShapeExtractorVisitor();

                // Place the values of resources, operations, and errors on multiple lines.
                Doc body = new BracketFormatter()
                        .extractChildren(nodeCursor, BracketFormatter.extractByType(TreeType.NODE_OBJECT_KVP, visitor))
                        .detectHardLines(nodeCursor) // If the list is empty, then keep it as "[]".
                        .write();

                return skippedComments.append(formatShape(cursor, entityType, Doc.lineOrSpace().append(body)));
            }

            case OPERATION_SHAPE: {
                return skippedComments(cursor, false)
                        .append(formatShape(cursor,
                                Doc.text("operation"),
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
                                : Doc.text(": "))
                        .append(visit(simpleTarget));
            }

            case OPERATION_OUTPUT: {
                TreeCursor simpleTarget = cursor.getFirstChild(TreeType.SHAPE_ID);
                return skippedComments(cursor, false)
                        .append(Doc.text("output"))
                        .append(simpleTarget == null
                                ? visit(cursor.getFirstChild(TreeType.INLINE_AGGREGATE_SHAPE))
                                : Doc.text(": "))
                        .append(visit(simpleTarget));
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
                // Pull out any comments that come after 'errors' but before the opening '[' so they
                // can be placed before 'errors: ['
                Doc comments = Doc.empty();
                TreeCursor child = cursor.getFirstChild(); // 'errors'
                if (child.getNextSibling().getTree().getType() == TreeType.WS) {
                    comments = comments.append(skippedComments(child.getNextSibling(), false));
                    child = child.getNextSibling(); // skip ws
                }
                child = child.getNextSibling(); // ':'
                if (child.getNextSibling().getTree().getType() == TreeType.WS) {
                    comments = comments.append(skippedComments(child.getNextSibling(), false));
                    child = child.getNextSibling();
                }
                return comments.append(Doc.text("errors: ")
                        .append(new BracketFormatter()
                                .open(Formatter.LBRACKET)
                                .close(Formatter.RBRACKET)
                                .extractChildren(child,
                                        BracketFormatter.extractor(
                                                this::visit,
                                                BracketFormatter.byTypeMapper(TreeType.SHAPE_ID),
                                                BracketFormatter.siblingChildrenSupplier()))
                                .forceLineBreaks() // always put each error on separate lines.
                                .write()));
            }

            case MIXINS: {
                return Doc.text("with ")
                        .append(new BracketFormatter()
                                .open(Formatter.LBRACKET)
                                .close(Formatter.RBRACKET)
                                .extractChildren(cursor, BracketFormatter.extractor(this::visit, child -> {
                                    return child.getTree().getType() == TreeType.SHAPE_ID
                                            ? Stream.of(child)
                                            : Stream.empty();
                                }))
                                .detectHardLines(cursor)
                                .write());
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
                    return new BracketFormatter()
                            .open(Formatter.LPAREN)
                            .close(Formatter.RPAREN)
                            .extractChildren(cursor, BracketFormatter.extractor(this::visit, child -> {
                                if (child.getTree().getType() == TreeType.TRAIT_STRUCTURE) {
                                    // Split WS and NODE_OBJECT_KVP so that they appear on different lines.
                                    return child.getChildrenByType(TreeType.NODE_OBJECT_KVP, TreeType.WS).stream();
                                }
                                return Stream.empty();
                            }))
                            .detectHardLines(cursor)
                            .write();
                } else if (cursor.getFirstChild(TreeType.TRAIT_NODE) != null) {
                    TreeCursor traitNode = cursor.getFirstChild(TreeType.TRAIT_NODE);
                    // Check the inner trait node for hard line breaks rather than the wrapper.
                    TreeCursor actualTraitNodeValue = traitNode
                            .getFirstChild(TreeType.NODE_VALUE)
                            .getFirstChild(); // The actual node value.

                    BracketFormatter formatter = new BracketFormatter()
                            .open(Formatter.LPAREN)
                            .close(Formatter.RPAREN)
                            .extractChildren(cursor, BracketFormatter.extractor(this::visit, child -> {
                                if (child.getTree().getType() == TreeType.TRAIT_NODE) {
                                    // Split WS and NODE_VALUE so that they appear on different lines.
                                    return child.getChildrenByType(TreeType.NODE_VALUE, TreeType.WS).stream();
                                } else {
                                    return Stream.empty();
                                }
                            }));

                    // TraitBody may have leading comments and TraitNode may have trailing comments
                    // which both require line break
                    TreeCursor bodyWs = cursor.getFirstChild(TreeType.WS);
                    TreeCursor traitNodeWs = traitNode.getFirstChild(TreeType.WS);
                    if ((bodyWs != null && !bodyWs.findChildrenByType(TreeType.COMMENT).isEmpty())
                            || (traitNodeWs != null && !traitNodeWs.findChildrenByType(TreeType.COMMENT).isEmpty())) {
                        // Need to line break if there's a leading comment in the trait body no matter what
                        formatter.forceLineBreaks();
                    } else if (actualTraitNodeValue.getTree().getType() == TreeType.NODE_ARRAY
                            || actualTraitNodeValue.getTree().getType() == TreeType.NODE_OBJECT) {
                        // Just inline arrays and objects in trait body
                        formatter.forceInline();
                    } else {
                        // Any other values can use normal detection
                        formatter.detectHardLines(cursor);
                    }

                    return formatter.write();
                } else {
                    // If the trait node is empty, remove the empty parentheses.
                    return Doc.text("");
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
                        .append(Formatter.SPACE)
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
                                .append(Doc.line()
                                        .append(visit(cursor.getFirstChild(
                                                TreeType.TRAIT_STATEMENTS)))
                                        .indent(4))
                                .render(width)
                                .trim())
                                .append(Doc.line())
                                .append(Formatter.RBRACE));
            }

            case NODE_ARRAY: {
                return new BracketFormatter()
                        .open(Formatter.LBRACKET)
                        .close(Formatter.RBRACKET)
                        .extractChildren(cursor, BracketFormatter.extractByType(TreeType.NODE_VALUE, this::visit))
                        .detectHardLines(cursor)
                        .write();
            }

            case NODE_OBJECT: {
                BracketFormatter formatter = new BracketFormatter()
                        .extractChildren(cursor,
                                BracketFormatter.extractByType(TreeType.NODE_OBJECT_KVP,
                                        this::visit));
                if (cursor.getParent().getParent().getTree().getType() == TreeType.NODE_ARRAY) {
                    // Always break objects inside arrays if not empty
                    formatter.forceLineBreaksIfNotEmpty();
                } else {
                    formatter.detectHardLines(cursor);
                }
                return formatter.write();

            }

            case NODE_OBJECT_KVP: {
                return skippedComments(cursor, false)
                        .append(formatNodeObjectKvp(cursor, this::visit, this::visit));
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

                // We need to rebuild the text block to remove any incidental leading whitespace. The easiest way to
                // do that is to use the already parsed and resolved value from the lexer.
                String stringValue = cursor.getTree()
                        .tokens()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("TEXT_BLOCK cursor does not have an IDL token"))
                        .getStringContents();

                // If the last character is a newline, then the closing triple quote must be on the next line.
                boolean endQuoteOnNextLine = stringValue.endsWith("\n") || stringValue.endsWith("\r");

                List<Doc> resultLines = new ArrayList<>();
                resultLines.add(Doc.text("\"\"\""));

                String[] inputLines = stringValue.split("\\r?\\n", -1);
                for (int i = 0; i < inputLines.length; i++) {
                    boolean lastLine = i == inputLines.length - 1;

                    // If this is the last line and the ending quote is on the next line, then skip the extra line.
                    if (endQuoteOnNextLine && lastLine) {
                        break;
                    }

                    String lineValue = inputLines[i];

                    // Trim trailing whitespace.
                    // TODO: This may need to be configurable.
                    lineValue = StringUtils.stripEnd(lineValue, null);

                    // Add the closing quote to this line if it needs to be on the last line.
                    if (lastLine) {
                        lineValue += "\"\"\"";
                    }

                    resultLines.add(Doc.text(lineValue));
                }

                if (endQuoteOnNextLine) {
                    resultLines.add(Doc.text("\"\"\""));
                }

                return Doc.intersperse(Doc.line(), resultLines);
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
                        cursor.getChildrenByType(TreeType.COMMENT).stream().map(this::visit));
            }

            case BR: {
                pendingComments = Doc.empty();
                Doc result = Doc.empty();
                List<TreeCursor> comments = getComments(cursor);
                for (TreeCursor comment : comments) {
                    if (comment.getTree().getStartLine() == tree.getStartLine()) {
                        result = result.append(Formatter.SPACE.append(visit(comment)));
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
        Doc result = Doc.intersperse(Formatter.SPACE, docs);
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
    private static boolean hasComment(TreeCursor cursor) {
        return !getComments(cursor).isEmpty();
    }

    // Get direct child comments from a cursor, or from direct WS children that have comments.
    private static List<TreeCursor> getComments(TreeCursor cursor) {
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
            return Doc.group(Formatter.LINE_OR_SPACE.append(Doc.text("{}")));
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

        Doc open = Formatter.LINE_OR_SPACE.append(Formatter.LBRACE);
        return renderBlock(open, Formatter.RBRACE, Doc.intersperse(separator, memberDocs));
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

    private static Doc formatNodeObjectKvp(
            TreeCursor cursor,
            Function<TreeCursor, Doc> keyVisitor,
            Function<TreeCursor, Doc> valueVisitor
    ) {
        // Since text blocks span multiple lines, when they are the NODE_VALUE for NODE_OBJECT_KVP,
        // they have to be indented. Since we only format valid models, NODE_OBJECT_KVP is guaranteed to
        // have a NODE_VALUE child.
        TreeCursor nodeValue = cursor.getFirstChild(TreeType.NODE_VALUE);
        boolean isTextBlock = Optional.ofNullable(nodeValue.getFirstChild(TreeType.NODE_STRING_VALUE))
                .map(nodeString -> nodeString.getFirstChild(TreeType.TEXT_BLOCK))
                .isPresent();
        Doc nodeValueDoc = valueVisitor.apply(nodeValue);

        if (isTextBlock) {
            nodeValueDoc = nodeValueDoc.indent(4);
        }

        // Hoist awkward comments in the KVP *before* the KVP rather than between the values and colon.
        // If there is an awkward comment before the TRAIT value, hoist it above the statement.
        return keyVisitor.apply(cursor.getFirstChild(TreeType.NODE_OBJECT_KEY))
                .append(Doc.text(": "))
                .append(nodeValueDoc);
    }

    // Ensure that special key-value pairs of service and resource shapes are always on multiple lines if not empty.
    private final class EntityShapeExtractorVisitor implements Function<TreeCursor, Doc> {

        // Format known NODE_OBJECT_KVP list values to always place items on multiple lines.
        private final Function<TreeCursor, Doc> hardLineList = value -> {
            value = value.getFirstChild(TreeType.NODE_ARRAY);
            return new BracketFormatter()
                    .open(Formatter.LBRACKET)
                    .close(Formatter.RBRACKET)
                    .extractChildren(value,
                            BracketFormatter
                                    .extractByType(TreeType.NODE_VALUE, FormatVisitor.this::visit))
                    .forceLineBreaksIfNotEmpty()
                    .write();
        };

        // Format known NODE_OBJECT_KVP object values to always place them on multiple lines.
        private final Function<TreeCursor, Doc> hardLineObject = value -> {
            value = value.getFirstChild(TreeType.NODE_OBJECT);
            return new BracketFormatter()
                    .extractChildren(value,
                            BracketFormatter
                                    .extractByType(TreeType.NODE_OBJECT_KVP, FormatVisitor.this::visit))
                    .forceLineBreaksIfNotEmpty()
                    .write();
        };

        @Override
        public Doc apply(TreeCursor c) {
            if (c.getTree().getType() != TreeType.NODE_OBJECT_KVP) {
                return visit(c);
            }

            TreeCursor key = c.getFirstChild(TreeType.NODE_OBJECT_KEY);
            String keyValue = key.getTree().concatTokens();

            // Remove quotes if found.
            if (key.getTree().getType() == TreeType.QUOTED_TEXT) {
                keyValue = keyValue.substring(1, keyValue.length() - 1);
            }

            switch (keyValue) {
                case "resources":
                case "operations":
                case "collectionOperations":
                case "errors":
                    return formatNodeObjectKvp(c, FormatVisitor.this::visit, hardLineList);
                case "identifiers":
                case "properties":
                case "rename":
                    return formatNodeObjectKvp(c, FormatVisitor.this::visit, hardLineObject);
                default:
                    return visit(c);
            }
        }
    }
}
