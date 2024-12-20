/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolContainer;
import software.amazon.smithy.codegen.core.SymbolDependency;
import software.amazon.smithy.codegen.core.SymbolDependencyContainer;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.utils.AbstractCodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * This class is deprecated and will be removed in a future release.
 *
 * <p>Use {@link software.amazon.smithy.codegen.core.SymbolWriter} instead.
 *
 * @param <T> The concrete type, used to provide a fluent interface.
 * @param <U> The import container used by the writer to manage imports.
 */
@SmithyUnstableApi
@Deprecated
public class SymbolWriter<T extends SymbolWriter<T, U>, U extends ImportContainer>
        extends AbstractCodeWriter<T> implements SymbolDependencyContainer {

    private static final Logger LOGGER = Logger.getLogger(SymbolWriter.class.getName());
    private static final String RELATIVIZE_SYMBOLS = "__CodegenWriterRelativizeSymbols";

    private final List<SymbolDependency> dependencies = new ArrayList<>();
    private final DocWriter<T> documentationWriter;
    private final U importContainer;

    /**
     * @param documentationWriter Writes out documentation emitted by a {@code Runnable}.
     * @param importContainer Container used to persist and filter imports based on package names.
     */
    public SymbolWriter(DocWriter<T> documentationWriter, U importContainer) {
        this.documentationWriter = documentationWriter;
        this.importContainer = importContainer;

        // Register T by default. This can be overridden as needed.
        putFormatter('T', new DefaultSymbolFormatter());
    }

    /**
     * The default implementation for formatting Symbols in CodegenWriter.
     */
    private final class DefaultSymbolFormatter implements BiFunction<Object, String, String> {
        @Override
        public String apply(Object type, String indent) {
            if (type instanceof Symbol) {
                Symbol typeSymbol = (Symbol) type;
                addUseImports(typeSymbol);
                String relativizeSymbols = getContext(RELATIVIZE_SYMBOLS, String.class);
                if (relativizeSymbols != null) {
                    return typeSymbol.relativize(relativizeSymbols);
                } else {
                    return typeSymbol.toString();
                }
            } else if (type instanceof SymbolReference) {
                SymbolReference typeSymbol = (SymbolReference) type;
                addImport(typeSymbol.getSymbol(), typeSymbol.getAlias(), SymbolReference.ContextOption.USE);
                return typeSymbol.getAlias();
            } else {
                throw new CodegenException("Invalid type provided to $T. Expected a Symbol or SymbolReference, "
                        + "but found `" + type + "`");
            }
        }
    }

    /**
     * Sets a string used to relativize Symbols formatted using the default {@code T}
     * implementation used by {@code CodegenWriter} in the <em>current state</em>.
     *
     * <p>In many programming languages, when referring to types in the same namespace as
     * the current scope of a CodegenWriter, the symbols written in that scope don't need
     * to be fully-qualified. They can just reference the unqualified type name. By setting
     * a value for {@code relativizeSymbols}, if the result of {@link Symbol#getNamespace()}
     * is equal to {@code relativizeSymbols}, then the unqualified name of the symbol is
     * written to the {@code CodegenWriter} when the default implementation of {@code T}
     * is written. Symbols that refer to types in other namespaces will write the fully
     * qualified type.
     *
     * <p><strong>Note:</strong> This method may have no effect if a programming language
     * does not use namespaces or concepts like namespaces or if {@code T} has been
     * overridden with another implementation.
     *
     * @param relativizeSymbols The package name, namespace, etc to relativize symbols with.
     * @return Returns the CodegenWriter.
     */
    @SuppressWarnings("unchecked")
    public T setRelativizeSymbols(String relativizeSymbols) {
        putContext(RELATIVIZE_SYMBOLS, relativizeSymbols);
        return (T) this;
    }

    /**
     * Gets the documentation writer.
     *
     * @return Returns the documentation writer.
     */
    public final DocWriter<T> getDocumentationWriter() {
        return documentationWriter;
    }

    /**
     * Gets the import container associated with the writer.
     *
     * <p>The {@link #toString()} method of the {@code CodegenWriter} should
     * be overridden so that it includes the import container's contents in
     * the output as appropriate.
     *
     * @return Returns the import container.
     */
    public final U getImportContainer() {
        return importContainer;
    }

    @Override
    public final List<SymbolDependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /**
     * Adds one or more dependencies to the generated code (represented as
     * a {@link SymbolDependency}).
     *
     * <p>Tracking dependencies on a {@code CodegenWriter} allows dependencies
     * to be automatically aggregated and collected in order to generate
     * configuration files for dependency management tools (e.g., npm,
     * maven, etc).
     *
     * @param dependencies Dependency to add.
     * @return Returns the writer.
     */
    @SuppressWarnings("unchecked")
    public final T addDependency(SymbolDependencyContainer dependencies) {
        List<SymbolDependency> values = dependencies.getDependencies();
        LOGGER.finest(() -> String.format("Adding dependencies from %s: %s", dependencies, values));
        this.dependencies.addAll(values);
        return (T) this;
    }

    /**
     * Imports one or more USE symbols using the name of the symbol
     * (e.g., {@link SymbolReference.ContextOption#USE} references).
     *
     * <p>USE references are only necessary when referring to a symbol, not
     * <em>declaring</em> the symbol. For example, when referring to a
     * {@code List<Foo>}, the USE references would be both the {@code List}
     * type and {@code Foo} type.
     *
     * <p>This method may be overridden as needed.
     *
     * @param container Symbols to add.
     * @return Returns the writer.
     */
    @SuppressWarnings("unchecked")
    public T addUseImports(SymbolContainer container) {
        for (Symbol symbol : container.getSymbols()) {
            addImport(symbol, symbol.getName(), SymbolReference.ContextOption.USE);
        }
        return (T) this;
    }

    /**
     * Imports a USE symbols possibly using an alias of the symbol
     * (e.g., {@link SymbolReference.ContextOption#USE} references).
     *
     * <p>This method may be overridden as needed.
     *
     * @param symbolReference Symbol reference to import.
     * @return Returns the writer.
     * @see #addUseImports(SymbolContainer)
     */
    public T addUseImports(SymbolReference symbolReference) {
        return addImport(symbolReference.getSymbol(), symbolReference.getAlias(), SymbolReference.ContextOption.USE);
    }

    /**
     * Imports a symbol (if necessary) using a specific alias and list of
     * context options.
     *
     * <p>This method automatically adds any dependencies of the {@code symbol}
     * to the writer, calls {@link ImportContainer#importSymbol}, and
     * automatically calls {@link #addImportReferences} for the provided
     * {@code symbol}.
     *
     * <p>When called with no {@code options}, both {@code USE} and
     * {@code DECLARE} symbols are imported from any references the
     * {@code Symbol} might contain.
     *
     * @param symbol Symbol to optionally import.
     * @param alias The alias to refer to the symbol by.
     * @param options The list of context options (e.g., is it a USE or DECLARE symbol).
     * @return Returns the writer.
     */
    @SuppressWarnings("unchecked")
    public final T addImport(Symbol symbol, String alias, SymbolReference.ContextOption... options) {
        LOGGER.finest(() -> String.format("Adding import %s as `%s` (%s)",
                symbol.getNamespace(),
                alias,
                Arrays.toString(options)));

        // Always add the dependencies of the symbol.
        dependencies.addAll(symbol.getDependencies());

        // Only add an import for the symbol if the symbol is external to the
        // current "namespace" (where "namespace" can mean whatever is need to
        // mean for each target language).
        importContainer.importSymbol(symbol, alias);

        // Even if the symbol is in the same namespace as the current namespace,
        // the symbol references of the given symbol always need to be imported
        // because the assumption is that the symbol is being USED or DECLARED
        // and is required ot refer to other symbols as part of the definition.
        addImportReferences(symbol, options);

        return (T) this;
    }

    /**
     * Adds any imports to the writer by getting all of the references from the
     * symbol that contain one or more of the given {@code options}.
     *
     * @param symbol Symbol to import the references of.
     * @param options The options that must appear on the reference.
     */
    final void addImportReferences(Symbol symbol, SymbolReference.ContextOption... options) {
        for (SymbolReference reference : symbol.getReferences()) {
            if (options.length == 0) {
                addImport(reference.getSymbol(), reference.getAlias(), options);
            } else {
                for (SymbolReference.ContextOption option : options) {
                    if (reference.hasOption(option)) {
                        addImport(reference.getSymbol(), reference.getAlias(), options);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Writes documentation comments.
     *
     * <p>This method is responsible for setting up the writer to begin
     * writing documentation comments. This includes writing any necessary
     * opening tokens (e.g., "/*"), adding tokens to the beginning of lines
     * (e.g., "*"), sanitizing documentation strings, and writing any
     * tokens necessary to close documentation comments (e.g., "*\/").
     *
     * <p>This method <em>does not</em> automatically escape the expression
     * start character ("$" by default). Write calls made by the Runnable
     * should either use {@link AbstractCodeWriter#writeWithNoFormatting} or escape
     * the expression start character manually.
     *
     * <p>This method may be overridden as needed.
     *
     * @param runnable Runnable that handles actually writing docs with the writer.
     * @return Returns the writer.
     */
    @SuppressWarnings("unchecked")
    public final T writeDocs(Runnable runnable) {
        pushState();
        documentationWriter.writeDocs((T) this, runnable);
        popState();
        return (T) this;
    }

    /**
     * Writes documentation comments from a string.
     *
     * @param docs Documentation to write.
     * @return Returns the writer.
     */
    @SuppressWarnings("unchecked")
    public final T writeDocs(String docs) {
        writeDocs(() -> writeWithNoFormatting(docs));
        return (T) this;
    }
}
