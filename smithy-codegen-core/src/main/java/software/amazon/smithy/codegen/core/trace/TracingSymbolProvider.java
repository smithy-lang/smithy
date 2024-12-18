/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Decorates a {@link SymbolProvider} with a {@link TraceFile.Builder} and adds a {@link ShapeLink} object
 * to the builder on each call to toSymbol.
 */
public final class TracingSymbolProvider implements SymbolProvider {
    private final TraceFile.Builder traceFileBuilder = new TraceFile.Builder();
    private final Set<ShapeId> visitedShapes = new HashSet<>();
    private final SymbolProvider symbolProvider;
    private final BiFunction<Shape, Symbol, List<ShapeLink>> shapeLinkCreator;

    private TracingSymbolProvider(Builder builder) {
        symbolProvider = SmithyBuilder.requiredState("symbolProvider", builder.symbolProvider);
        shapeLinkCreator = SmithyBuilder.requiredState("shapeLinkCreator", builder.shapeLinkCreator);
        traceFileBuilder.metadata(SmithyBuilder.requiredState("metadata", builder.metadata))
                .definitions(builder.artifactDefinitions);
    }

    /**
     * Builder to create a TracingSymbolProvider instance.
     *
     * @return Returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds and returns the {@link TracingSymbolProvider}'s {@link TraceFile.Builder}.
     *
     * @return The {@link TraceFile} built from this {@link TracingSymbolProvider}'s {@link TraceFile.Builder}.
     */
    public TraceFile buildTraceFile() {
        return traceFileBuilder.build();
    }

    /**
     * Converts a shape into a symbol by calling the toSymbol method of the
     * SymbolProvider used to construct this TracingSymbolProvider. Adds a
     * list of ShapeLinks to the TracingSymbolProvider's TraceFile.Builder.
     *
     * @param shape Shape to convert to Symbol.
     * @return Symbol created from Shape.
     */
    @Override
    public Symbol toSymbol(Shape shape) {
        Symbol symbol = symbolProvider.toSymbol(shape);
        ShapeId shapeId = shape.getId();
        if (visitedShapes.add(shapeId)) {
            List<ShapeLink> shapeLinks = shapeLinkCreator.apply(shape, symbol);
            if (shapeLinks.size() > 0) {
                traceFileBuilder.addShapeLinks(shapeId, shapeLinks);
            }
        }
        return symbol;
    }

    @Override
    public String toMemberName(MemberShape shape) {
        return symbolProvider.toMemberName(shape);
    }

    /**
     * Builder to create a TracingSymbolProvider instance.
     */
    public static final class Builder implements SmithyBuilder<TracingSymbolProvider> {
        private SymbolProvider symbolProvider;
        private BiFunction<Shape, Symbol, List<ShapeLink>> shapeLinkCreator;
        private ArtifactDefinitions artifactDefinitions;
        private TraceMetadata metadata;

        /**
         * Sets this Builder's ArtifactDefinitions.
         *
         * @param artifactDefinitions ArtifactDefinitions for this TracingSymbolProvider's
         *                            TraceFile.
         * @return This Builder.
         */
        public Builder artifactDefinitions(ArtifactDefinitions artifactDefinitions) {
            this.artifactDefinitions = artifactDefinitions;
            return this;
        }

        /**
         * Sets this Builder's TraceMetadata.
         *
         * @param metadata TraceMetadata for this TracingSymbolProvider's
         *                         TraceFile.
         * @return This Builder.
         */
        public Builder metadata(TraceMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the Builder's {@link TraceMetadata} based on the given type and
         * default values for other required fields. This method should ONLY be used
         * when the version, type, homepage, and typeVersion of the TraceMetadata
         * object is unknown at the time of code generation. This method will ONLY set
         * the required fields of the {@link TraceMetadata}.
         *
         * <p>The type is set to the artifactType that is passed in. The artifactType is
         * the code language of the generated artifact, e.g. Java.
         *
         * <p>The timestamp in TraceMetadata is set to the current time when the
         * method is called.
         *
         * <p>The id and version are set to a UUID that should be changed after the
         * TraceFile is constructed and the correct id and version are known.
         *
         * @param artifactType The type, i.e. language, of the TraceMetadata object.
         * @return This Builder.
         */
        public Builder setTraceMetadataAsDefault(String artifactType) {
            String tempIdVersion = UUID.randomUUID().toString();
            this.metadata = TraceMetadata.builder()
                    .version(tempIdVersion)
                    .id(tempIdVersion)
                    .type(artifactType)
                    .setTimestampAsNow()
                    .build();
            return this;
        }

        /**
         * Sets this Builder's shapeLinkCreator. The shapeLinkCreator
         * is a function that maps from a Symbol to a List of ShapeLinks.
         * Custom Functions should be designed for each code generator
         * that map apply the tags and types in the definitions files
         * to specific ShapeLinks.
         *
         * @param shapeLinkCreator A Function that defines a mapping
         *                         from a Symbol to a List of ShapeLinks.
         * @return This Builder.
         */
        public Builder shapeLinkCreator(BiFunction<Shape, Symbol, List<ShapeLink>> shapeLinkCreator) {
            this.shapeLinkCreator = shapeLinkCreator;
            return this;
        }

        /**
         * Sets this Builder's SymbolProvider.
         *
         * @param symbolProvider The SymbolProvider that the
         *                       TracingSymbolProvider will decorate.
         * @return This Builder.
         */
        public Builder symbolProvider(SymbolProvider symbolProvider) {
            this.symbolProvider = symbolProvider;
            return this;
        }

        /**
         * Builds a {@code TracingSymbolProvider} implementation.
         *
         * @return Built TracingSymbolProvider.
         */
        public TracingSymbolProvider build() {
            return new TracingSymbolProvider(this);
        }

    }

}
