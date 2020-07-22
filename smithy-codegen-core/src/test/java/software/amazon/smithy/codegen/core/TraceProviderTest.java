package software.amazon.smithy.codegen.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

class TraceProviderTest {

    @Test
    void assertToSymbolCreatesTraceFileWithRequiredFields() {
        TraceProvider traceProvider = new TraceProvider(new SymbolProviderTestHelper());
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-with-shapeids.smithy"))
                .assemble()
                .unwrap();

        for (Shape shape : model.toSet()) {
            traceProvider.toSymbol(shape);
        }

        //verifying that it can be written and parsed without exception
        Node node = traceProvider.getTraceFile().toNode();
        TraceFile traceFile = TraceFile.fromNode((node));

        //verifying TraceFile ArtifactMetadata contains the correct type
        assertThat(traceFile.getArtifactMetadata().getType(), equalTo("TypeScript"));

        model.toSet().parallelStream().forEach(shape -> {
            if(!Prelude.isPreludeShape(shape)) { //ignoring everything in smithy.api
                ShapeId x = shape.getId();
                assertThat(traceFile.getShapes(), hasKey(x));
                assertThat(traceFile.getShapes().get(x).get(0).getType(), equalTo("FIELD"));
                assertThat(traceFile.getShapes().get(x).get(0).getFile().get(), equalTo("namespace.ts"));
            }
        });
    }

    @Test
    void assertGetShapeLinkCreatesShapeLinkFromSymbol() {
        Symbol symbol = Symbol.builder()
                .definitionFile("my_name.py")
                .namespace("namespace/name", "/")
                .name("baz")
                .build();

        ShapeLink link = TraceProvider.getShapeLink(symbol);
        assertThat(link.getId(), equalTo("namespace.name.baz"));
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
        assertThat(Instant.parse(metadata.getTimestamp()), is(Instant.class));
        assertThat(UUID.fromString(metadata.getId()), is(UUID.class));
    }

    //mock class that substitutes for language-specific symbol provider
    static class SymbolProviderTestHelper implements SymbolProvider {
        @Override
        public Symbol toSymbol(Shape shape) {
            return Symbol.builder().putProperty("shape", shape)
                    .name("boolean")
                    .namespace("namespace", "/")
                    .definitionFile("namespace.ts").build();
        }
    }

}