/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.utils.IoUtils;

public class ModelSerializerTest {
    @TestFactory
    public Stream<DynamicTest> generateV2RoundTripTests() throws IOException, URISyntaxException {
        return Files.list(Paths.get(
                SmithyIdlModelSerializer.class.getResource("ast-serialization/cases/v2").toURI()))
                .filter(path -> !path.toString().endsWith(".1.0.json"))
                .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> testRoundTripV2(path)));
    }

    private void testRoundTripV2(Path path) {
        testV2Serialization(path, path);
        testV1DowngradeSerialization(path, Paths.get(path.toString().replace(".json", ".1.0.json")));
    }

    @TestFactory
    public Stream<DynamicTest> generateV1RoundTripTests() throws IOException, URISyntaxException {
        return Files.list(Paths.get(
                SmithyIdlModelSerializer.class.getResource("ast-serialization/cases/v1").toURI()))
                .filter(path -> !path.toString().endsWith(".2.0.json"))
                .map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> testRoundTripV1(path)));
    }

    private void testRoundTripV1(Path path) {
        testV2Serialization(path, Paths.get(path.toString().replace(".json", ".2.0.json")));
        testV1DowngradeSerialization(path, path);
    }

    private void testV2Serialization(Path path, Path expectedV2Path) {
        Model model = Model.assembler().addImport(path).assemble().unwrap();
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode actual = serializer.serialize(model);
        ObjectNode expected = Node.parse(IoUtils.readUtf8File(expectedV2Path)).expectObjectNode();

        Node.assertEquals(actual, expected);
    }

    private void testV1DowngradeSerialization(Path path, Path expectedV1Path) {
        Model model = Model.assembler().addImport(path).assemble().unwrap();
        ObjectNode expectedDowngrade = Node.parse(IoUtils.readUtf8File(expectedV1Path)).expectObjectNode();
        ModelSerializer serializer1 = ModelSerializer.builder().version("1.0").build();
        ObjectNode model1 = serializer1.serialize(model);

        Node.assertEquals(model1, expectedDowngrade);
    }

    @Test
    public void serializesModels() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode serialized = serializer.serialize(model);
        String serializedString = Node.prettyPrintJson(serialized);

        Model other = Model.assembler()
                .addUnparsedModel("N/A", serializedString)
                .assemble()
                .unwrap();

        String serializedString2 = Node.prettyPrintJson(serializer.serialize(other));
        assertThat(serialized.expectMember("smithy").expectStringNode(), equalTo(Node.from(Model.MODEL_VERSION)));
        assertThat(serializedString, equalTo(serializedString2));
        assertThat(model, equalTo(other));
    }

    @Test
    public void filtersMetadata() {
        ModelSerializer serializer = ModelSerializer.builder()
                .metadataFilter(k -> k.equals("foo"))
                .build();
        Model model = Model.builder()
                .putMetadataProperty("foo", Node.from("baz"))
                .putMetadataProperty("bar", Node.from("qux"))
                .build();
        ObjectNode result = serializer.serialize(model);

        assertThat(result.getMember("metadata"), not(Optional.empty()));
        assertThat(result.getMember("metadata").get().expectObjectNode().getMember("foo"),
                equalTo(Optional.of(Node.from("baz"))));
        assertThat(result.getMember("metadata").get().expectObjectNode().getMember("bar"), is(Optional.empty()));
    }

    @Test
    public void filtersShapes() {
        ModelSerializer serializer = ModelSerializer.builder()
                .shapeFilter(shape -> shape.getId().getName().equals("foo"))
                .build();
        Model model = Model.builder()
                .addShape(StringShape.builder().id("ns.foo#foo").build())
                .addShape(StringShape.builder().id("ns.foo#baz").build())
                .build();
        ObjectNode result = serializer.serialize(model);

        ObjectNode shapes = result.expectObjectMember("shapes");
        assertThat(shapes.getMember("ns.foo#foo"), not(Optional.empty()));
        assertThat(shapes.getMember("ns.foo#baz"), is(Optional.empty()));
        assertThat(result.getMember("ns.foo#metadata"), is(Optional.empty()));
    }

    @Test
    public void canFilterTraits() {
        Shape shape = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(new SensitiveTrait())
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .build();
        Model model = Model.assembler().addShape(shape).assemble().unwrap();
        ModelSerializer serializer = ModelSerializer.builder()
                .traitFilter(trait -> trait.toShapeId().toString().equals("smithy.api#documentation"))
                .build();

        ObjectNode obj = serializer.serialize(model)
                .expectObjectMember("shapes")
                .expectObjectMember("ns.foo#baz");
        obj.expectStringMember("type");
        ObjectNode traits = obj.expectObjectMember("traits");
        assertThat(traits.expectStringMember("smithy.api#documentation"), equalTo(Node.from("docs")));
        assertThat(traits.getMember("smithy.api#sensitive"), is(Optional.empty()));
    }

    @Test
    public void serializesAliasedPreludeTraitsUsingFullyQualifiedFormWhenNecessary() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("prelude-trait-alias.smithy"))
                .assemble()
                .unwrap();
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode serialized = serializer.serialize(model);
        String result = Node.prettyPrintJson(serialized);

        // Make sure that we can serialize and deserialize the original model.
        Model roundTrip = Model.assembler()
                .addUnparsedModel("foo.json", result)
                .assemble()
                .unwrap();

        assertThat(model, equalTo(roundTrip));
        assertThat(result, containsString("\"ns.foo#sensitive\""));
        assertThat(result, containsString("\"smithy.api#sensitive\""));
        assertThat(result, containsString("\"smithy.api#deprecated\""));
    }

    @Test
    public void doesNotSerializePreludeTraitsOrShapes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode serialized = serializer.serialize(model);

        ObjectNode shapes = serialized.expectObjectMember("shapes");
        shapes.getMembers().forEach((key, value) -> {
            assertThat(key.getValue(), not(startsWith("smithy.api#")));
        });
    }

    @Test
    public void allowsDisablingPreludeFilter() {
        Model model = Model.assembler().assemble().unwrap();
        ModelSerializer serializer = ModelSerializer.builder().includePrelude(true).build();
        ObjectNode serialized = serializer.serialize(model);

        ObjectNode shapes = serialized.expectObjectMember("shapes");
        assertTrue(shapes.getMembers().size() > 1);
        shapes.getMembers().forEach((key, value) -> {
            assertThat(key.getValue(), startsWith("smithy.api#"));
        });
    }

    @Test
    public void doesNotSerializeEmptyServiceVersions() {
        ServiceShape service = ServiceShape.builder()
                .id("com.foo#Example")
                .build();
        Model model = Model.builder().addShape(service).build();
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode result = serializer.serialize(model);

        assertThat(NodePointer.parse("/shapes/com.foo#Example")
                .getValue(result)
                .expectObjectNode()
                .getStringMap(),
                not(hasKey("version")));
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

        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode result = serializer.serialize(model);

        assertTrue(NodePointer.parse("/shapes/com.foo#Str/traits").getValue(result).isNullNode());
    }

    @Test
    public void serializesSetsAsListsWithUniqueItems() {
        SetShape set = SetShape.builder()
                .id("smithy.example#Set")
                .member(ShapeId.from("smithy.example#String"))
                .build();
        Model model = Model.builder().addShape(set).build();
        Node node = ModelSerializer.builder().build().serialize(model);

        assertThat(NodePointer.parse("/shapes/smithy.example#Set/type")
                .getValue(node)
                .expectStringNode()
                .getValue(), equalTo("list"));
        assertThat(NodePointer.parse("/shapes/smithy.example#Set/traits/smithy.api#uniqueItems")
                .getValue(node)
                .isNullNode(), equalTo(false));
    }

    @Test
    public void serializesResourceProperties() {
        Map<String, ShapeId> properties = new TreeMap<>();
        properties.put("fooProperty", ShapeId.from("ns.foo#Shape"));
        ResourceShape shape = ResourceShape.builder()
                .id("ns.foo#Bar")
                .properties(properties)
                .build();
        Model model = Model.builder().addShape(shape).build();
        Node node = ModelSerializer.builder().build().serialize(model);
        Node expectedNode = Node.parse("{\"smithy\":\"2.0\",\"shapes\":{\"ns.foo#Bar\":" +
                "{\"type\":\"resource\",\"properties\":{\"fooProperty\":{\"target\":\"ns.foo#Shape\"}}}}}");
        Node.assertEquals(node, expectedNode);
    }

    @Test
    public void failsOnInvalidVersion() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelSerializer.builder().version("1.5").build();
        });
    }

    @Test
    public void failsWhenUsingV1WithPrelude() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ModelSerializer.builder().version("1.0").includePrelude(true).build();
        });
    }
}
