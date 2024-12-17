/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Provides {@link Symbol} objects for shapes.
 *
 * <p>Implementations of this interface are used to determine what file a
 * shape is defined within, what namespace/module/package a shape is defined
 * in, converts shapes to class/struct/interface names, converts shapes to
 * function/method names, converts shapes to member/property names, and
 * creates variable names from strings.
 *
 * <p>Method names MUST account for reserved words and the syntax constraints
 * of the target language. This typically means that implementations will
 * leverage one or more internal instances of {@link ReservedWords}.
 */
public interface SymbolProvider {
    /**
     * Gets the symbol to define for the given shape.
     *
     * <p>A "symbol" represents the qualified name of a type in a target
     * programming language.
     *
     * <ul>
     *     <li>When given a structure, union, resource, or service shape,
     *     this method should provide the namespace and name of the type to
     *     generate.</li>
     *     <li>When given a simple type like a string, number, or timestamp,
     *     this method should return the language-specific type of the
     *     shape.</li>
     *     <li>When given a member shape, this method should return the
     *     language specific type to use as the target of the member.</li>
     *     <li>When given a list, set, or map, this method should return the
     *     language specific type to use for the shape (e.g., a map shape for
     *     a Python code generator might return "dict".</li>
     * </ul>
     *
     * @param shape Shape to get the class name of.
     * @return Returns the generated class name.
     */
    Symbol toSymbol(Shape shape);

    /**
     * Converts a member shape to a member/property name of a containing
     * data structure.
     *
     * <p>The default implementation will return the member name of
     * the provided shape ID and should be overridden if necessary.
     *
     * @param shape Shape to convert.
     * @return Returns the converted member name.
     */
    default String toMemberName(MemberShape shape) {
        return shape.getMemberName();
    }

    /**
     * Decorates a {@code SymbolProvider} with a cache and returns the
     * decorated {@code SymbolProvider}.
     *
     * <p>The results of calling {@code toSymbol} and {@code toMemberName}
     * on {@code delegate} are cached using a thread-safe cache.
     *
     * <pre>
     * {@code
     * SymbolProvider delegate = createComplexProvider(myModel);
     * SymbolProvider cachingProvider = SymbolProvider.cache(delegate);
     * }
     * </pre>
     *
     * @param delegate Symbol provider to wrap and cache its results.
     * @return Returns the wrapped SymbolProvider.
     */
    static SymbolProvider cache(SymbolProvider delegate) {
        return new CachingSymbolProvider(delegate);
    }
}
