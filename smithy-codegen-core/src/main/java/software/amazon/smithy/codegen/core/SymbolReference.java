/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a reference from a Symbol to another Symbol.
 *
 * <p>A reference from one symbol to another is used when a symbol definition
 * refers to other symbols (for example, when using things like generic type
 * parameters) or when using a symbol that has generic type parameters in
 * its signature. References can contain arbitrary properties that can be
 * accessed using {@link #getProperties}, {@link #getProperty}, and
 * {@link #expectProperty}.
 *
 * Options can be added to a SymbolReference to give more context about the
 * reference. For example, the {@link ContextOption} enum is used to define
 * the context in which a reference is relevant (e.g., only when defining
 * a symbol vs only when using/importing a symbol). If no options are
 * provided when creating a {@code SymbolReference}, the reference defaults
 * to using both the {@link ContextOption#DECLARE} and
 * {@link ContextOption#USE} options, meaning that the reference is
 * necessary both when defining and when using a symbol.
 */
public final class SymbolReference
        extends TypedPropertiesBag
        implements SymbolContainer, SymbolDependencyContainer, ToSmithyBuilder<SymbolReference> {

    /**
     * Top-level interface for all {@code SymbolReference} options.
     */
    public interface Option {}

    /**
     * Options used to control the context of when the symbol reference is needed.
     *
     * <p>These context options are used to define the context in which one symbol
     * refers to another. For example, if a reference is only needed when the symbol
     * is defined and not needed when the symbol is used, then code generators can
     * automatically add the necessary imports for each circumstance.
     */
    public enum ContextOption implements Option {
        /** Indicates that the referenced symbol is needed when declaring the symbol. */
        DECLARE,

        /** Indicates that the referenced symbol is needed when importing/using the symbol. */
        USE
    }

    private final Symbol symbol;
    private final Set<Option> options;
    private final String alias;

    /**
     * @param symbol Symbol that is referenced.
     * @param options Options to store with the reference.
     */
    public SymbolReference(Symbol symbol, Option... options) {
        this(symbol, Collections.emptyMap(), options);
    }

    /**
     * @param symbol Symbol that is referenced.
     * @param properties Bag of reference properties.
     * @param options Options to store with the reference.
     */
    public SymbolReference(Symbol symbol, Map<String, Object> properties, Option... options) {
        this(new Builder().symbol(symbol).properties(properties).options(options));
    }

    private SymbolReference(Builder builder) {
        super(builder);
        this.symbol = SmithyBuilder.requiredState("symbol", builder.symbol);
        this.alias = builder.alias == null ? builder.symbol.getName() : builder.alias;

        if (!builder.options.hasValue() || builder.options.peek().isEmpty()) {
            this.options = SetUtils.of(ContextOption.USE, ContextOption.DECLARE);
        } else {
            this.options = builder.options.copy();
        }
    }

    /**
     * @return Returns a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the referenced symbol.
     *
     * @return Returns the symbol.
     */
    public Symbol getSymbol() {
        return symbol;
    }

    /**
     * Gets the alias to use when referring to the Symbol.
     *
     * <p>The value of {@code getSymbol().getName()} is returned if no
     * alias was explicitly configured on the reference.
     *
     * <p>An alias is used in some programming languages to change the
     * way a symbol is referenced in a source file. Aliases are often used
     * for de-conflicting symbols.
     *
     * @return Returns the alias.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Gets all of the reference options.
     *
     * @return Returns the options set.
     */
    public Set<Option> getOptions() {
        return options;
    }

    /**
     * Checks if the given option is set on the symbol.
     *
     * @param option Option to check.
     * @return Returns true if this option is set.
     */
    public boolean hasOption(Option option) {
        return options.contains(option);
    }

    @Override
    public List<Symbol> getSymbols() {
        return Collections.singletonList(getSymbol());
    }

    @Override
    public List<SymbolDependency> getDependencies() {
        return symbol.getDependencies();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .symbol(symbol)
                .options(options)
                .properties(getProperties())
                .typedProperties(getTypedProperties())
                .alias(alias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SymbolReference)) {
            return false;
        }

        SymbolReference that = (SymbolReference) o;
        return super.equals(o)
                && symbol.equals(that.symbol)
                && getProperties().equals(that.getProperties())
                && options.equals(that.options)
                && alias.equals(that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, alias, getProperties());
    }

    @Override
    public String toString() {
        return "SymbolReference{symbol=" + symbol + ", alias='" + alias + "', options=" + options + "}";
    }

    /**
     * Builds a SymbolReference.
     */
    public static final class Builder
            extends TypedPropertiesBag.Builder<Builder>
            implements SmithyBuilder<SymbolReference> {

        private Symbol symbol;
        private final BuilderRef<Set<Option>> options = BuilderRef.forUnorderedSet();
        private String alias;

        private Builder() {}

        @Override
        public SymbolReference build() {
            return new SymbolReference(this);
        }

        /**
         * Sets the Symbol referenced by the SymbolReference.
         *
         * @param symbol Symbol to reference.
         * @return Returns the builder.
         */
        public Builder symbol(Symbol symbol) {
            this.symbol = symbol;
            return this;
        }

        /**
         * Replaces the Set of Options to the SymbolReference.
         *
         * @param options Options to add.
         * @return Returns the builder.
         */
        public Builder options(Set<Option> options) {
            this.options.clear();
            this.options.get().addAll(options);
            return this;
        }

        /**
         * Replaces the array of Options in the SymbolReference.
         *
         * @param options Options to add.
         * @return Returns the builder.
         */
        public Builder options(Option... options) {
            this.options.clear();
            Collections.addAll(this.options.get(), options);
            return this;
        }

        /**
         * Adds an alias to the SymbolReference.
         *
         * <p>An alias is used in some programming languages to change the
         * way a symbol is referenced in a source file. Aliases are often used
         * for de-conflicting symbols.
         *
         * @param alias Alias to assign the symbol.
         * @return Returns the builder.
         */
        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }
    }
}
