/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

class TracingSymbolProviderTest {

    @Test
    void assertBuildDoesNotThrowWithAllFields() {
        Assertions.assertDoesNotThrow(() -> {
            TracingSymbolProvider.builder()
                    .metadata(constructTraceMetadata())
                    .artifactDefinitions(constructArtifactDefinitions())
                    .symbolProvider(new TestSymbolProvider())
                    .shapeLinkCreator(constructFunction())
                    .build();
        });
    }

    @Test
    void assertBuildDoesNotThrowWithRequiredFields() {
        Assertions.assertDoesNotThrow(() -> {
            TracingSymbolProvider.builder()
                    .metadata(constructTraceMetadata())
                    .symbolProvider(new TestSymbolProvider())
                    .shapeLinkCreator(constructFunction())
                    .build();
        });
    }

    @Test
    void assertBuildDoesNotThrowWithDefaultTraceMetadata() {
        Assertions.assertDoesNotThrow(() -> {
            TracingSymbolProvider.builder()
                    .setTraceMetadataAsDefault("Java")
                    .symbolProvider(new TestSymbolProvider())
                    .shapeLinkCreator(constructFunction())
                    .build();
        });
    }

    @Test
    void assertBuildFailsWithoutSymbolProvider() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> TracingSymbolProvider.builder()
                        .metadata(constructTraceMetadata())
                        .shapeLinkCreator(constructFunction())
                        .build());
    }

    @Test
    void assertBuildFailsWithoutTraceMetadata() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> TracingSymbolProvider.builder()
                        .symbolProvider(new TestSymbolProvider())
                        .shapeLinkCreator(constructFunction())
                        .build());
    }

    @Test
    void assertBuildFailsWithoutShapeLinkCreator() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> TracingSymbolProvider.builder()
                        .metadata(constructTraceMetadata())
                        .symbolProvider(new TestSymbolProvider())
                        .build());
    }

    @Test
    void assertToSymbolShapeLinkCreatorCreatesShapeLink() {
        TracingSymbolProvider tracingSymbolProvider = TracingSymbolProvider.builder()
                .metadata(constructTraceMetadata())
                .artifactDefinitions(constructArtifactDefinitions())
                .symbolProvider(new TestSymbolProvider())
                .shapeLinkCreator(constructFunction())
                .build();

        Shape shape = StringShape.builder().id("namespace.foo#baz").build();

        Symbol symbol = tracingSymbolProvider.toSymbol(shape);

        TraceFile traceFile = tracingSymbolProvider.buildTraceFile();

        ShapeLink createdShapeLink = traceFile.getShapes().get(ShapeId.from("namespace.foo#baz")).get(0);

        assertThat(createdShapeLink.getType(), equalTo("TYPE"));
        assertThat(createdShapeLink.getId(), equalTo(symbol.toString()));
    }

    @Test
    void assertToSymbolDoesNotDuplicateShapeLinks() {
        TracingSymbolProvider tracingSymbolProvider = TracingSymbolProvider.builder()
                .metadata(constructTraceMetadata())
                .artifactDefinitions(constructArtifactDefinitions())
                .symbolProvider(new TestSymbolProvider())
                .shapeLinkCreator(constructFunction())
                .build();

        Shape shape = StringShape.builder().id("namespace.foo#baz").build();

        // Call toSymbol twice to make sure the ShapeId is not added twice.
        tracingSymbolProvider.toSymbol(shape);
        tracingSymbolProvider.toSymbol(shape);

        TraceFile traceFile = tracingSymbolProvider.buildTraceFile();

        assertThat(traceFile.getShapes().get(ShapeId.from("namespace.foo#baz")).size(), equalTo(1));
    }

    ArtifactDefinitions constructArtifactDefinitions() {
        return ArtifactDefinitions.builder()
                .addTag("service", "Service client")
                .addType("TYPE", "Class, interface (including annotation type), or enum declaration")
                .build();
    }

    TraceMetadata constructTraceMetadata() {
        return TraceMetadata.builder()
                .id("software.amazon.awssdk.services:snowball:2.10.79")
                .version("2.10.79")
                .type("Java")
                .setTimestampAsNow()
                .build();
    }

    BiFunction<Shape, Symbol, List<ShapeLink>> constructFunction() {
        return (shape, symbol) -> {
            List<ShapeLink> list = new ArrayList<>();
            list.add(ShapeLink.builder()
                    .id(symbol.toString())
                    .type("TYPE")
                    .build());
            return list;
        };
    }

    // Test class that substitutes for language-specific symbol provider.
    static class TestSymbolProvider implements SymbolProvider {
        @Override
        public Symbol toSymbol(Shape shape) {
            return Symbol.builder()
                    .putProperty("shape", shape)
                    .name(shape.getId().getName())
                    .namespace(shape.getId().getNamespace(), "/")
                    .definitionFile("file.java")
                    .build();
        }

    }

}
