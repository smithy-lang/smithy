/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;

public class SmithyIdlModelSerializerTest {
    private static final URL TEST_FILE_URL =
            Objects.requireNonNull(SmithyIdlModelSerializer.class.getResource("idl-serialization/cases"));

    @TestFactory
    public Stream<DynamicTest> generateTests() throws IOException, URISyntaxException {
        return Files.list(Paths.get(TEST_FILE_URL.toURI()))
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
        assertEquals(IoUtils.readUtf8File(path).replaceAll("\\R", "\n"), serializedString);
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
                generated,
                equalTo(IoUtils.readUtf8File(path).replaceAll("\\R", "\n"))));
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

        assertThat(serialized, aMapWithSize(2));
        assertThat(serialized, hasKey(Paths.get("ns.structures.smithy")));
        assertThat(serialized.get(Paths.get("ns.structures.smithy")),
                containsString("namespace ns.structures"));
        assertThat(serialized, hasKey(Paths.get("metadata.smithy")));
        assertThat(serialized.get(Paths.get("metadata.smithy")),
                containsString("metadata shared = true"));
        assertThat(serialized, not(hasKey(Paths.get("smithy.api.smithy"))));
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
    public void emptyServiceVersionNotSerialized() {
        ServiceShape service = ServiceShape.builder()
                .id("com.foo#Example")
                .build();
        Model model = Model.builder().addShape(service).build();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();
        Map<Path, String> serialized = serializer.serialize(model);

        assertThat(serialized.get(Paths.get("com.foo.smithy")), not(containsString("version: \"\"")));
    }

    @Test
    public void transientTraitsAreNotSerialized() {
        ShapeId originalId = ShapeId.from("com.foo.nested#Str");
        StringShape stringShape = StringShape.builder()
                .id("com.foo#Str")
                .addTrait(new OriginalShapeIdTrait(originalId))
                .build();
        Model model = Model.builder()
                .addShape(stringShape)
                .build();

        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();
        Map<Path, String> results = serializer.serialize(model);

        assertThat(results.get(Paths.get("com.foo.smithy")),
                not(containsString(OriginalShapeIdTrait.ID.toString())));
    }

    @Test
    public void canEnableSerializingPrelude() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/test-model.json"))
                .assemble()
                .unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder()
                .serializePrelude()
                .build();
        Map<Path, String> serialized = serializer.serialize(model);
        assertThat(serialized.get(Paths.get("smithy.api.smithy")), containsString("namespace smithy.api"));
    }

    @Test
    public void serializesSetsAsListsWithUniqueItems() {
        SetShape set = SetShape.builder()
                .id("smithy.example#Set")
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(set).assemble().unwrap();
        SmithyIdlModelSerializer serializer = SmithyIdlModelSerializer.builder().build();
        Map<Path, String> models = serializer.serialize(model);
        String modelResult = models.get(Paths.get("smithy.example.smithy"));

        assertThat(modelResult, containsString("list Set"));
        assertThat(modelResult, containsString("@uniqueItems"));
    }

    @Test
    public void serializesRootLevelDefaults() {
        String stringModel = "$version: \"2.0\"\n"
                + "namespace smithy.example\n"
                + "@default(false)\n"
                + "boolean PrimitiveBool\n";
        Model model = Model.assembler().addUnparsedModel("test.smithy", stringModel).assemble().unwrap();
        Map<Path, String> reserialized = SmithyIdlModelSerializer.builder().build().serialize(model);
        String modelResult = reserialized.get(Paths.get("smithy.example.smithy"));
        Model model2 = Model.assembler().addUnparsedModel("test.smithy", modelResult).assemble().unwrap();

        assertThat(model.expectShape(ShapeId.from("smithy.example#PrimitiveBool")).hasTrait(DefaultTrait.ID),
                is(true));
        assertThat(model2.expectShape(ShapeId.from("smithy.example#PrimitiveBool")).hasTrait(DefaultTrait.ID),
                is(true));
        assertThat(model2, equalTo(model2));
    }

    @Test
    public void usesOriginalSourceLocation() {
        URL resource = getClass().getResource("idl-serialization/out-of-order.smithy");
        Model model = Model.assembler().addImport(resource).assemble().unwrap();
        Map<Path, String> reserialized = SmithyIdlModelSerializer.builder()
                .componentOrder(SmithyIdlComponentOrder.SOURCE_LOCATION)
                .build()
                .serialize(model);
        String modelResult = reserialized.values().iterator().next().replace("\r\n", "\n");

        assertThat(modelResult, equalTo(IoUtils.readUtf8Url(resource).replace("\r\n", "\n")));
    }

    @Test
    public void sortsAlphabetically() {
        URL before = getClass().getResource("idl-serialization/alphabetical/before.smithy");
        URL after = getClass().getResource("idl-serialization/alphabetical/after.smithy");
        URL metadata = getClass().getResource("idl-serialization/alphabetical/metadata.smithy");

        Model model = Model.assembler().addImport(before).assemble().unwrap();
        Map<Path, String> reserialized = SmithyIdlModelSerializer.builder()
                .componentOrder(SmithyIdlComponentOrder.ALPHA_NUMERIC)
                .build()
                .serialize(model);

        String modelResult = reserialized.get(Paths.get("com.example.smithy")).replace("\r\n", "\n");
        String metadataResult = reserialized.get(Paths.get("metadata.smithy")).replace("\r\n", "\n");

        assertThat(modelResult, equalTo(IoUtils.readUtf8Url(after).replace("\r\n", "\n")));
        assertThat(metadataResult, equalTo(IoUtils.readUtf8Url(metadata).replace("\r\n", "\n")));
    }

    @Test
    public void handlesEnumMixins() {
        URL resource = getClass().getResource("idl-serialization/enum-mixin-input.smithy");
        Model model = Model.assembler().addImport(resource).assemble().unwrap();
        Map<Path, String> serialized = SmithyIdlModelSerializer.builder().build().serialize(model);

        if (serialized.size() != 1) {
            throw new RuntimeException("Exactly one smithy file should be output for generated tests.");
        }

        String expectedOutput = IoUtils.readUtf8Resource(getClass(), "idl-serialization/enum-mixin-output.smithy")
                .replaceAll("\\R", "\n");
        String serializedString = serialized.entrySet().iterator().next().getValue();
        assertEquals(expectedOutput, serializedString);
    }

    @Test
    public void handlesCustomInlineSuffixes() {
        URL resource = getClass().getResource("idl-serialization/custom-inline-io.smithy");
        Model model = Model.assembler().addImport(resource).assemble().unwrap();

        Assertions.assertTrue(model.getShape(ShapeId.from("com.example#InlineOperationRequest")).isPresent());
        Assertions.assertTrue(model.getShape(ShapeId.from("com.example#InlineOperationResponse")).isPresent());

        Map<Path, String> reserialized = SmithyIdlModelSerializer.builder()
                .inlineInputSuffix("Request")
                .inlineOutputSuffix("Response")
                .build()
                .serialize(model);
        String modelResult = reserialized.values().iterator().next().replace("\r\n", "\n");

        assertThat(modelResult, equalTo(IoUtils.readUtf8Url(resource).replace("\r\n", "\n")));
    }

    @Test
    public void canInferInlineSuffixes() {
        Map<Path, URL> resources = MapUtils.of(
                Paths.get("default.smithy"),
                getClass().getResource("idl-serialization/inferred-io/default.smithy"),
                Paths.get("main.smithy"),
                getClass().getResource("idl-serialization/inferred-io/main.smithy"),
                Paths.get("mixed.smithy"),
                getClass().getResource("idl-serialization/inferred-io/mixed.smithy"),
                Paths.get("shared.smithy"),
                getClass().getResource("idl-serialization/inferred-io/shared.smithy"));
        ModelAssembler assembler = Model.assembler();
        resources.values().forEach(assembler::addImport);
        Model model = assembler.assemble().unwrap();

        Map<Path, String> reserialized = SmithyIdlModelSerializer.builder()
                .shapePlacer(s -> Paths.get(s.getSourceLocation().getFilename()).getFileName())
                .inferInlineIoSuffixes(true)
                .build()
                .serialize(model);

        assertThat(reserialized.size(), equalTo(resources.size()));
        for (Map.Entry<Path, String> entry : reserialized.entrySet()) {
            Path path = entry.getKey();
            String actual = entry.getValue().replace("\r\n", "\n");
            String expected = IoUtils.readUtf8Url(resources.get(path)).replace("\r\n", "\n");
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void coercesInlineIO() {
        Model before = Model.assembler()
                .addImport(getClass().getResource("idl-serialization/coerced-io/before.smithy"))
                .assemble()
                .unwrap();

        Map<Path, String> reserialized = SmithyIdlModelSerializer.builder()
                .coerceInlineIo(true)
                .build()
                .serialize(before);
        String modelResult = reserialized.values().iterator().next().replace("\r\n", "\n");

        String expected = IoUtils.readUtf8Url(getClass().getResource("idl-serialization/coerced-io/after.smithy"))
                .replace("\r\n", "\n");
        assertEquals(expected, modelResult);
    }
}
