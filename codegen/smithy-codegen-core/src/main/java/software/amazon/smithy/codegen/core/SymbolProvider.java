/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.StringUtils;

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
     * Converts a shape to a member/property name of a containing
     * data structure.
     *
     * <p>The default implementation will return the member name of
     * the provided shape ID if the shape ID contains a member. If no
     * member is present, the name of the shape with the first letter
     * converted to lowercase is returned. The default implementation may
     * not work for all use cases and should be overridden as needed.
     *
     * @param shape Shape to convert.
     * @return Returns the converted member name.
     */
    default String toMemberName(Shape shape) {
        return shape.getId().getMember().orElseGet(() -> StringUtils.uncapitalize(shape.getId().getName()));
    }
}
