/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import software.amazon.smithy.utils.AbstractCodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A {@code SymbolWriter} is a specialized {@link AbstractCodeWriter} that makes it
 * easier to implement code generation that utilizes {@link Symbol}s and
 * {@link SymbolDependency} values.
 *
 * <p>A {@code SymbolWriter} is expected to be subclassed, and the
 * subclass is expected to implement language-specific functionality
 * like writing documentation comments, tracking "imports", and adding
 * any other kinds of helpful functionality for generating source code
 * for a programming language.
 *
 * <p>The following example shows how a subclass of {@code SymbolWriter}
 * should be created. SymbolWriters are expected to define a recursive
 * type signature (notice that {@code MyWriter} is a generic parametric
 * type in its own type definition).
 *
 * <pre>{@code
 * public final class MyWriter extends SymbolWriter<MyWriter, MyImportContainer> {
 *     public MyWriter(String namespace) {
 *         super(new MyImportContainer(namespace));
 *     }
 *
 *     \@Override
 *     public String toString() {
 *         return getImportContainer().toString() + "\n\n" + super.toString();
 *     }
 *
 *     public MyWriter someCustomMethod() {
 *         // You can implement custom methods that are specific to whatever
 *         // language you're implementing a generator for.
 *         return this;
 *     }
 * }
 * }</pre>
 *
 *
 * <h2>Formatting symbols with "T"</h2>
 *
 * <p>{@code SymbolWriter} registers a default formatter for "T" that writes
 * {@link Symbol}s and {@link SymbolReference}s. Imports needed by these types
 * are automatically registered with {@link #addUseImports} for a {@code Symbol}
 * and {@link #addImport} for a {@code SymbolReference}. Programming languages
 * that have a concept of namespaces can use {@link #setRelativizeSymbols} to
 * the namespace of the SymbolWriter, and then the default symbol formatter
 * will relativize symbols against that namespace using {@link Symbol#relativize}
 * when writing the symbol as a string.
 *
 * @param <W> The concrete type, used to provide a fluent interface.
 * @param <I> The import container used by the writer to manage imports.
 */
@SmithyUnstableApi
public abstract class SymbolWriter<W extends SymbolWriter<W, I>, I extends ImportContainer>
        extends AbstractCodeWriter<W> implements SymbolDependencyContainer {

    private static final Logger LOGGER = Logger.getLogger(SymbolWriter.class.getName());
    private static final String RELATIVIZE_SYMBOLS = "__RelativizeSymbols";

    private final List<SymbolDependency> dependencies = new ArrayList<>();
    private final I importContainer;

    /**
     * Factory used to create a {@code SymbolWriter}.
     *
     * <p>The following example shows how to implement a basic {@code Factory}.
     *
     * <pre>{@code
     * public final class MyWriterFactory implements SymbolWriter.Factory<MyWriter> {
     *     \@Override
     *     public MyWriter apply(String filename, String namespace) {
     *         return new MyWriter(namespace);
     *     }
     * }
     * }</pre>
     *
     * <p>Because this class is a {@link FunctionalInterface}, it can be implemented
     * using a lambda expression:
     *
     * <pre>{@code
     * SymbolWriter.Factory<MyWriter> = (filename, namespace) -> new MyWriter(namespace);
     * }</pre>
     *
     * @param <W> Type of {@code SymbolWriter} to create.
     */
    @FunctionalInterface
    public interface Factory<W extends SymbolWriter<W, ? extends ImportContainer>>
            extends BiFunction<String, String, W> {
        /**
         * Creates a {@code SymbolWriter} of type {@code W} for the given
         * filename and namespace.
         *
         * @param filename  Non-null filename of the writer being created.
         * @param namespace Non-null namespace associated with the file (possibly empty string).
         * @return Returns the created writer of type {@code W}.
         */
        W apply(String filename, String namespace);
    }

    /**
     * @param importContainer Container used to persist and filter imports based on package names.
     */
    public SymbolWriter(I importContainer) {
        this.importContainer = importContainer;

        // Register T by default. This can be overridden as needed.
        putFormatter('T', new DefaultSymbolFormatter());
    }

    /**
     * The default implementation for formatting Symbols in SymbolWriter.
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
     * Sets a string used to relativize Symbols formatted using the default {@code W}
     * implementation used by {@code SymbolWriter} in the <em>current state</em>.
     *
     * <p>In many programming languages, when referring to types in the same namespace as
     * the current scope of a SymbolWriter, the symbols written in that scope don't need
     * to be fully-qualified. They can just reference the unqualified type name. By setting
     * a value for {@code relativizeSymbols}, if the result of {@link Symbol#getNamespace()}
     * is equal to {@code relativizeSymbols}, then the unqualified name of the symbol is
     * written to the {@code SymbolWriter} when the default implementation of {@code W}
     * is written. Symbols that refer to types in other namespaces will write the fully
     * qualified type.
     *
     * <p><strong>Note:</strong> This method may have no effect if a programming language
     * does not use namespaces or concepts like namespaces or if {@code W} has been
     * overridden with another implementation.
     *
     * @param relativizeSymbols The package name, namespace, etc to relativize symbols with.
     * @return Returns the SymbolWriter.
     */
    @SuppressWarnings("unchecked")
    public W setRelativizeSymbols(String relativizeSymbols) {
        putContext(RELATIVIZE_SYMBOLS, relativizeSymbols);
        return (W) this;
    }

    /**
     * Gets the import container associated with the writer.
     *
     * <p>The {@link #toString()} method of the {@code SymbolWriter} should
     * be overridden so that it includes the import container's contents in
     * the output as appropriate.
     *
     * @return Returns the import container.
     */
    public final I getImportContainer() {
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
     * <p>Tracking dependencies on a {@code SymbolWriter} allows dependencies
     * to be automatically aggregated and collected in order to generate
     * configuration files for dependency management tools (e.g., npm,
     * maven, etc).
     *
     * @param dependencies Dependency to add.
     * @return Returns the writer.
     */
    @SuppressWarnings("unchecked")
    public final W addDependency(SymbolDependencyContainer dependencies) {
        List<SymbolDependency> values = dependencies.getDependencies();
        LOGGER.finest(() -> String.format("Adding dependencies from %s: %s", dependencies, values));
        this.dependencies.addAll(values);
        return (W) this;
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
    public W addUseImports(SymbolContainer container) {
        for (Symbol symbol : container.getSymbols()) {
            addImport(symbol, symbol.getName(), SymbolReference.ContextOption.USE);
        }
        return (W) this;
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
    public W addUseImports(SymbolReference symbolReference) {
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
    public final W addImport(Symbol symbol, String alias, SymbolReference.ContextOption... options) {
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

        return (W) this;
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
}
