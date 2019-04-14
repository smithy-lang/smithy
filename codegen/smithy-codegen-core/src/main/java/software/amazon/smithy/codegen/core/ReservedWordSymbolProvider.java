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

    private ReservedWordSymbolProvider(Builder builder) {
        this.delegate = SmithyBuilder.requiredState("symbolProvider", builder.delegate);
        this.filenameReservedWords = resolveReserved(builder.filenameReservedWords);
        this.namespaceReservedWords = resolveReserved(builder.namespaceReservedWords);
        this.nameReservedWords = resolveReserved(builder.nameReservedWords);
        this.memberReservedWords = resolveReserved(builder.memberReservedWords);
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
        return upstream.toBuilder()
                .name(nameReservedWords.escape(upstream.getName()))
                .namespace(namespaceReservedWords.escape(upstream.getNamespace()), upstream.getNamespaceDelimiter())
                .declarationFile(filenameReservedWords.escape(upstream.getDeclarationFile()))
                .definitionFile(filenameReservedWords.escape(upstream.getDefinitionFile()))
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
    }
}
