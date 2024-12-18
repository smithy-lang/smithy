/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import java.net.URL;
import software.amazon.smithy.codegen.core.ReservedWords;
import software.amazon.smithy.codegen.core.ReservedWordsBuilder;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.utils.CaseUtils;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides utility methods for trait code generation.
 */
@SmithyInternalApi
public final class TraitCodegenUtils {
    public static final Symbol JAVA_STRING_SYMBOL = TraitCodegenUtils.fromClass(String.class);
    public static final URL RESERVED_WORDS_FILE = TraitCodegenUtils.class.getResource("reserved-words.txt");
    public static final ReservedWords SHAPE_ESCAPER = new ReservedWordsBuilder()
            .loadCaseInsensitiveWords(RESERVED_WORDS_FILE, word -> word + "Shape")
            .build();
    public static final ReservedWords MEMBER_ESCAPER = new ReservedWordsBuilder()
            .loadCaseInsensitiveWords(RESERVED_WORDS_FILE, word -> word + "Member")
            .build();

    private TraitCodegenUtils() {}

    /**
     * Gets a Smithy codegen {@link Symbol} for a Java class.
     *
     * @param clazz class to get symbol for.
     * @return Symbol representing the provided class.
     */
    public static Symbol fromClass(Class<?> clazz) {
        Symbol.Builder builder = Symbol.builder()
                .name(clazz.getSimpleName())
                .namespace(clazz.getCanonicalName().replace("." + clazz.getSimpleName(), ""), ".");

        if (clazz.isPrimitive()) {
            builder.putProperty(SymbolProperties.IS_PRIMITIVE, true);
        }

        return builder.build();
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
        if (baseName.contains("_")) {
            unescaped = CaseUtils.toPascalCase(shape.getId().getName());
        } else {
            unescaped = StringUtils.capitalize(baseName);
        }

        return SHAPE_ESCAPER.escape(unescaped);
    }

    /**
     * Gets the default class name to use for a given Smithy {@link Shape} that
     * defines a trait.
     *
     * @param shape Shape to get name for.
     * @return Default name.
     */
    public static String getDefaultTraitName(Shape shape) {
        String name = getDefaultName(shape);

        // If the trait class name doesn't already end with `Trait`,
        // use that. Otherwise, append the `Trait` suffix.
        if (name.endsWith("Trait")) {
            return name;
        }

        return name + "Trait";
    }

    /**
     * Checks if a symbol maps to a Java {@link String}.
     *
     * @param symbol Symbol to check.
     * @return Returns true if the symbol maps to a Java String.
     */
    public static boolean isJavaString(Symbol symbol) {
        Symbol baseSymbol = symbol.getProperty(SymbolProperties.BASE_SYMBOL).orElse(symbol);
        return JAVA_STRING_SYMBOL.getName().equals(baseSymbol.getName())
                && JAVA_STRING_SYMBOL.getNamespace().equals(baseSymbol.getNamespace());
    }

    /**
     * Checks if a symbol maps to a Java {@code List<String>}.
     *
     * @param shape shape to check if it resolves to a list of java strings
     * @param symbolProvider symbol provider to use for checking member type
     * @return Returns true if the symbol maps to a Java String List.
     */
    public static boolean isJavaStringList(Shape shape, SymbolProvider symbolProvider) {
        return shape.isListShape()
                && !shape.hasTrait(UniqueItemsTrait.class)
                && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(
                        shape.asListShape().get().getMember()));
    }

    /**
     * Maps a smithy namespace to a java package namespace.
     *
     * @param rootSmithyNamespace base smithy namespace in use for trait codegen trait discovery
     * @param shapeNamespace namespace of shape to map into package namespace.
     * @param packageNamespace Java package namespace for trait codegen.
     */
    public static String mapNamespace(
            String rootSmithyNamespace,
            String shapeNamespace,
            String packageNamespace
    ) {
        if (!shapeNamespace.startsWith(rootSmithyNamespace)) {
            throw new IllegalArgumentException("Cannot relativize non-nested namespaces "
                    + "Root: " + rootSmithyNamespace + " Nested: " + shapeNamespace + ".");
        }
        return shapeNamespace.replace(rootSmithyNamespace, packageNamespace);
    }

    /**
     * Determines if a given member represents a nullable type.
     *
     * @see <a href="https://smithy.io/2.0/spec/aggregate-types.html#structure-member-optionality">structure member optionality</a>
     *
     * @param shape member to check for nullability
     *
     * @return if the shape is a nullable type
     */
    public static boolean isNullableMember(MemberShape shape) {
        return !shape.isRequired() && !shape.hasNonNullDefault();
    }
}
