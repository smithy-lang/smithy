/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.writers;

import java.util.Locale;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Writes documentation in <a href="https://spec.commonmark.org">CommonMark</a>-based
 * format for the <a href="https://www.sphinx-doc.org">Sphinx</a> doc build system.
 *
 * <p>The specific markdown parser being written for is
 * <a href="https://myst-parser.readthedocs.io/en/latest/index.html">MyST</a> with the
 * following <a href="https://myst-parser.readthedocs.io/en/latest/syntax/optional.html">
 * extensions</a> enabled: {@code linkify} and {@code colon_fence}
 */
@SmithyUnstableApi
public final class SphinxMarkdownWriter extends MarkdownWriter {

    private boolean isNewTabGroup = true;

    /**
     * Constructs a SphinxMarkdownWriter.
     *
     * @param filename The full path to the file being written to.
     */
    public SphinxMarkdownWriter(String filename) {
        super(filename);
    }

    /**
     * Factory to construct {@code SphinxMarkdownWriter}s.
     */
    public static final class Factory implements SymbolWriter.Factory<DocWriter> {
        @Override
        public DocWriter apply(String filename, String namespace) {
            return new SphinxMarkdownWriter(filename);
        }
    }

    @Override
    public DocWriter openDefinitionListItem(Consumer<DocWriter> titleWriter) {
        writeInline("""
                **$C**
                :\s""", titleWriter);
        indent();
        return this;
    }

    @Override
    public DocWriter closeDefinitionListItem() {
        dedent();
        return this;
    }

    @Override
    public DocWriter writeAnchor(String linkId) {
        write("($L)=", linkId);
        return this;
    }

    @Override
    public DocWriter openTabGroup() {
        isNewTabGroup = true;
        return this;
    }

    @Override
    public DocWriter closeTabGroup() {
        isNewTabGroup = true;
        return this;
    }

    @Override
    public DocWriter openTab(String title) {
        write(":::{tab} $L", title);
        if (isNewTabGroup) {
            // The inline tab plugin will automatically gather tabs into groups so long
            // as no other elements separate them, so to make sure we never accidentally
            // merge what should be two groups, we add this directive config to opening
            // tabs to ensure a new group gets created.
            write(":new-set:\n");
            isNewTabGroup = false;
        }
        return this;
    }

    @Override
    public DocWriter closeTab() {
        return write(":::");
    }

    @Override
    public DocWriter openAdmonition(NoticeType type, Consumer<DocWriter> titleWriter) {
        return write("""
                :::{admonition} $C
                :class: $L

                """, titleWriter, getAdmonitionName(type));
    }

    @Override
    public DocWriter openAdmonition(NoticeType type) {
        return write(":::{$L}", getAdmonitionName(type));
    }

    private String getAdmonitionName(NoticeType type) {
        if (type.equals(NoticeType.INFO)) {
            return "seealso";
        }
        return type.toString().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public DocWriter closeAdmonition() {
        write(":::");
        return this;
    }

    @Override
    public DocWriter writeBadge(NoticeType type, String text) {
        switch (type) {
            case NOTE -> writeInline("{bdg-primary}");
            case IMPORTANT -> writeInline("{bdg-success}");
            case WARNING -> writeInline("{bdg-warning}");
            case DANGER -> writeInline("{bdg-danger}");
            case INFO -> writeInline("{bdg-info}");
            default -> writeInline("{bdg}");
        }
        return writeInline("`$L`", text);
    }
}
