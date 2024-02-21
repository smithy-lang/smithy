/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides various utility methods for trait code generation.
 */
@SmithyInternalApi
public final class TraitCodegenUtils {
    public static final Symbol JAVA_STRING_SYMBOL = TraitCodegenUtils.fromClass(String.class);

    private TraitCodegenUtils() {}

    /**
     * Gets a Smithy codegen {@link Symbol} for a Java class.
     *
     * @param clazz class to get symbol for.
     * @return Symbol representing the provided class.
     */
    public static Symbol fromClass(Class<?> clazz) {
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
    public static String getDefaultName(Shape shape) {
        return StringUtils.capitalize(shape.getId().getName());
    }

    /**
     * Gets the default name of a Shape that defines a trait.
     *
     * @param shape Shape to get name for.
     * @return Default name.
     */
    public static String getDefaultTraitName(Shape shape) {
        return getDefaultName(shape) + "Trait";
    }

    /**
     * Checks if a symbol maps to a Java {@link String}.
     *
     * @param symbol Symbol to check.
     * @return Returns true if the symbol maps to a Java String.
     */
    public static boolean isJavaString(final Symbol symbol) {
        Symbol baseSymbol = symbol.getProperty(SymbolProperties.BASE_SYMBOL, Symbol.class)
                .orElse(symbol);
        return JAVA_STRING_SYMBOL.getName().equals(baseSymbol.getName())
                && JAVA_STRING_SYMBOL.getNamespace().equals(baseSymbol.getNamespace());
    }

    /**
     * Maps a smithy namespace to a java package namespace.
     *
     * @param rootSmithyNamespace base smithy namespace in use for trait codegen trait discovery
     * @param shapeNamespace namespace of shape to map into package namespace.
     * @param packageNamespace Java package namespace for trait codegen.
     */
    public static String mapNamespace(String rootSmithyNamespace,
                                      String shapeNamespace,
                                      String packageNamespace
    ) {
        if (!shapeNamespace.startsWith(rootSmithyNamespace)) {
            throw new IllegalArgumentException("Cannot relativize non-nested namespaces "
                    + "Root: " + rootSmithyNamespace + " Nested: " + shapeNamespace + ".");
        }
        return shapeNamespace.replace(rootSmithyNamespace, packageNamespace);
    }
}
