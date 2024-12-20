/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.writers;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A {@code SymbolWriter} provides abstract methods that will be used during
 * documentation generation. This allows for other formats to be swapped out
 * without much difficulty.
 */
@SmithyUnstableApi
public abstract class DocWriter extends SymbolWriter<DocWriter, DocImportContainer> {
    private static final int MAX_HEADING_DEPTH = 6;

    /**
     * The full path to the file being written to by the writer.
     */
    protected final String filename;

    private int headingDepth = 0;

    /**
     * Constructor.
     *
     * @param importContainer The container to store any imports in.
     * @param filename The name of the file being written.
     */
    public DocWriter(DocImportContainer importContainer, String filename) {
        super(importContainer);
        this.filename = filename;
        putFormatter('R', (s, i) -> referenceFormatter(s));
        putFormatter('B', (s, i) -> boldFormatter(s));
        putFormatter('`', (s, i) -> inlineLiteralFormatter(s));
        trimTrailingSpaces();
    }

    /**
     * Formats the given reference object as a link if possible.
     *
     * <p>This given value can be expected to be one of the following types:
     *
     * <ul>
     *     <li>{@code Symbol}: The symbol's name is the link text and a combination of
     *     the definition file and {@link DocSymbolProvider#LINK_ID_PROPERTY}
     *     forms the actual link. If either the link id or definition file are not set,
     *     the formatter must return the symbol's name.
     *     <li>{@code SymbolReference}: The reference's alias is the link text and a
     *     combination of the referenced symbol's definition file and
     *     {@link DocSymbolProvider#LINK_ID_PROPERTY}
     *     forms the actual link. If either the link id or definition file are not set,
     *     the formatter should return the reference's alias.
     *     <li>{@code Pair<String, String>}: The key is the link text and the value is
     *     the link. Both key and value MUST be present.
     * </ul>
     *
     * @param value The value to format.
     * @return returns a string formatted to reference the given value.
     */
    abstract String referenceFormatter(Object value);

    /**
     * Formats the given object as a bold string.
     *
     * <p>For example, a raw HTML writer might surround the given text with {@code b} tags.
     *
     * @param value The value to format.
     * @return returns the value formatted as a bold string.
     */
    abstract String boldFormatter(Object value);

    /**
     * Formats the given object an inline literal.
     *
     * <p>This is the equivalent of surrounding text with backticks (`) in markdown.
     *
     * @param value The value to format.
     * @return returns the value formatted an inline literal.
     */
    abstract String inlineLiteralFormatter(Object value);

    /**
     * Writes out the content of the shape's
     * <a href="https://smithy.io/2.0/spec/documentation-traits.html#smithy-api-documentation-trait">
     * documentation trait</a>, if present.
     *
     * <p>Smithy's documentation trait is in the
     * <a href="https://spec.commonmark.org">CommonMark</a> format, so writers
     * for formats that aren't based on CommonMark will need to convert the value to
     * their format. This includes raw HTML, which CommonMark allows.
     *
     * <p>If the shape doesn't have a documentation trait, the writer MAY write out
     * default documentation.
     *
     * @param shape The shape whose documentation should be written.
     * @param model The model whose documentation is being written.
     * @return returns the writer.
     */
    public DocWriter writeShapeDocs(Shape shape, Model model) {
        var documentation = shape.getMemberTrait(model, DocumentationTrait.class)
                .map(StringTrait::getValue)
                .orElse("Placeholder documentation for `" + shape.getId() + "`");
        writeCommonMark(documentation.replace("$", "$$"));
        return this;
    }

    /**
     * Writes documentation based on a commonmark string.
     *
     * <p>Smithy's documentation trait is in the
     * <a href="https://spec.commonmark.org">CommonMark</a> format, so writers
     * for formats that aren't based on CommonMark will need to convert the value to
     * their format. This includes raw HTML, which CommonMark allows.
     *
     * @param commmonMark A string containing CommonMark-formatted documentation.
     * @return returns the writer.
     */
    public abstract DocWriter writeCommonMark(String commmonMark);

    /**
     * Writes a heading with the given content.
     *
     * <p>{@link #closeHeading} will be called to enable cleaning up any resources or
     * context this method creates.
     *
     * @param content A string to use as the heading content.
     * @return returns the writer.
     */
    public DocWriter openHeading(String content) {
        headingDepth++;
        if (headingDepth > MAX_HEADING_DEPTH) {
            throw new CodegenException(String.format(
                    "Tried opening a heading nested more deeply than the max depth of %d.",
                    MAX_HEADING_DEPTH));
        }
        return openHeading(content, headingDepth);
    }

    /**
     * Writes a heading with the given content and linkId.
     *
     * <p>{@link #closeHeading} will be called to enable cleaning up any resources or
     * context this method creates.
     *
     * @param content A string to use as the heading content.
     * @param linkId The identifier used to link to the heading.
     * @return returns the writer.
     */
    public DocWriter openHeading(String content, String linkId) {
        return writeAnchor(linkId).openHeading(content);
    }

    /**
     * Writes a heading of a given level with the given content.
     *
     * <p>{@link #closeHeading} will be called to enable cleaning up any resources or
     * context this method creates.
     *
     * @param content A string to use as the heading content.
     * @param level The level of the heading to open. This corresponds to HTML heading
     *              levels, and will only have values between 1 and 6.
     * @return returns the writer.
     */
    abstract DocWriter openHeading(String content, int level);

    /**
     * Closes the current heading, cleaning any context created for the current level,
     * then writes a blank line.
     *
     * @return returns the writer.
     */
    public DocWriter closeHeading() {
        headingDepth--;
        if (headingDepth < 0) {
            throw new CodegenException(
                    "Attempted to close a heading when at the base heading level.");
        }
        write("");
        return this;
    }

    /**
     * Writes any context needed to open a definition list.
     *
     * <p>A definition list is a list where each element has an emphasized title or
     * term. A basic way to represent this might be an unordered list where the term
     * is followed by a colon.
     *
     * <p>This will primarily be used to list members, with the element titles being
     * the member names, member types, and a link to those member types where
     * applicable. It will also be used for resource lifecycle operations, which will
     * have similar titles.
     *
     * @return returns the writer.
     */
    public abstract DocWriter openDefinitionList();

    /**
     * Writes any context needed to close a definition list.
     *
     * <p>A definition list is a list where each element has an emphasized title or
     * term. A basic way to represent this might be an unordered list where the term
     * is followed by a colon.
     *
     * <p>This will primarily be used to list members, with the element titles being
     * the member names, member types, and a link to those member types where
     * applicable. It will also be used for resource lifecycle operations, which will
     * have similar titles.
     *
     * @return returns the writer.
     */
    public abstract DocWriter closeDefinitionList();

    /**
     * Writes any context needed to open a definition list item.
     *
     * <p>A definition list is a list where each element has an emphasized title or
     * term. A basic way to represent this might be an unordered list where the term
     * is followed by a colon.
     *
     * <p>This will primarily be used to list members, with the element titles being
     * the member names, member types, and a link to those member types where
     * applicable. It will also be used for resource lifecycle operations, which will
     * have similar titles.
     *
     * @param titleWriter writes the title or term for the definition list item.
     * @return returns the writer.
     */
    public abstract DocWriter openDefinitionListItem(Consumer<DocWriter> titleWriter);

    /**
     * Writes any context needed to close a definition list item.
     *
     * <p>A definition list is a list where each element has an emphasized title or
     * term. A basic way to represent this might be an unordered list where the term
     * is followed by a colon.
     *
     * <p>This will primarily be used to list members, with the element titles being
     * the member names, member types, and a link to those member types where
     * applicable. It will also be used for resource lifecycle operations, which will
     * have similar titles.
     *
     * @return returns the writer.
     */
    public abstract DocWriter closeDefinitionListItem();

    /**
     * Writes a linkable element to the documentation with the given identifier.
     *
     * <p>The resulting HTML should be able to link to this anchor with {@code #linkId}.
     *
     * <p>For example, a direct HTML writer might create a {@code span} tag with
     * the given string as the tag's {@code id}, or modify the next emitted tag
     * to have the given id.
     *
     * @param linkId The anchor's link identifier.
     * @return returns the writer.
     */
    public abstract DocWriter writeAnchor(String linkId);

    /**
     * Writes any opening context needed to form a tab group.
     *
     * @return returns the writer.
     */
    public abstract DocWriter openTabGroup();

    /**
     * Writes any context needed to close a tab group.
     *
     * @return returns the writer.
     */
    public abstract DocWriter closeTabGroup();

    /**
     * Writes any context needed to open a tab.
     *
     * @param title The title text that is displayed on the tab itself.
     * @return returns the writer.
     */
    public abstract DocWriter openTab(String title);

    /**
     * Writes any context needed to close a tab.
     *
     * @return returns the writer.
     */
    public abstract DocWriter closeTab();

    /**
     * Writes any context needed to open a code block.
     *
     * <p>For example, a pure HTML writer might write an opening {@code pre} tag.
     *
     * @param language the language of the block's code.
     * @return returns the writer.
     */
    public abstract DocWriter openCodeBlock(String language);

    /**
     * Writes any context needed to close a code block.
     *
     * <p>For example, a pure HTML writer might write a closing {@code pre} tag.
     *
     * @return returns the writer.
     */
    public abstract DocWriter closeCodeBlock();

    /**
     * Writes any context needed to open a code block tab.
     *
     * @param title The title text that is displayed on the tab itself.
     * @param language the language of the tab's code.
     * @return returns the writer.
     */
    public DocWriter openCodeTab(String title, String language) {
        return openTab(title).openCodeBlock(language);
    }

    /**
     * Writes any context needed to close a code block tab.
     *
     * @return returns the writer.
     */
    public DocWriter closeCodeTab() {
        return closeCodeBlock().closeTab();
    }

    /**
     * Writes any context needed to open a list of the given type.
     *
     * <p>For example, a raw HTML writer might write an opening {@code ul} tag for
     * an unordered list or an {@code ol} tag for an ordered list.
     *
     * @param listType The type of list to open.
     * @return returns the writer.
     */
    public abstract DocWriter openList(ListType listType);

    /**
     * Writes any context needed to close a list of the given type.
     *
     * <p>For example, a raw HTML writer might write a closing {@code ul} tag for
     * an unordered list or an {@code ol} tag for an ordered list.
     *
     * @param listType The type of list to close.
     * @return returns the writer.
     */
    public abstract DocWriter closeList(ListType listType);

    /**
     * Writes any context needed to open a list item of the given type.
     *
     * <p>For example, a raw HTML writer might write an opening {@code li} tag for
     * a list of any type.
     *
     * @param listType The type of list the item is a part of.
     * @return returns the writer.
     */
    public abstract DocWriter openListItem(ListType listType);

    /**
     * Writes any context needed to close a list item of the given type.
     *
     * <p>For example, a raw HTML writer might write a closing {@code li} tag for
     * a list of any type.
     *
     * @param listType The type of list the item is a part of.
     * @return returns the writer.
     */
    public abstract DocWriter closeListItem(ListType listType);

    /**
     * Represents different types of lists.
     */
    public enum ListType {
        /**
         * A list whose elements are ordered with numbers.
         */
        ORDERED,

        /**
         * A list whose elements don't have associated numbers.
         */
        UNORDERED
    }

    /**
     * Opens an admonition with a custom title.
     *
     * <p>An admonition is an emphasized callout that typically have color-coded
     * severity. A warning admonition, for example, might have a yellow or red
     * banner that emphasizes the importance of the body text.
     *
     * @param type The type of admonition to open.
     * @param titleWriter A consumer that writes out the title.
     * @return returns the writer.
     */
    public abstract DocWriter openAdmonition(NoticeType type, Consumer<DocWriter> titleWriter);

    /**
     * Opens an admonition with a default title.
     *
     * <p>An admonition is an emphasized callout that typically have color-coded
     * severity. A warning admonition, for example, might have a yellow or red
     * banner that emphasizes the importance of the body text.
     *
     * @param type The type of admonition to open.
     * @return returns the writer.
     */
    public abstract DocWriter openAdmonition(NoticeType type);

    /**
     * Closes the body of an admonition.
     *
     * @return returns the writer.
     */
    public abstract DocWriter closeAdmonition();

    /**
     * Writes text as a badge.
     *
     * <p>Implementations SHOULD write inline.
     *
     * <p>A badge in this context means text enclosed in a color-coded rectangular
     * shape. The color should be based on the given type.
     *
     * @param type The notice type of the badge that determines styling.
     * @param text The text to put in the badge.
     * @return returns the writer.
     */
    public abstract DocWriter writeBadge(NoticeType type, String text);

    /**
     * The type of admonition.
     *
     * <p>This affects the default title of the admonition, as well as styling.
     */
    @SmithyUnstableApi
    public enum NoticeType {
        /**
         * An notice that adds context without any strong severity.
         */
        NOTE,

        /**
         * An notice that adds context which is important, but not severely so.
         */
        IMPORTANT,

        /**
         * A notice that adds context with strong severity.
         *
         * <p>This might be used by deprecation notices or required badges, for
         * example.
         */
        WARNING,

        /**
         * A notice that adds context with extreme severity.
         *
         * <p>This might be used to add information about security-related concerns,
         * such as sensitive shapes and members.
         */
        DANGER,

        /**
         * A notice that refers to external context or highlights information.
         */
        INFO
    }
}
