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

package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumConstantBody;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.utils.ListUtils;

public class JsonSchemaConverterTest {
    @Test
    public void dealsWithRecursion() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("recursive.json"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.create().convert(model);

        assertThat(document.getDefinitions().keySet(), not(empty()));
    }

    @Test
    public void integrationTest() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.create().convert(model);

        assertThat(document.getDefinitions().keySet(), not(empty()));
    }

    @Test
    public void hasCopyConstructor() {
        Predicate<Shape> predicate = shape -> false;
        ObjectNode config = Node.objectNodeBuilder().withMember("foo", "bar").build();
        PropertyNamingStrategy propertyStrategy = (container, member, conf) -> "a";
        RefStrategy refStrategy = (member, conf) -> "#/foo/bar";

        JsonSchemaConverter.create()
                .config(config)
                .propertyNamingStrategy(propertyStrategy)
                .refStrategy(refStrategy)
                .shapePredicate(predicate)
                .copy();
    }

    @Test
    public void canFilterShapesWithCustomPredicate() {
        Predicate<Shape> predicate = shape -> !shape.getId().getName().equals("Foo");
        Model model = Model.builder()
                .addShape(StringShape.builder().id("smithy.example#Foo").build())
                .addShape(StringShape.builder().id("smithy.example#Baz").build())
                .build();
        SchemaDocument doc = JsonSchemaConverter.create().shapePredicate(predicate).convert(model);

        assertFalse(doc.getDefinition("#/definitions/SmithyExampleFoo").isPresent());
        assertTrue(doc.getDefinition("#/definitions/SmithyExampleBaz").isPresent());
    }

    @Test
    public void canUseCustomPropertyNamingStrategy() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .build();
        Model model = Model.builder().addShapes(struct, member, string).build();
        PropertyNamingStrategy strategy = (container, memberShape, conf) -> {
            return memberShape.getMemberName().toUpperCase(Locale.US);
        };
        SchemaDocument doc = JsonSchemaConverter.create().propertyNamingStrategy(strategy).convert(model);

        assertThat(doc.getDefinition("#/definitions/SmithyExampleFoo").get().getProperties().keySet(),
                   contains("BAR"));
    }

    @Test
    public void canUseCustomRefStrategy() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .build();
        Model model = Model.builder().addShapes(struct, member, string).build();
        RefStrategy strategy = (id, conf) -> "#/foo/" + id;
        SchemaDocument doc = JsonSchemaConverter.create().refStrategy(strategy).convert(model);

        assertThat(doc.getDefinitions().keySet(), containsInAnyOrder(
                "#/foo/smithy.example#Foo",
                "#/foo/smithy.example#Foo$bar",
                "#/foo/smithy.example#String"));
    }

    @Test
    public void canAddCustomSchemaMapper() {
        Model model = Model.builder().addShape(StringShape.builder().id("smithy.example#Foo").build()).build();
        JsonSchemaMapper mapper = (shape, builder, conf) -> builder.putExtension("Hi", Node.from("There"));
        SchemaDocument doc = JsonSchemaConverter.create().addMapper(mapper).convert(model);

        assertTrue(doc.getDefinition("#/definitions/SmithyExampleFoo").isPresent());
        assertTrue(doc.getDefinition("#/definitions/SmithyExampleFoo").get().getExtension("Hi").isPresent());
    }

    @Test
    public void excludesServiceShapes() {
        Model model = Model.builder()
                .addShape(ServiceShape.builder().id("smithy.example#Service").version("X").build())
                .build();
        SchemaDocument doc = JsonSchemaConverter.create().convert(model);

        assertThat(doc.getDefinitions().keySet(), empty());
    }

    @Test
    public void excludesPrivateShapes() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("smithy.example#String").addTrait(new PrivateTrait()).build())
                .build();
        SchemaDocument doc = JsonSchemaConverter.create().convert(model);

        assertThat(doc.getDefinitions().keySet(), empty());
    }

    @Test
    public void excludesMembersOfPrivateShapes() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .addTrait(new PrivateTrait())
                .build();
        Model model = Model.builder().addShapes(struct, member, string).build();
        SchemaDocument doc = JsonSchemaConverter.create().convert(model);

        assertThat(doc.getDefinitions().keySet(), contains("#/definitions/SmithyExampleString"));
    }

    @Test
    public void excludesMembersThatTargetPrivateShapes() {
        StringShape string = StringShape.builder().id("smithy.example#String").addTrait(new PrivateTrait()).build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder().id("smithy.example#Foo").addMember(member).build();
        Model model = Model.builder().addShapes(struct, member, string).build();
        SchemaDocument doc = JsonSchemaConverter.create().convert(model);

        // The member and the target are filtered out.
        assertThat(doc.getDefinitions().keySet(), contains("#/definitions/SmithyExampleFoo"));
    }

    @Test
    public void canIncludePrivateShapesWithFlag() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#Foo$bar").target(string).build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Foo")
                .addMember(member)
                .addTrait(new PrivateTrait())
                .build();
        Model model = Model.builder().addShapes(struct, member, string).build();
        ObjectNode config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.SMITHY_INCLUDE_PRIVATE_SHAPES, true)
                .build();
        SchemaDocument doc = JsonSchemaConverter.create().config(config).convert(model);

        assertThat(doc.getDefinitions().keySet(), not(empty()));
        assertThat(doc.getDefinitions().keySet(), containsInAnyOrder(
                "#/definitions/SmithyExampleFoo",
                "#/definitions/SmithyExampleFooBarMember",
                "#/definitions/SmithyExampleString"));
    }

    @Test
    public void addsExtensionsFromConfig() {
        Model model = Model.builder().build();
        ObjectNode config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.SCHEMA_DOCUMENT_EXTENSIONS, Node.objectNode()
                        .withMember("foo", Node.from("bar")))
                .build();
        SchemaDocument doc = JsonSchemaConverter.create().config(config).convert(model);

        assertThat(doc.getDefinitions().keySet(), empty());
        assertThat(doc.getExtension("foo").get(), equalTo(Node.from("bar")));
    }

    @Test
    public void convertsRootSchemas() {
        StringShape shape = StringShape.builder().id("smithy.example#String").build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
    }

    @Test
    public void convertsBlobToString() {
        BlobShape shape = BlobShape.builder().id("smithy.example#Blob").build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
    }

    @Test
    public void convertsNumbersToNumber() {
        List<Shape> shapes = ListUtils.of(
                ByteShape.builder().id("smithy.example#Number").build(),
                ShortShape.builder().id("smithy.example#Number").build(),
                IntegerShape.builder().id("smithy.example#Number").build(),
                LongShape.builder().id("smithy.example#Number").build(),
                FloatShape.builder().id("smithy.example#Number").build(),
                DoubleShape.builder().id("smithy.example#Number").build(),
                BigIntegerShape.builder().id("smithy.example#Number").build(),
                BigDecimalShape.builder().id("smithy.example#Number").build());

        for (Shape shape : shapes) {
            Model model = Model.builder().addShape(shape).build();
            SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

            assertThat(document.getRootSchema().getType().get(), equalTo("number"));
        }
    }

    @Test
    public void supportsRangeTrait() {
        IntegerShape shape = IntegerShape.builder()
                .id("smithy.example#Number")
                .addTrait(RangeTrait.builder().min(BigDecimal.valueOf(10)).max(BigDecimal.valueOf(100)).build())
                .build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("number"));
        assertThat(document.getRootSchema().getMinimum().get(), equalTo(BigDecimal.valueOf(10)));
        assertThat(document.getRootSchema().getMaximum().get(), equalTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void convertsBooleanToBoolean() {
        BooleanShape shape = BooleanShape.builder().id("smithy.example#Boolean").build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("boolean"));
    }

    @Test
    public void convertsListShapes() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder()
                .id("smithy.example#Collection$member")
                .target("smithy.api#String")
                .build();

        List<Shape> shapes = ListUtils.of(
                ListShape.builder().id("smithy.example#Collection").addMember(member).build(),
                SetShape.builder().id("smithy.example#Collection").addMember(member).build());

        for (Shape shape : shapes) {
            Model model = Model.builder().addShapes(string, shape, member).build();
            SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

            assertThat(document.getRootSchema().getType().get(), equalTo("array"));
            assertThat(document.getRootSchema().getItems().get().getRef().get(),
                       equalTo("#/definitions/SmithyExampleCollectionMember"));
            Schema memberDef = document.getDefinition("#/definitions/SmithyExampleCollectionMember").get();
            assertThat(memberDef.getType().get(), equalTo("string"));
        }
    }

    @Test
    public void supportsStringLengthTrait() {
        StringShape shape = StringShape.builder()
                .id("smithy.example#String")
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
        assertThat(document.getRootSchema().getMinLength().get(), equalTo(10L));
        assertThat(document.getRootSchema().getMaxLength().get(), equalTo(100L));
    }

    @Test
    public void supportsListAndSetLengthTrait() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder()
                .id("smithy.example#Collection$member")
                .target("smithy.api#String")
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();

        List<Shape> shapes = ListUtils.of(
                ListShape.builder().id("smithy.example#Collection").addMember(member).build(),
                SetShape.builder().id("smithy.example#Collection").addMember(member).build());

        for (Shape shape : shapes) {
            Model model = Model.builder().addShapes(string, shape, member).build();
            SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);
            Schema memberDef = document.getDefinition("#/definitions/SmithyExampleCollectionMember").get();

            assertThat(memberDef.getMinLength().get(), equalTo(10L));
            assertThat(memberDef.getMaxLength().get(), equalTo(100L));
        }
    }

    @Test
    public void supportsMapLengthTrait() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape key = MemberShape.builder().id("smithy.example#Map$key").target("smithy.api#String").build();
        MemberShape value = MemberShape.builder().id("smithy.example#Map$value").target("smithy.api#String").build();
        MapShape shape = MapShape.builder()
                .id("smithy.example#Map")
                .key(key)
                .value(value)
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();
        Model model = Model.builder().addShapes(string, shape, key, value).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);
        Schema schema = document.getRootSchema();

        assertThat(schema.getMinProperties().get(), equalTo(10));
        assertThat(schema.getMaxProperties().get(), equalTo(100));
    }

    @Test
    public void supportsUniqueItemsOnLists() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder().id("smithy.example#List$member").target("smithy.api#String").build();
        ListShape shape = ListShape.builder()
                .id("smithy.example#List")
                .addMember(member)
                .addTrait(new UniqueItemsTrait())
                .build();

        Model model = Model.builder().addShapes(string, shape, member).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, shape);

        assertTrue(document.getRootSchema().getUniqueItems());
    }

    @Test
    public void supportsPatternTrait() {
        String pattern = "^[A-Z]+$";
        StringShape string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new PatternTrait(pattern))
                .build();
        Model model = Model.builder().addShapes(string).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getPattern().get(), equalTo(pattern));
    }

    @Test
    public void supportsMediaType() {
        String mediaType = "application/json";
        StringShape string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new MediaTypeTrait(mediaType))
                .build();
        Model model = Model.builder().addShapes(string).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getContentMediaType().get(), equalTo(mediaType));
    }

    @Test
    public void supportsTitle() {
        String title = "Hello";
        StringShape string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new TitleTrait(title))
                .build();
        Model model = Model.builder().addShapes(string).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getTitle().get(), equalTo(title));
    }

    @Test
    public void supportsDocumentation() {
        String docs = "Hello";
        StringShape string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new DocumentationTrait(docs))
                .build();
        Model model = Model.builder().addShapes(string).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getDescription().get(), equalTo(docs));
    }

    @Test
    public void supportsEnum() {
        StringShape string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(EnumTrait.builder().addEnum("foo", EnumConstantBody.builder().build()).build())
                .build();
        Model model = Model.builder().addShapes(string).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getEnumValues().get(), contains("foo"));
    }

    @Test
    public void supportsUnionOneOf() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        UnionShape union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        Model model = Model.builder().addShapes(union, member, string).build();
        SchemaDocument document = JsonSchemaConverter.create().convert(model, union);
        Schema schema = document.getRootSchema();

        assertThat(schema.getOneOf(), hasSize(1));
        assertThat(schema.getOneOf().get(0).getRequired(), contains("foo"));
        assertThat(schema.getOneOf().get(0).getProperties().keySet(), contains("foo"));
    }

    @Test
    public void supportsUnionObject() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        UnionShape union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        Model model = Model.builder().addShapes(union, member, string).build();
        SchemaDocument document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder()
                                .withMember(JsonSchemaConstants.SMITHY_UNION_STRATEGY, "object")
                                .build())
                .convert(model, union);
        Schema schema = document.getRootSchema();

        assertThat(schema.getOneOf(), empty());
        assertThat(schema.getType().get(), equalTo("object"));
        assertThat(schema.getProperties().keySet(), empty());
    }

    @Test
    public void supportsUnionStructure() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        UnionShape union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        Model model = Model.builder().addShapes(union, member, string).build();
        SchemaDocument document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder()
                                .withMember(JsonSchemaConstants.SMITHY_UNION_STRATEGY, "structure")
                                .build())
                .convert(model, union);
        Schema schema = document.getRootSchema();

        assertThat(schema.getOneOf(), empty());
        assertThat(schema.getType().get(), equalTo("object"));
        assertThat(schema.getProperties().keySet(), contains("foo"));
    }

    @Test
    public void throwsForUnsupportUnionSetting() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            StringShape string = StringShape.builder().id("smithy.api#String").build();
            MemberShape member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
            UnionShape union = UnionShape.builder().id("a.b#Union").addMember(member).build();
            Model model = Model.builder().addShapes(union, member, string).build();
            JsonSchemaConverter.create()
                    .config(Node.objectNodeBuilder()
                                    .withMember(JsonSchemaConstants.SMITHY_UNION_STRATEGY, "not-valid")
                                    .build())
                    .convert(model, union);
        });
    }

    @Test
    public void dealsWithConflictsWithoutPollutingState() {
        Model model1 = Model.assembler()
                .addImport(getClass().getResource("recursive.json"))
                .assemble()
                .unwrap();

        StringShape stringShape = StringShape.builder().id("com.foo#String").build();
        MemberShape pageScriptsListMember = MemberShape.builder()
                .id("com.foo#PageScripts$member")
                .target(stringShape)
                .build();
        ListShape pageScripts = ListShape.builder()
                .id("com.foo#PageScripts")
                .member(pageScriptsListMember)
                .build();
        MemberShape pageScriptsMember = MemberShape.builder()
                .id("com.foo#Page$scripts")
                .target(stringShape)
                .build();
        StructureShape page = StructureShape.builder()
                .id("com.foo#Page")
                .addMember(pageScriptsMember)
                .build();
        Model model2 = Model.builder()
                .addShapes(page, pageScriptsMember, pageScripts, pageScriptsListMember, stringShape)
                .build();

        JsonSchemaConverter converter = JsonSchemaConverter.create();
        SchemaDocument document1 = converter.convert(model1);
        assertThat(document1.getDefinitions().keySet(), not(empty()));

        SchemaDocument document2 = converter.convert(model2);
        assertThat(document2.getDefinitions().keySet(), not(empty()));
        assertThat(document2.getDefinitions().keySet(), hasItem("#/definitions/ComFooPageScriptsMember"));
        assertThat(document2.getDefinitions().keySet(), hasItem("#/definitions/ComFooPageScriptsMember2"));
    }
}
