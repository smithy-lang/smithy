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

import java.util.function.BiPredicate;
import java.util.logging.Logger;
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
 */
public final class ReservedWordSymbolProvider implements SymbolProvider {
    private static final Logger LOGGER = Logger.getLogger(ReservedWordSymbolProvider.class.getName());
    private static final ReservedWords IDENTITY = ReservedWords.identity();

    private final SymbolProvider delegate;
    private final ReservedWords filenameReservedWords;
    private final ReservedWords namespaceReservedWords;
    private final ReservedWords nameReservedWords;
    private final ReservedWords memberReservedWords;
    private final BiPredicate<Shape, Symbol> escapePredicate;

    private ReservedWordSymbolProvider(Builder builder) {
        this.delegate = SmithyBuilder.requiredState("symbolProvider", builder.delegate);
        this.filenameReservedWords = resolveReserved(builder.filenameReservedWords);
        this.namespaceReservedWords = resolveReserved(builder.namespaceReservedWords);
        this.nameReservedWords = resolveReserved(builder.nameReservedWords);
        this.memberReservedWords = resolveReserved(builder.memberReservedWords);
        this.escapePredicate = builder.escapePredicate;
    }

    private static ReservedWords resolveReserved(ReservedWords specific) {
        return specific != null ? specific : IDENTITY;
    }

    /**
     * Builder to create a ReservedWordSymbolProvider instance.
     *
     * @return Returns a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        Symbol upstream = delegate.toSymbol(shape);

        // Only escape symbols when the predicate returns true.
        if (!escapePredicate.test(shape, upstream)) {
            return upstream;
        }

        String newName = convertWord("name", upstream.getName(), nameReservedWords);
        String newNamespace = convertWord("namespace", upstream.getNamespace(), namespaceReservedWords);
        String newDeclarationFile = convertWord("filename", upstream.getDeclarationFile(), filenameReservedWords);
        String newDefinitionFile = convertWord("filename", upstream.getDefinitionFile(), filenameReservedWords);

        // Only create a new symbol when needed.
        if (newName.equals(upstream.getName())
                && newNamespace.equals(upstream.getNamespace())
                && newDeclarationFile.equals(upstream.getDeclarationFile())
                && newDefinitionFile.equals(upstream.getDeclarationFile())) {
            return upstream;
        }

        return upstream.toBuilder()
                .name(newName)
                .namespace(newNamespace, upstream.getNamespaceDelimiter())
                .declarationFile(newDeclarationFile)
                .definitionFile(newDefinitionFile)
                .build();
    }

    @Override
    public String toMemberName(Shape shape) {
        return convertWord("member", delegate.toMemberName(shape), memberReservedWords);
    }

    private static String convertWord(String name, String result, ReservedWords reservedWords) {
        if (!reservedWords.isReserved(result)) {
            return result;
        }

        String newResult = reservedWords.escape(result);
        LOGGER.warning(() -> String.format(
                "Reserved word: %s is a reserved word for a %s. Converting to %s",
                result, name, newResult));
        return newResult;
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
         * Builds the provider.
         *
         * @return Returns the built provider.
         */
        public SymbolProvider build() {
            return new ReservedWordSymbolProvider(this);
        }

        /**
         * Sets the <strong>required</strong> delegate symbol provider.
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
}
