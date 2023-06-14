/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions;

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * For code generating from a template, use a `TemplateVisitor`. Template visitor is written to enable optimized
 * behavior for static templates with no dynamic components.
 *
 * @param <T> The return type of this visitor
 */
@SmithyUnstableApi
public interface TemplateVisitor<T> {
    /**
     * The template contains a single static string, eg. {@literal "https://mystaticendpoing.com"}
     *
     * @param value The static value of the template.
     * @return T
     */
    T visitStaticTemplate(String value);

    /**
     * The template contains a single dynamic element, eg. `{Region}`. In this case, string formatting is not required.
     * The type of the value is guaranteed to be a string.
     *
     * @param value The single expression that represents this template.
     * @return T
     */
    T visitSingleDynamicTemplate(Expression value);

    /**
     * Visit a static element within a multipart template. This will only be called after
     * {@link #startMultipartTemplate()} has been invoked.
     *
     * @param value A static element within a larger template
     * @return T
     */
    T visitStaticElement(String value);

    /**
     * Visit a dynamic element within a multipart template. This will only be called after
     * {@link #startMultipartTemplate()} has been invoked.
     *
     * @param value The dynamic template value
     * @return T
     */
    T visitDynamicElement(Expression value);

    /**
     * Invoked prior to visiting a multipart template like `https://{Region}.{dnsSuffix}`. This function will
     * be followed by invocations of {@link #visitStaticTemplate(String)} and
     * {@link #visitDynamicElement(Expression)}.
     *
     * @return T
     */
    T startMultipartTemplate();

    /**
     * Invoked at the conclusion of visiting a multipart template like `https://{Region}.{dnsSuffix}`. This allows
     * implementations to do something like call `string.join()` or `stringbuilder.toString()`.
     *
     * @return T
     */
    T finishMultipartTemplate();
}
