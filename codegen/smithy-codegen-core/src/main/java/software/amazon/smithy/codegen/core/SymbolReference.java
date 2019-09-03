/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
public final class SymbolReference extends TypedPropertiesBag {

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
        super(properties);
        this.symbol = symbol;

        Set<Option> opts = new HashSet<>(options.length + 2);
        if (options.length == 0) {
            opts.add(ContextOption.USE);
            opts.add(ContextOption.DECLARE);
        } else {
            Collections.addAll(opts, options);
        }

        this.options = Collections.unmodifiableSet(opts);
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SymbolReference)) {
            return false;
        }

        SymbolReference that = (SymbolReference) o;
        return symbol.equals(that.symbol)
               && getProperties().equals(that.getProperties())
               && options.equals(that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, getProperties());
    }

    @Override
    public String toString() {
        return "SymbolReference{symbol=" + symbol + ", options=" + options + '}';
    }
}
