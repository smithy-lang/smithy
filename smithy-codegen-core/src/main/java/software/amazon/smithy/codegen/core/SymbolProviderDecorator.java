/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;

public class SymbolProviderDecorator implements SymbolProvider {
    protected SymbolProvider provider;

    /**
     * Constructor for {@link SymbolProviderDecorator}.
     *
     * @param provider The {@link SymbolProvider} to be decorated.
     */
    public SymbolProviderDecorator(SymbolProvider provider) {
        this.provider = provider;
    }

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
    @Override
    public Symbol toSymbol(Shape shape) {
        Symbol symbol = provider.toSymbol(shape);
        return symbol;
    }

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
    @Override
    public String toMemberName(MemberShape shape) {
        return provider.toMemberName(shape);
    }

}
