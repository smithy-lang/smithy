/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.writers;

import static software.amazon.smithy.docgen.DocgenUtils.getSymbolLink;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Writes documentation in <a href="https://spec.commonmark.org">CommonMark</a> format.
 */
@SmithyUnstableApi
public class MarkdownWriter extends DocWriter {

    /**
     * Constructs a MarkdownWriter.
     *
     * @param importContainer this file's import container.
     * @param filename The full path to the file being written to.
     */
    public MarkdownWriter(DocImportContainer importContainer, String filename) {
        super(importContainer, filename);
    }

    /**
     * Constructs a MarkdownWriter.
     *
     * @param filename The full path to the file being written to.
     */
    public MarkdownWriter(String filename) {
        this(new DocImportContainer(), filename);
    }

    /**
     * Factory to construct {@code MarkdownWriter}s.
     */
    public static final class Factory implements SymbolWriter.Factory<DocWriter> {
        @Override
        public DocWriter apply(String filename, String namespace) {
            return new MarkdownWriter(filename);
        }
    }

    @Override
    String referenceFormatter(Object value) {
        var reference = getReferencePair(value);
        if (reference.getRight().isPresent()) {
            return String.format("[%s](%s)", reference.getLeft(), reference.getRight().get());
        } else {
            return reference.getLeft();
        }
    }

    @Override
    String boldFormatter(Object value) {
        return String.format("**%s**", formatLiteral(value).replace("*", "\\*"));
    }

    @Override
    String inlineLiteralFormatter(Object value) {
        return String.format("`%s`", formatLiteral(value).replace("`", "\\`"));
    }

    private Pair<String, Optional<String>> getReferencePair(Object value) {
        String text;
        Optional<String> ref;
        var relativeTo = Paths.get(filename);
        if (value instanceof Optional<?> optional && optional.isPresent()) {
            return getReferencePair(optional.get());
        } else if (value instanceof Symbol symbolValue) {
            text = symbolValue.getName();
            ref = getSymbolLink(symbolValue, relativeTo);
        } else if (value instanceof SymbolReference referenceValue) {
            text = referenceValue.getAlias();
            ref = getSymbolLink(referenceValue.getSymbol(), relativeTo);
        } else if (value instanceof Pair pairValue) {
            if (pairValue.getLeft() instanceof String left && pairValue.getRight() instanceof String right) {
                text = left;
                ref = Optional.of(right);
            } else {
                throw new CodegenException(
                        "Invalid type provided to $R. Expected both key and vale of the Pair to be Strings, but "
                                + "found " + value.getClass());
            }
        } else {
            throw new CodegenException(
                    "Invalid type provided to $R. Expected a Symbol, SymbolReference, or Pair<String, String>, but "
                            + "found " + value.getClass());
        }
        return Pair.of(text, ref);
    }

    @Override
    public DocWriter writeCommonMark(String commonMark) {
        return writeWithNewline(commonMark);
    }

    private DocWriter writeWithNewline(Object content, Object... args) {
        write(content, args);
        write("");
        return this;
    }

    @Override
    public DocWriter openHeading(String content, int level) {
        writeWithNewline(StringUtils.repeat("#", level) + " " + content);
        return this;
    }

    @Override
    public DocWriter openDefinitionList() {
        return this;
    }

    @Override
    public DocWriter closeDefinitionList() {
        return this;
    }

    @Override
    public DocWriter openDefinitionListItem(Consumer<DocWriter> titleWriter) {
        openListItem(ListType.UNORDERED);
        writeInline("**$C** - ", titleWriter);
        return this;
    }

    @Override
    public DocWriter closeDefinitionListItem() {
        closeListItem(ListType.UNORDERED);
        return this;
    }

    @Override
    public DocWriter writeAnchor(String linkId) {
        // Anchors have no meaning in base markdown
        return this;
    }

    @Override
    public DocWriter openTabGroup() {
        return this;
    }

    @Override
    public DocWriter closeTabGroup() {
        return this;
    }

    @Override
    public DocWriter openTab(String title) {
        return write("- $L", title).indent();
    }

    @Override
    public DocWriter closeTab() {
        return dedent();
    }

    @Override
    public DocWriter openCodeBlock(String language) {
        return write("```$L", language);
    }

    @Override
    public DocWriter closeCodeBlock() {
        return write("```");
    }

    @Override
    public DocWriter openList(ListType listType) {
        return this;
    }

    @Override
    public DocWriter closeList(ListType listType) {
        return this;
    }

    @Override
    public DocWriter openListItem(ListType listType) {
        if (listType == ListType.ORDERED) {
            // We don't actually need to keep track of how far we are in the list because
            // commonmark will render the list correctly so long as there is any number
            // in front of the period.
            writeInline("1. ");
        } else {
            writeInline("- ");
        }
        return indent();
    }

    @Override
    public DocWriter closeListItem(ListType listType) {
        return dedent();
    }

    @Override
    public String toString() {
        // Ensure there's exactly one trailing newline
        return super.toString().stripTrailing() + "\n";
    }

    @Override
    public DocWriter openAdmonition(NoticeType type, Consumer<DocWriter> titleWriter) {
        return writeInline("**$C:** ", titleWriter);
    }

    @Override
    public DocWriter openAdmonition(NoticeType type) {
        return writeInline("**$L:** ", getAdmonitionName(type));
    }

    private String getAdmonitionName(NoticeType type) {
        if (type.equals(NoticeType.INFO)) {
            return "See Also";
        }
        return type.toString();
    }

    @Override
    public DocWriter closeAdmonition() {
        return this;
    }

    @Override
    public DocWriter writeBadge(NoticeType type, String text) {
        return writeInline("`$L`", text);
    }
}
