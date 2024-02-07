/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.writer;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.utils.StringUtils;


/**
 * Writes Java code for trait definitions.
 *
 * <p>This writter supports two custom formatters, a Java type formatter '$T' and
 * a Base type formatter '$B'.
 * <ul>
 *     <li>{@link JavaTypeFormatter}|{@code 'T'}: This formatter handles the formatting of
 *     Java types and also ensures that parameterized types (such as {@code List<String>} are
 *     written correctly.
 *
 *     <li>{@link BaseTypeFormatter}|{@code 'B'}: This formatter allows you to use the base type
 *     for a trait. For example a String Trait may have a base type of {@code ShapeId}. To write
 *     this base type simply use the {@code $B} formatter and provide the trait symbol. Note that
 *     if no base type is found (i.e. type is not a trait) then this formatter behaves exactly the
 *     same as the {@link JavaTypeFormatter}.
 * </ul>
 */
public class TraitCodegenWriter extends SymbolWriter<TraitCodegenWriter, TraitCodegenImportContainer> {
    private static final int MAX_LINE_LENGTH = 120;

    private final String packageName;
    private final String fileName;
    private final TraitCodegenSettings settings;

    public TraitCodegenWriter(String fileName, String packageName, TraitCodegenSettings settings) {
        super(new TraitCodegenImportContainer());
        this.packageName = packageName;
        this.fileName = fileName;
        this.settings = settings;

        putFormatter('T', new JavaTypeFormatter());
        putFormatter('B', new BaseTypeFormatter());
    }

    private static boolean fileMatchesSymbol(String file, Symbol symbol) {
        String fullClassName = StringUtils.strip(file, ".java").replace("/", ".");
        return fullClassName.equals(symbol.getNamespace() + "." + symbol.getName());
    }

    public void addImport(Symbol symbol) {
        // Do not add import if the symbol is the same class as this file
        if (!fileMatchesSymbol(fileName, symbol)) {
            addImport(symbol, symbol.getName());
        }
    }

    public void addImport(Class<?> clazz) {
        addImport(TraitCodegenUtils.fromClass(clazz));
    }

    public void addImports(Class<?>... clazzes) {
        for (Class<?> clazz : clazzes) {
            addImport(TraitCodegenUtils.fromClass(clazz));
        }
    }

    public void openDocstring() {
        pushState().write("/**");
    }

    public void writeDocStringContents(String contents, Object... args) {
        writeInline(" * ");
        write(StringUtils.wrap(contents.replace("\n", "\n * "), MAX_LINE_LENGTH - 8,
                getNewline() + " * ", false), args);
    }

    public void closeDocstring() {
        write(" */").popState();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        // Do not add code headers to META-INF files
        if (!fileName.startsWith("META-INF")) {
            builder.append(getHeader()).append(getNewline());
            builder.append(getPackageHeader()).append(getNewline());
            builder.append(getImportContainer().toString()).append(getNewline());
        }
        builder.append(super.toString());
        return builder.toString();
    }

    public String getPackageHeader() {
        return String.format("package %s;%n", packageName);
    }

    public String getHeader() {
        StringBuilder builder = new StringBuilder().append("/**").append(getNewline());
        for (String line : settings.headerLines()) {
            builder.append(" * ").append(line).append(getNewline());
        }
        builder.append(" */").append(getNewline());
        return builder.toString();
    }

    public void newLine() {
        writeInline(getNewline());
    }

    public void override() {
        write("@Override");
    }

    /**
     * Implements a formatter for {@code $T} that formats Java types.
     */
    private final class JavaTypeFormatter implements BiFunction<Object, String, String> {
        @Override
        public String apply(Object type, String indent) {
            if (!(type instanceof Symbol)) {
                throw new IllegalArgumentException("Invalid type provided for $T. Expected a Symbol but found: `"
                        + type + "`.");
            }
            Symbol typeSymbol = (Symbol) type;
            addImport(typeSymbol);
            if (typeSymbol.getReferences().isEmpty()) {
                return typeSymbol.getName();
            }
            StringBuilder builder = new StringBuilder();
            builder.append(typeSymbol.getName());
            builder.append("<");
            Iterator<SymbolReference> iterator = typeSymbol.getReferences().iterator();
            while (iterator.hasNext()) {
                Symbol refSymbol = iterator.next().getSymbol();
                addImport(refSymbol);
                builder.append(refSymbol.getName());
                if (iterator.hasNext()) {
                    builder.append(",");
                }
            }
            builder.append(">");
            return builder.toString();
        }
    }

    /**
     * Implements a formatter for {@code $B} that formats the base Java type for a Trait Symbol.
     */
    private final class BaseTypeFormatter implements BiFunction<Object, String, String> {
        private final JavaTypeFormatter javaTypeFormatter = new JavaTypeFormatter();

        @Override
        public String apply(Object type, String indent) {
            if (!(type instanceof Symbol)) {
                throw new IllegalArgumentException("Invalid type provided for $T. Expected a Symbol but found: `"
                        + type + "`.");
            }
            Symbol symbol = (Symbol) type;
            Optional<Symbol> baseSymbolOptional = symbol.getProperty(SymbolProperties.BASE_SYMBOL, Symbol.class);
            if (baseSymbolOptional.isPresent()) {
                return javaTypeFormatter.apply(baseSymbolOptional.get(), indent);
            }
            return javaTypeFormatter.apply(symbol, indent);
        }
    }
}
