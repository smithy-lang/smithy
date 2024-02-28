/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.net.URL;
import java.util.List;
import software.amazon.smithy.codegen.core.ReservedWords;
import software.amazon.smithy.codegen.core.ReservedWordsBuilder;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.CaseUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides various utility methods for trait code generation.
 */
@SmithyInternalApi
public final class TraitCodegenUtils {
    public static final Symbol JAVA_STRING_SYMBOL = TraitCodegenUtils.fromClass(String.class);
    private static final URL RESERVED_WORDS_FILE = TraitCodegenUtils.class.getResource("reserved-words.txt");
    private static final String SHAPE_ESCAPE_SUFFIX = "Shape";
    private static final String MEMBER_ESCAPE_SUFFIX = "Member";
    public static final ReservedWords SHAPE_ESCAPER = new ReservedWordsBuilder()
            .loadCaseInsensitiveWords(RESERVED_WORDS_FILE, word -> word + SHAPE_ESCAPE_SUFFIX)
            .build();
    public static final ReservedWords MEMBER_ESCAPER = new ReservedWordsBuilder()
            .loadCaseInsensitiveWords(RESERVED_WORDS_FILE, word -> word + MEMBER_ESCAPE_SUFFIX)
            .build();
    private static final List<String> DELIMITERS = ListUtils.of("_", " ", "-");

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
        String baseName = shape.getId().getName();

        // If the name contains any problematic delimiters, use PascalCase converter,
        // otherwise, just capitalize first letter to avoid messing with user-defined
        // capitalization.
        String unescaped;
        if (DELIMITERS.stream().anyMatch(baseName::contains)) {
            unescaped = CaseUtils.toPascalCase(shape.getId().getName());
        } else {
            unescaped = StringUtils.capitalize(baseName);
        }

        // No need to escape trait name (they already have 'Trait' appended)
        if (shape.hasTrait(TraitDefinition.class)) {
            // If the trait class name doesn't already end with `Trait`,
            // use that. Otherwise, append the `Trait` suffix.
            if (unescaped.endsWith("Trait")) {
                return unescaped;
            }
            return unescaped + "Trait";
        }

        return SHAPE_ESCAPER.escape(unescaped);
    }

    /**
     * Checks if a symbol maps to a Java {@link String}.
     *
     * @param symbol Symbol to check.
     * @return Returns true if the symbol maps to a Java String.
     */
    public static boolean isJavaString(Symbol symbol) {
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
