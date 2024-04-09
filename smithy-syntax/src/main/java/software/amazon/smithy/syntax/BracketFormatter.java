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
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Formats various kinds of brackets, dealing with interspersed whitespace and comments.
 */
final class BracketFormatter {

    private Doc open = Formatter.LBRACE;
    private Doc close = Formatter.RBRACE;
    private Collection<Doc> children;
    private boolean forceLineBreaks;
    private boolean forceInline = false;

    static Function<TreeCursor, Stream<Doc>> extractor(
            Function<TreeCursor, Doc> visitor,
            Function<TreeCursor, Stream<TreeCursor>> mapper
    ) {
        return new Extractor(visitor, mapper);
    }

    // Brackets children of childType between open and closed brackets. If the children can fit together
    // on a single line, they are comma separated. If not, they are split onto multiple lines with no commas.
    static Function<TreeCursor, Stream<Doc>> extractByType(
            TreeType childType,
            Function<TreeCursor, Doc> visitor
    ) {
        return extractor(visitor, child -> child.getTree().getType() == childType
                                           ? Stream.of(child)
                                           : Stream.empty());
    }

    BracketFormatter open(Doc open) {
        this.open = open;
        return this;
    }

    BracketFormatter close(Doc close) {
        this.close = close;
        return this;
    }

    BracketFormatter children(Stream<Doc> children) {
        this.children = children.collect(Collectors.toList());
        return this;
    }

    BracketFormatter extractChildren(TreeCursor cursor, Function<TreeCursor, Stream<Doc>> extractor) {
        return children(extractor.apply(cursor));
    }

    BracketFormatter detectHardLines(TreeCursor hardLineSubject) {
        forceLineBreaks = hasHardLine(hardLineSubject);
        return this;
    }

    // Check if the given tree has any hard lines. Nested arrays and objects are always considered hard lines.
    private boolean hasHardLine(TreeCursor cursor) {
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

    BracketFormatter forceLineBreaks() {
        forceLineBreaks = true;
        return this;
    }

    BracketFormatter forceLineBreaksIfNotEmpty() {
        if (!children.isEmpty()) {
            forceLineBreaks = true;
        }
        return this;
    }

    // Don't force line breaks and don't indent inside brackets
    BracketFormatter forceInline() {
        forceInline = true;
        forceLineBreaks = false;
        return this;
    }

    Doc write() {
        SmithyBuilder.requiredState("open", open);
        SmithyBuilder.requiredState("close", close);
        SmithyBuilder.requiredState("children", children);
        if (forceLineBreaks) {
            return FormatVisitor.renderBlock(open, close, Doc.intersperse(Doc.line(), children));
        }

        int indent = forceInline ? 0 : 4;
        Doc lineBreakDoc = getBracketLineBreakDoc();
        return Doc.intersperse(Formatter.LINE_OR_COMMA, children).bracket(indent, lineBreakDoc, open, close);
    }

    private Doc getBracketLineBreakDoc() {
        Doc lineDoc;
        if (!children.isEmpty() && open == Formatter.LBRACE) {
            // Make flattened objects have space padding inside the braces
            lineDoc = Doc.lineOrSpace();
        } else if (children.size() > 1) {
            // Multiple children may be separated on to different lines
            lineDoc = Doc.lineOrEmpty();
        } else {
            // Only one child or less doesn't need line separation
            lineDoc = Doc.empty();
        }
        return lineDoc;
    }

    private static final class Extractor implements Function<TreeCursor, Stream<Doc>> {
        private final Function<TreeCursor, Stream<TreeCursor>> mapper;
        private final Function<TreeCursor, Doc> visitor;

        private Extractor(
                Function<TreeCursor, Doc> visitor,
                Function<TreeCursor, Stream<TreeCursor>> mapper
        ) {
            this.visitor = visitor;
            this.mapper = mapper;
        }

        @Override
        public Stream<Doc> apply(TreeCursor cursor) {
            SmithyBuilder.requiredState("childExtractor", mapper);
            SmithyBuilder.requiredState("visitor", visitor);
            return cursor.children()
                    .flatMap(c -> {
                        TreeType type = c.getTree().getType();
                        return type == TreeType.WS || type == TreeType.COMMENT ? Stream.of(c) : mapper.apply(c);
                    })
                    .flatMap(c -> {
                        // If the child extracts WS, then filter it down to just comments.
                        return c.getTree().getType() == TreeType.WS
                               ? c.getChildrenByType(TreeType.COMMENT).stream()
                               : Stream.of(c);
                    })
                    .map(visitor)
                    .filter(doc -> doc != Doc.empty());
        }
    }
}
