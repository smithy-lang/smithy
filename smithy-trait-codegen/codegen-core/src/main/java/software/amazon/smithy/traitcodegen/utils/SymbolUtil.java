/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.utils;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.StringUtils;

public interface SymbolUtil {
    Symbol JAVA_STRING_SYMBOL = SymbolUtil.fromClass(String.class);

    /**
     * Gets a Smithy codegen {@link Symbol} for a Java class.
     *
     * @param clazz class to get symbol for.
     * @return Symbol representing the provided class.
     */
    static Symbol fromClass(Class<?> clazz) {
        return Symbol.builder()
                .name(clazz.getSimpleName())
                .namespace(clazz.getCanonicalName().replace("." + clazz.getSimpleName(), ""), ".")
                .build();
    }

    /**
     * Gets the default class name to use for a given Smithy {@link Shape}.
     *
     * @param shape Shape to get name for.
     * @return Default name.
     */
    static String getDefaultName(Shape shape) {
        return StringUtils.capitalize(shape.getId().getName());
    }

    /**
     * Gets the default name of a Shape that defines a trait.
     *
     * @param shape Shape to get name for.
     * @return Default name.
     */
    static String getDefaultTraitName(Shape shape) {
        return getDefaultName(shape) + "Trait";
    }

    /**
     * Checks if a symbol maps to a Java {@link String}.
     *
     * @param symbol Symbol to check.
     * @return Returns true if the symbol maps to a Java String.
     */
    static boolean isJavaString(Symbol symbol) {
        return JAVA_STRING_SYMBOL.getName().equals(symbol.getName())
                && JAVA_STRING_SYMBOL.getNamespace().equals(symbol.getNamespace());
    }
}
