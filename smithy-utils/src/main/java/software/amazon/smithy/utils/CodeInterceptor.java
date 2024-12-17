/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.function.BiConsumer;

/**
 * A {@link CodeSection} interceptor for a specific type of {@code CodeSection}.
 *
 * <p>These interceptors are executed after a state is popped by
 * {@link AbstractCodeWriter}. Interceptors have an opportunity to
 * change the contents of the popped state and are expected to write to
 * the {@link AbstractCodeWriter} they are given when called.
 *
 * <p>Unless you need to intercept previously written content and change it,
 * it's best to implement the {@link Appender} or {@link Prepender} interfaces
 * since they take care of properly writing previously written content to
 * the section (for example, only writing if it's non-empty, and using
 * writeInlineWithNoFormatting to avoid unintentional interpolation).
 *
 * @param <S> Type of CodeSection to intercept.
 * @param <W> Type of CodeWriter to expect.
 */
public interface CodeInterceptor<S extends CodeSection, W extends AbstractCodeWriter<W>> {

    /**
     * Get the strongly typed {@link CodeSection} this interceptor is used
     * to intercept.
     *
     * @return The code section to intercept.
     */
    Class<S> sectionType();

    /**
     * Checks if the given section is filtered by this interceptor or not.
     *
     * <p>In some cases {@link #sectionType()} might allow filtering a wider
     * array of types than what is actually filtered by an interceptor. The
     * most common example of this is intercepting any type of CodeSection
     * and only filtering based on the result of {@link CodeSection#sectionName()}.
     *
     * <p>This method will return {@code true} by default, meaning that any
     * type of {@link CodeSection} that is an instance of the class returned
     * from {@link #sectionType()} will be intercepted.
     *
     * @param section Section to test if this interceptor is relevant.
     * @return Returns true if the section is intercepted or not.
     */
    default boolean isIntercepted(S section) {
        return true;
    }

    /**
     * Intercepts an {@link AbstractCodeWriter} section.
     *
     * @param writer       Writer used to write content. If no write calls are
     *                     made, any intercepted text is lost.
     * @param previousText The previous text that was written. This text
     *                     needs to be written again in order for it to
     *                     be kept in the section.
     * @param section      The strongly typed section value.
     */
    void write(W writer, String previousText, S section);

    /**
     * Provides a more concise way of creating anonymous {@link Appender}s.
     *
     * <p>This method does not support custom filters on matched CodeSections. That
     * functionality must be implemented by directly creating an Appender class.
     *
     * @param type The type of section to intercept.
     * @param appender A BiConsumer that takes the writer and section and is expected to make write calls.
     * @param <S> The type of section being intercepted.
     * @param <W> The type of writer to use.
     * @return Returns the created Appender.
     */
    static <S extends CodeSection, W extends AbstractCodeWriter<W>> CodeInterceptor<S, W> appender(
            Class<S> type,
            BiConsumer<W, S> appender
    ) {
        return new Appender<S, W>() {
            @Override
            public void append(W writer, S section) {
                appender.accept(writer, section);
            }

            @Override
            public Class<S> sectionType() {
                return type;
            }
        };
    }

    /**
     * A code section interceptor that adds text after the intercepted section.
     *
     * <p>The previously written text is written before {@link #append(AbstractCodeWriter, CodeSection)}
     * is called. The previously written text is only included if it's a non-empty string.
     *
     * @param <S> The type of section to intercept.
     * @param <W> The type of code writer.
     */
    interface Appender<S extends CodeSection, W extends AbstractCodeWriter<W>> extends CodeInterceptor<S, W> {
        @Override
        default void write(W writer, String previousText, S section) {
            if (!previousText.isEmpty()) {
                writer.writeInlineWithNoFormatting(previousText);
            }
            append(writer, section);
        }

        /**
         * Writes text after previous content in the section.
         *
         * @param writer The code writer to write to.
         * @param section The section being intercepted.
         */
        void append(W writer, S section);
    }

    /**
     * A code section interceptor that adds text before the intercepted section.
     *
     * <p>The previously written text is only included if it's a non-empty string.
     *
     * @param <S> The type of section to intercept.
     * @param <W> The type of code writer.
     */
    interface Prepender<S extends CodeSection, W extends AbstractCodeWriter<W>> extends CodeInterceptor<S, W> {
        @Override
        default void write(W writer, String previousText, S section) {
            prepend(writer, section);
            if (!previousText.isEmpty()) {
                writer.writeInlineWithNoFormatting(previousText);
            }
        }

        /**
         * Writes text before previously written content in the section.
         *
         * @param writer The code writer to write to.
         * @param section The section being intercepted.
         */
        void prepend(W writer, S section);
    }

    /**
     * Creates an interceptor that  works with any type of CodeSection and is filtered
     * only by the name of the section.
     *
     * @param sectionName The name of the section to intercept.
     * @param consumer A consumer to invoke for intercepted sections that accepts the writer and previous text.
     * @param <W> The type of code writer being used.
     * @return Returns the created interceptor.
     */
    static <W extends AbstractCodeWriter<W>> CodeInterceptor<CodeSection, W> forName(
            String sectionName,
            BiConsumer<W, String> consumer
    ) {
        return new CodeInterceptor<CodeSection, W>() {
            @Override
            public Class<CodeSection> sectionType() {
                return CodeSection.class;
            }

            @Override
            public boolean isIntercepted(CodeSection section) {
                return section.sectionName().equals(sectionName);
            }

            @Override
            public void write(W writer, String previousText, CodeSection section) {
                consumer.accept(writer, previousText);
            }
        };
    }
}
