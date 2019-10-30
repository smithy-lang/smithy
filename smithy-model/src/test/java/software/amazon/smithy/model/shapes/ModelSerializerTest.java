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

package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class ModelSerializerTest {
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
                .shapeIndex(ShapeIndex.builder().build())
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
                .shapeIndex(ShapeIndex.builder()
                        .addShape(StringShape.builder().id("ns.foo#foo").build())
                        .addShape(StringShape.builder().id("ns.foo#baz").build())
                        .build())
                .build();
        ObjectNode result = serializer.serialize(model);

        assertThat(result.getMember("ns.foo"), not(Optional.empty()));
        assertThat(result.getMember("ns.foo").get().expectObjectNode().getMember("shapes"), not(Optional.empty()));
        ObjectNode shapes = result.getMember("ns.foo").get()
                .expectObjectNode()
                .expectMember("shapes")
                .expectObjectNode();
        assertThat(shapes.getMember("foo"), not(Optional.empty()));
        assertThat(shapes.getMember("baz"), is(Optional.empty()));
        assertThat(result.getMember("metadata"), is(Optional.empty()));
    }

    @Test
    public void canFilterTraits() {
        Shape shape = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .build();
        Model model = Model.assembler().addShape(shape).assemble().unwrap();
        ModelSerializer serializer = ModelSerializer.builder()
                .traitFilter(trait -> trait.toShapeId().toString().equals("smithy.api#documentation"))
                .build();
        ObjectNode obj = serializer.serialize(model)
                .expectMember("ns.foo").expectObjectNode()
                .expectMember("shapes").expectObjectNode()
                .expectMember("baz").expectObjectNode();

        assertThat(obj.getMember("type"), is(Optional.of(Node.from("string"))));
        assertThat(obj.getMember("smithy.api#documentation"), equalTo(Optional.of(Node.from("docs"))));
        assertThat(obj.getMember("smithy.api#sensitive"), is(Optional.empty()));
    }

    @Test
    public void serializesAliasedPreludeTraitsUsingFullyQualifiedFormWhenNecessary() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("prelude-trait-alias.json"))
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

        assertFalse(serialized.getMember("smithy.api").isPresent());
    }

    @Test
    public void serializesAbsoluteShapeIdsOnlyWhenNeeded() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode serialized = serializer.serialize(model);

        ObjectNode resourceWithRelativeIds = serialized.expectObjectMember("ns.foo")
                .expectObjectMember("shapes")
                .expectObjectMember("MyResource");
        ObjectNode resourceWithAbsoluteIds = serialized.expectObjectMember("ns.foo")
                .expectObjectMember("shapes")
                .expectObjectMember("ResourceNeedingAbsoluteShapeIds");
        ObjectNode structureWithAbsoluteIds = serialized.expectObjectMember("ns.resource.needing.ids")
                .expectObjectMember("shapes")
                .expectObjectMember("GetResourceNeedingAbsoluteShapeIdsInput");
        ObjectNode structureWithMixedIds = serialized.expectObjectMember("ns.foo")
                .expectObjectMember("shapes")
                .expectObjectMember("Structure");

        assertThat(resourceWithAbsoluteIds.expectObjectMember("identifiers").expectStringMember("id").getValue(),
                   equalTo("ns.baz#String"));
        assertThat(resourceWithAbsoluteIds.expectStringMember("read").getValue(),
                   equalTo("ns.resource.needing.ids#GetResourceNeedingAbsoluteShapeIds"));

        assertThat(resourceWithRelativeIds.expectObjectMember("identifiers").expectStringMember("id").getValue(),
                   equalTo("MyResourceId"));
        assertThat(resourceWithRelativeIds.expectStringMember("put").getValue(), equalTo("PutMyResource"));
        assertThat(resourceWithRelativeIds.expectStringMember("read").getValue(), equalTo("GetMyResource"));
        assertThat(resourceWithRelativeIds.expectStringMember("delete").getValue(), equalTo("DeleteMyResource"));
        assertThat(resourceWithRelativeIds.expectArrayMember("collectionOperations")
                           .get(0).get().expectStringNode().getValue(),
                   equalTo("BatchDeleteMyResource"));

        assertThat(structureWithAbsoluteIds.expectObjectMember("members").expectObjectMember("id")
                           .expectStringMember("target").getValue(),
                   equalTo("ns.baz#String"));

        assertThat(structureWithMixedIds.expectObjectMember("members").expectObjectMember("a")
                           .expectStringMember("target").getValue(),
                   equalTo("MyString"));
        assertThat(structureWithMixedIds.expectObjectMember("members").expectObjectMember("c")
                           .expectStringMember("target").getValue(),
                   equalTo("ns.shapes#String"));
    }
}
