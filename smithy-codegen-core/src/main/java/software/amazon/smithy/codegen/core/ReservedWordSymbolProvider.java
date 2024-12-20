/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.function.BiPredicate;
import java.util.logging.Logger;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Decorates a {@link SymbolProvider} by passing values through context
 * specific {@link ReservedWords} implementations.
 *
 * <p>A specific {@code ReservedWords} implementation can be registered
 * for each kind of symbol provided by the delegated {@code SymbolProvider}.
 * For example, reserved words can be created that are specific to
 * class names.
 *
 * <p>This motivation behind this class is to allow more general purpose
 * implementations of {@code SymbolProvider} and {@code ReservedWords} to
 * be composed.
 *
 * <p>A warning is logged each time a symbol is renamed by a reserved words
 * implementation.
 *
 * <p>SymbolProvider implementations that need to recursively call themselves
 * in a way that requires recursive symbols to be escaped will need to
 * manually make calls into {@link ReservedWordSymbolProvider.Escaper} and
 * cannot be decorated by an instance of {@code ReservedWordSymbolProvider}.
 * For example, this is the case if a list of strings needs to be turned
 * into something like "Array[%s]" where "%s" is the symbol name of the
 * targeted member.
 */
public final class ReservedWordSymbolProvider implements SymbolProvider {
    private static final Logger LOGGER = Logger.getLogger(ReservedWordSymbolProvider.class.getName());
    private static final ReservedWords IDENTITY = ReservedWords.identity();

    private final SymbolProvider delegate;
    private final Escaper escaper;

    private ReservedWordSymbolProvider(SymbolProvider delegate, Escaper escaper) {
        this.delegate = delegate;
        this.escaper = escaper;
    }

    /**
     * Builder to create a ReservedWordSymbolProvider instance.
     *
     * @return Returns a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private static ReservedWords resolveReserved(ReservedWords specific) {
        return specific != null ? specific : IDENTITY;
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        return escaper.escapeSymbol(shape, delegate.toSymbol(shape));
    }

    @Override
    public String toMemberName(MemberShape shape) {
        return escaper.escapeMemberName(delegate.toMemberName(shape));
    }

    /**
     * Builder to build a {@link ReservedWordSymbolProvider}.
     */
    public static final class Builder {
        private SymbolProvider delegate;
        private ReservedWords filenameReservedWords;
        private ReservedWords namespaceReservedWords;
        private ReservedWords nameReservedWords;
        private ReservedWords memberReservedWords;
        private BiPredicate<Shape, Symbol> escapePredicate = (shape, symbol) -> true;

        /**
         * Builds a {@code SymbolProvider} implementation that wraps another
         * symbol provider and escapes its results.
         *
         * <p>This might not always be the right solution. For example,
         * symbol providers often need to recursively resolve symbols to
         * create shapes like arrays and maps. In these cases, delegating
         * would be awkward or impossible since the symbol provider being
         * wrapped would also need access to the wrapper. In cases like this,
         * use {@link #buildEscaper()} and pass that into the SymbolProvider
         * directly.
         *
         * @return Returns the built SymbolProvider that delegates to another.
         */
        public SymbolProvider build() {
            return new ReservedWordSymbolProvider(
                    SmithyBuilder.requiredState("delegate", delegate),
                    buildEscaper());
        }

        /**
         * Builds a {@code SymbolProvider.Escaper} that is used to manually
         * escape {@code Symbol}s and member names.
         *
         * @return Returns the built escaper.
         */
        public Escaper buildEscaper() {
            return new Escaper(this);
        }

        /**
         * Sets the delegate symbol provider.
         *
         * <p>This is only required when calling {@link #build} to build
         * a {@code SymbolProvider} that delegates to another provider.
         *
         * @param delegate Symbol provider to delegate to.
         * @return Returns the builder
         */
        public Builder symbolProvider(SymbolProvider delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * Sets the reserved word implementation for file names.
         *
         * <p>If not provided, file names are not passed through a reserved
         * words implementation after calling the delegate.
         *
         * @param filenameReservedWords Reserved word implementation for namespaces.
         * @return Returns the builder.
         */
        public Builder filenameReservedWords(ReservedWords filenameReservedWords) {
            this.filenameReservedWords = filenameReservedWords;
            return this;
        }

        /**
         * Sets the reserved word implementation for namespace names.
         *
         * <p>If not provided, namespace names are not passed through a reserved
         * words implementation after calling the delegate.
         *
         * @param namespaceReservedWords Reserved word implementation for namespaces.
         * @return Returns the builder.
         */
        public Builder namespaceReservedWords(ReservedWords namespaceReservedWords) {
            this.namespaceReservedWords = namespaceReservedWords;
            return this;
        }

        /**
         * Sets the reserved word implementation for names (structures names,
         * class names, etc.).
         *
         * <p>If not provided, names are not passed through a reserved words
         * implementation after calling the delegate.
         *
         * @param nameReservedWords Reserved word implementation for containers.
         * @return Returns the builder.
         */
        public Builder nameReservedWords(ReservedWords nameReservedWords) {
            this.nameReservedWords = nameReservedWords;
            return this;
        }

        /**
         * Sets the reserved word implementation for members.
         *
         * <p>If not provided, member names are not passed through a reserved
         * words implementation after calling the delegate.
         *
         * @param memberReservedWords Reserved word implementation for members.
         * @return Returns the builder.
         */
        public Builder memberReservedWords(ReservedWords memberReservedWords) {
            this.memberReservedWords = memberReservedWords;
            return this;
        }

        /**
         * Sets a predicate that is used to control when a shape + symbol
         * combination should be checked if it's a reserved word.
         *
         * <p>The predicate is invoked when {@code toSymbol} is called. It
         * is used to disable/enable escaping reserved words based on the
         * shape and symbol. The given predicate accepts the {@code Shape}
         * and the {@code Symbol} that was created for the shape and returns
         * true if reserved word checks should be made or false if reserved
         * word checks should not be made. For example, some code generators
         * only escape words that have namespaces to differentiate between
         * language built-ins and user-defined types.
         *
         * <p>By default, all symbols are checked for reserved words.
         *
         * @param escapePredicate Predicate that returns true if escaping should be checked.
         * @return Returns the builder.
         */
        public Builder escapePredicate(BiPredicate<Shape, Symbol> escapePredicate) {
            this.escapePredicate = escapePredicate;
            return this;
        }
    }

    /**
     * Uses to manually escape {@code Symbol}s and member names.
     */
    public static final class Escaper {
        private final ReservedWords filenameReservedWords;
        private final ReservedWords namespaceReservedWords;
        private final ReservedWords nameReservedWords;
        private final ReservedWords memberReservedWords;
        private final BiPredicate<Shape, Symbol> escapePredicate;

        private Escaper(Builder builder) {
            this.filenameReservedWords = resolveReserved(builder.filenameReservedWords);
            this.namespaceReservedWords = resolveReserved(builder.namespaceReservedWords);
            this.nameReservedWords = resolveReserved(builder.nameReservedWords);
            this.memberReservedWords = resolveReserved(builder.memberReservedWords);
            this.escapePredicate = builder.escapePredicate;
        }

        /**
         * Escapes the given symbol using the reserved words implementations
         * registered for each component.
         *
         * @param shape Shape being turned into a {@code Symbol}.
         * @param symbol {@code Symbol} to escape.
         * @return Returns the escaped {@code Symbol}.
         */
        public Symbol escapeSymbol(Shape shape, Symbol symbol) {
            // Only escape symbols when the predicate returns true.
            if (!escapePredicate.test(shape, symbol)) {
                return symbol;
            }

            String newName = convertWord("name", symbol.getName(), nameReservedWords);
            String newNamespace = convertWord("namespace", symbol.getNamespace(), namespaceReservedWords);
            String newDeclarationFile = convertWord("filename", symbol.getDeclarationFile(), filenameReservedWords);
            String newDefinitionFile = convertWord("filename", symbol.getDefinitionFile(), filenameReservedWords);

            // Only create a new symbol when needed.
            if (newName.equals(symbol.getName())
                    && newNamespace.equals(symbol.getNamespace())
                    && newDeclarationFile.equals(symbol.getDeclarationFile())
                    && newDefinitionFile.equals(symbol.getDeclarationFile())) {
                return symbol;
            }

            return symbol.toBuilder()
                    .name(newName)
                    .namespace(newNamespace, symbol.getNamespaceDelimiter())
                    .declarationFile(newDeclarationFile)
                    .definitionFile(newDefinitionFile)
                    .build();
        }

        /**
         * Escapes the given member name if needed.
         *
         * @param memberName Member name to escape.
         * @return Returns the possibly escaped member name.
         */
        public String escapeMemberName(String memberName) {
            return convertWord("member", memberName, memberReservedWords);
        }

        private static String convertWord(String name, String result, ReservedWords reservedWords) {
            if (!reservedWords.isReserved(result)) {
                return result;
            }

            String newResult = reservedWords.escape(result);
            LOGGER.warning(() -> String.format(
                    "Reserved word: %s is a reserved word for a %s. Converting to %s",
                    result,
                    name,
                    newResult));
            return newResult;
        }
    }
}
