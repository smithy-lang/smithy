package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.utils.IoUtils;

public class SmithyIdlModelSerializerTest {
    @TestFactory
    public Stream<DynamicTest> generateTests() throws IOException, URISyntaxException {
        return Files.list(Paths.get(
                SmithyIdlModelSerializer.class.getResource("idl-serialization/cases").toURI()))
                .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> testConversion(path)));
    }

    public void testConversion(Path path) {
        Model model = Model.assembler().addImport(path).assemble().unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();
        Map<Path, String> serialized = serializer.serialize(model);

        if (serialized.size() != 1) {
            throw new RuntimeException("Exactly one smithy file should be output for generated tests.");
        }

        String serializedString = serialized.entrySet().iterator().next().getValue();
        Assertions.assertEquals(serializedString, IoUtils.readUtf8File(path).replaceAll("\\R", "\n"));
    }

    @Test
    public void multipleNamespacesGenerateMultipleFiles() throws Exception {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/multiple-namespaces/input.json"))
                .assemble()
                .unwrap();
        Path outputDir = Paths.get(getClass().getResource("idl-serialization/multiple-namespaces/output").toURI());
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .basePath(outputDir)
                .build();
        Map<Path, String> serialized = serializer.serialize(model);
        serialized.forEach((path, generated) -> assertThat(
                generated, equalTo(IoUtils.readUtf8File(path).replaceAll("\\R", "\n"))));
    }

    @Test
    public void filtersShapes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/test-model.json"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .shapeFilter(shape -> shape.getId().getNamespace().equals("ns.structures"))
                .build();
        Map<Path, String> serialized = serializer.serialize(model);

        assertThat(serialized, aMapWithSize(1));
        assertThat(serialized, hasKey(Paths.get("ns.structures.smithy")));
        assertThat(serialized.get(Paths.get("ns.structures.smithy")),
                containsString("namespace ns.structures"));
    }

    @Test
    public void filtersMetadata() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/test-model.json"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .metadataFilter(key -> false)
                .build();
        Map<Path, String> serialized = serializer.serialize(model);
        for (String output : serialized.values()) {
            assertThat(output, not(containsString("metadata")));
        }
    }

    @Test
    public void filtersTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/test-model.json"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .traitFilter(trait -> !(trait instanceof RequiredTrait))
                .build();
        Map<Path, String> serialized = serializer.serialize(model);
        for (String output : serialized.values()) {
            assertThat(output, not(containsString("@required")));
        }
    }

    @Test
    public void filtersDocumentationTrait() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/test-model.json"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .traitFilter(trait -> !(trait instanceof DocumentationTrait))
                .build();
        Map<Path, String> serialized = serializer.serialize(model);
        for (String output : serialized.values()) {
            assertThat(output, not(containsString("/// ")));
        }
    }

    @Test
    public void basePathAppliesToMetadataOnlyModel() {
        Path basePath = Paths.get("/tmp/smithytest");
        Model model = Model.assembler()
                .putMetadata("foo", StringNode.from("bar"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .basePath(basePath)
                .build();
        Map<Path, String> serialized = serializer.serialize(model);
        assertThat(serialized.keySet(), contains(basePath.resolve("metadata.smithy")));
    }

    @Test
    public void serializesRequiredTraitsUsingSugar() {
        ShapeId stringId = ShapeId.from("smithy.api#String");
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("req", stringId, builder -> builder.addTrait(new RequiredTrait()))
                .addMember("opt", stringId)
                .build();
        Model model = Model.builder().addShape(struct).build();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();

        Map<Path, String> serialized = serializer.serialize(model);
        String output = serialized.values().iterator().next();

        assertThat(output, not(containsString("@required")));
        assertThat(output, containsString("String!"));
    }
}
