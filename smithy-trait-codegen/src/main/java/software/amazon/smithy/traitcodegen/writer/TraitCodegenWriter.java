/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.writer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Writes Java code for trait definitions.
 *
 * <p>This writer supports two custom formatters, a Java type formatter '$T' and
 * a Base type formatter '$B'.
 * <dl>
 *     <dt>{@link JavaTypeFormatter}|{@code 'T'}</dt>
 *     <dd>This formatter handles the formatting of
 *     Java types and also ensures that parameterized types (such as {@code List<String>} are
 *     written correctly.</dd>
 *     <dt>{@link BaseTypeFormatter}|{@code 'B'}</dt>
 *     <dd>This formatter allows you to use the base type
 *     for a trait. For example a String Trait may have a base type of {@code ShapeId}. To write
 *     this base type, use the {@code $B} formatter and provide the trait symbol. Note that
 *     if no base type is found (i.e. type is not a trait) then this formatter behaves exactly the
 *     same as the {@link JavaTypeFormatter}.</dd>
 *     <dt>{@link CapitalizingFormatter}|{@code 'U'}</dt>
 *     <dd>This formatter will capitalize the first letter of any string literal it is used to format.</dd>
 * </dl>
 */
public class TraitCodegenWriter extends SymbolWriter<TraitCodegenWriter, TraitCodegenImportContainer> {
    private static final int MAX_LINE_LENGTH = 120;
    private static final Pattern PATTERN = Pattern.compile("<([a-z]+)*>.*?</\\1>", Pattern.DOTALL);
    private final String namespace;
    private final String fileName;
    private final TraitCodegenSettings settings;
    private final Map<String, Set<Symbol>> symbolNames = new HashMap<>();

    public TraitCodegenWriter(
            String fileName,
            String namespace,
            TraitCodegenSettings settings
    ) {
        super(new TraitCodegenImportContainer(namespace));
        this.namespace = namespace;
        this.fileName = fileName;
        this.settings = settings;

        // Ensure extraneous white space is trimmed
        trimBlankLines();
        trimTrailingSpaces();

        putFormatter('T', new JavaTypeFormatter());
        putFormatter('B', new BaseTypeFormatter());
        putFormatter('U', new CapitalizingFormatter());
    }

    private void addImport(Symbol symbol) {
        addImport(symbol, symbol.getName());
    }

    /**
     * Writes the provided text in the format of a Java doc string.
     *
     * @param contents text to format as a doc string.
     */
    public void writeDocString(String contents) {
        writeWithNoFormatting("/**");

        // Split out any HTML-tag wrapped sections as we do not want to wrap
        // any customer documentation with tags
        Matcher matcher = PATTERN.matcher(contents);
        int lastMatchPos = 0;
        writeInlineWithNoFormatting(" * ");
        while (matcher.find()) {
            // write all contents up to the match.
            writeDocStringLine(contents.substring(lastMatchPos, matcher.start()));

            // write match contents
            writeInlineWithNoFormatting(contents.substring(matcher.start(), matcher.end()).replace("\n", "\n * "));
            lastMatchPos = matcher.end();
        }

        // Write out all remaining contents
        writeDocStringLine(contents.substring(lastMatchPos));
        writeWithNoFormatting("\n */");
    }

    private void writeDocStringLine(String string) {
        for (Scanner it = new Scanner(string); it.hasNextLine();) {
            String s = it.nextLine();
            writeInlineWithNoFormatting(StringUtils.wrap(s, MAX_LINE_LENGTH, getNewline() + " * ", false));
            if (it.hasNextLine()) {
                writeInlineWithNoFormatting(getNewline() + " * ");
            }
        }
    }

    @Override
    public String toString() {
        // Do not add code headers to META-INF files
        if (fileName.startsWith("META-INF")) {
            return super.toString();
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getHeader()).append(getNewline());
        builder.append(getPackageHeader()).append(getNewline());
        builder.append(getImportContainer().toString()).append(getNewline());

        // Handle duplicates that may need to use full name
        resolveNameContext();
        builder.append(format(super.toString()));

        return builder.toString();
    }

    private void resolveNameContext() {
        for (Map.Entry<String, Set<Symbol>> entry : symbolNames.entrySet()) {
            Set<Symbol> duplicates = entry.getValue();
            // If the duplicates list has more than one entry
            // then duplicates are present, and we need to de-dupe
            if (duplicates.size() > 1) {
                duplicates.forEach(dupe -> {
                    // If we are in the namespace of a Symbol, use its
                    // short name, otherwise use the full name
                    if (dupe.getNamespace().equals(namespace)) {
                        putContext(dupe.getFullName(), dupe.getName());
                    } else {
                        putContext(dupe.getFullName(), dupe.getFullName());
                    }
                });
            } else {
                Symbol symbol = duplicates.iterator().next();
                putContext(symbol.getFullName(), symbol.getName());
            }
        }
    }

    public String getPackageHeader() {
        return String.format("package %s;%n", namespace);
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
        writeInlineWithNoFormatting(getNewline());
    }

    public void override() {
        writeWithNoFormatting("@Override");
    }

    /**
     * A factory class to create {@link TraitCodegenWriter}s.
     */
    public static final class Factory implements SymbolWriter.Factory<TraitCodegenWriter> {

        private final TraitCodegenSettings settings;

        /**
         * @param settings The Trait codegen plugin settings.
         */
        public Factory(TraitCodegenSettings settings) {
            this.settings = settings;
        }

        @Override
        public TraitCodegenWriter apply(String filename, String namespace) {
            return new TraitCodegenWriter(filename, namespace, settings);
        }
    }

    /**
     * Implements a formatter for {@code $T} that formats Java types.
     */
    private final class JavaTypeFormatter implements BiFunction<Object, String, String> {
        @Override
        public String apply(Object type, String indent) {
            Symbol typeSymbol;
            if (type instanceof Symbol) {
                typeSymbol = (Symbol) type;
            } else if (type instanceof Class<?>) {
                typeSymbol = TraitCodegenUtils.fromClass((Class<?>) type);
            } else {
                throw new IllegalArgumentException("Invalid type provided for $T. Expected a Symbol or Class "
                        + "but found: `" + type + "`.");
            }

            if (typeSymbol.getReferences().isEmpty()) {
                return getPlaceholder(typeSymbol);
            }

            // Add type references as type references (ex. `List<InnerType>`)
            StringBuilder builder = new StringBuilder();
            builder.append(getPlaceholder(typeSymbol));
            builder.append("<");
            Iterator<SymbolReference> iterator = typeSymbol.getReferences().iterator();
            while (iterator.hasNext()) {
                String placeholder = getPlaceholder(iterator.next().getSymbol());
                builder.append(placeholder);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(">");
            return builder.toString();
        }

        private String getPlaceholder(Symbol symbol) {
            Symbol normalizedSymbol = symbol.toBuilder().references(ListUtils.of()).build();

            // Add symbol to import container
            addImport(normalizedSymbol);

            // Add symbol to symbol map, so we can handle potential type name conflicts
            Set<Symbol> nameSet = symbolNames.computeIfAbsent(normalizedSymbol.getName(), n -> new HashSet<>());
            nameSet.add(normalizedSymbol);

            // Return a placeholder value that will be filled when toString is called
            return format("$${$L:L}", normalizedSymbol.getFullName());
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
            Optional<Symbol> baseSymbolOptional = symbol.getProperty(SymbolProperties.BASE_SYMBOL);
            if (baseSymbolOptional.isPresent()) {
                return javaTypeFormatter.apply(baseSymbolOptional.get(), indent);
            }
            return javaTypeFormatter.apply(symbol, indent);
        }
    }

    /**
     * Implements a formatter for {@code $U} that capitalizes the first letter of a string literal.
     */
    private static final class CapitalizingFormatter implements BiFunction<Object, String, String> {
        @Override
        public String apply(Object type, String indent) {
            if (type instanceof String) {
                return StringUtils.capitalize((String) type);
            }
            throw new IllegalArgumentException(
                    "Invalid type provided for $U. Expected a String but found: `"
                            + type + "`.");
        }
    }

}
