package software.amazon.smithy.codegen.core;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class TraceProviderTest {

    @Test
    void assertToSymbolCreatesTraceFileWithCorrectValues() {
        TraceProvider traceProvider = new TraceProvider(new SymbolProviderTestHelper());
        Shape shape = StringShape.builder().id("namespace.foo#baz").build();
        traceProvider.toSymbol(shape);
        TraceFile traceFile = traceProvider.getTraceFile();

        //assert TraceFile ArtifactMetadata contains the correct type
        assertThat(traceFile.getArtifactMetadata().getType(), equalTo("TypeScript"));

        //assert that all values in model have correct ShapeLink values in the tracefile
        ShapeLink link = traceFile.getShapes().get(ShapeId.from("namespace.foo#baz")).get(0);
        assertThat(link.getType(), equalTo("FIELD"));
        assertThat(link.getId(), equalTo("namespace.foo.baz"));
        assertThat(link.getFile(), equalTo(Optional.of("file.ts")));
    }

    @Test
    void assertDoesNotThrowWhenParsingWritingToSymbolTraceFile() {
        TraceProvider traceProvider = new TraceProvider(new SymbolProviderTestHelper());
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-with-shapeids.smithy"))
                .assemble()
                .unwrap();

        for (Shape shape : model.toSet()) {
            traceProvider.toSymbol(shape);
        }

        //verifying that it can be written and parsed without exception
        Assertions.assertDoesNotThrow(() -> {
            Node node = traceProvider.getTraceFile().toNode();
            TraceFile traceFile = TraceFile.fromNode((node));
            traceFile.validateModel(model);
        });
    }

    @Test
    void assertToSymbolWorksWithCustomShapeProvider() {
        TraceProvider traceProvider = new TraceProvider(new ShapeProviderHelper(new SymbolProviderTestHelper()));
        Shape shape = StringShape.builder().id("namespace.foo#baz").build();
        traceProvider.toSymbol(shape);
        TraceFile traceFile = traceProvider.getTraceFile();

        //assert that all values in model have correct ShapeLink values in the tracefile
        ShapeLink link = traceFile.getShapes().get(ShapeId.from("namespace.foo#baz")).get(0);
        assertThat(link.getType(), equalTo("baz"));
        assertThat(link.getId(), equalTo("zip"));
        assertThat(link.getFile(), equalTo(Optional.of("foo")));
    }

    @Test
    void assertGetShapeLinkCreatesShapeLinkFromSymbol() {
        Symbol symbol = Symbol.builder()
                .definitionFile("my_name.py")
                .namespace("namespace/foo", "/")
                .name("baz")
                .build();

        Shape shape = StringShape.builder().id("namespace.foo#baz").build();


        ShapeLink link = TraceProvider.getShapeLink(symbol, shape);
        assertThat(link.getId(), equalTo("namespace.foo.baz"));
        assertThat(link.getType(), equalTo("FIELD"));
        assertThat(link.getFile().get(), equalTo("my_name.py"));
    }

    @Test
    void assertFillArtifactMetadataGeneratesCorrectMetadata() {
        Symbol symbol = Symbol.builder()
                .definitionFile("my_name.py")
                .namespace("namespace/name", "/")
                .name("baz")
                .build();

        ArtifactMetadata metadata = TraceProvider.fillArtifactMetadata(symbol);

        assertThat(metadata.getType(), equalTo("Python"));
        Assertions.assertDoesNotThrow(() -> Instant.parse(metadata.getTimestamp()));
        Assertions.assertDoesNotThrow(() -> UUID.fromString(metadata.getId()));
    }

    //test class that substitutes for language-specific symbol provider
    static class SymbolProviderTestHelper implements SymbolProvider {
        @Override
        public Symbol toSymbol(Shape shape) {
            return Symbol.builder().putProperty("shape", shape)
                    .name(shape.getId().getName())
                    .namespace(shape.getId().getNamespace(), "/")
                    .definitionFile("file.ts").build();
        }

    }

    static class ShapeProviderHelper extends SymbolProviderDecorator {
        /**
         * Constructor for {@link SymbolProviderDecorator}.
         *
         * @param provider The {@link SymbolProvider} to be decorated.
         */
        public ShapeProviderHelper(SymbolProvider provider) {
            super(provider);
        }

        @Override
        public Symbol toSymbol(Shape shape) {
            Symbol symbol = super.toSymbol(shape);
            ShapeLink link = ShapeLink.builder()
                    .id("zip")
                    .type("baz")
                    .file("foo")
                    .build();
            symbol = symbol.toBuilder().putProperty(TraceFile.SHAPES_TEXT, link).build();
            return symbol;
        }

    }

}