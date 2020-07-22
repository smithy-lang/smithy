package software.amazon.smithy.codegen.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class DefinitionsProviderTest {
    @Test
    void assertToSymbolAddsDefinitionsToTraceFile() {
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("tag1", "tag1val");
        tagMap.put("tag2", "tag2val");

        Map<String, String> typeMap = new HashMap<>();
        tagMap.put("t1", "t1val");
        tagMap.put("t2", "t2val");

        ArtifactDefinitions definitions = ArtifactDefinitions.builder()
                .tags(tagMap)
                .types(typeMap)
                .build();

        TraceProvider traceProvider = new TraceProvider(
                new DefinitionsProvider(new TraceProviderTest.SymbolProviderTestHelper(), definitions));

        //adding shapes to our TraceFile so that it can build
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-with-shapeids.smithy"))
                .assemble()
                .unwrap();

        for (Shape shape : model.toSet()) {
            traceProvider.toSymbol(shape);
        }

        //verifying the resulting trace file can be written and parsed without exceptions
        Node node = traceProvider.getTraceFile().toNode();
        TraceFile traceFile = TraceFile.fromNode((node));

        //checking that the definitions component of our trace file is what it should be
        assertThat(traceFile.getArtifactDefinitions().get().getTags(), equalTo(tagMap));
        assertThat(traceFile.getArtifactDefinitions().get().getTypes(), equalTo(typeMap));
    }

}