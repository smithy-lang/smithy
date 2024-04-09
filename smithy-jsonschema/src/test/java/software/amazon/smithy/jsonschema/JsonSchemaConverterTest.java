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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class JsonSchemaConverterTest {
    @Test
    public void dealsWithRecursion() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("recursive.json"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convert();
        assertThat(document.getDefinitions().keySet(), not(empty()));
    }

    @Test
    public void integrationTestV07() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convert();

        assertThat(document.getDefinitions().keySet(), not(empty()));

        Node expected = Node.parse(
                IoUtils.toUtf8String(getClass().getResourceAsStream("test-service.jsonschema.v07.json")));
        Node.assertEquals(document.toNode(), expected);
    }

    @Test
    public void integrationTestV2020_12() {
        Model model = Model.assembler()
            .addImport(getClass().getResource("test-service.json"))
            .assemble()
            .unwrap();

        JsonSchemaConfig testConfig = new JsonSchemaConfig();
        testConfig.setJsonSchemaVersion(JsonSchemaVersion.DRAFT2020_12);
        SchemaDocument document = JsonSchemaConverter.builder()
            .config(testConfig)
            .model(model).build().convert();

        assertThat(document.getDefinitions().keySet(), not(empty()));

        Node expected = Node.parse(
            IoUtils.toUtf8String(getClass().getResourceAsStream("test-service.jsonschema.v2020.json")));
        Node.assertEquals(document.toNode(), expected);
    }

    @Test
    public void canConvertShapesThatAreOnlyInTheClosureOfShape() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("multiple-closures.json"))
                .assemble()
                .unwrap();
        SchemaDocument document1 = JsonSchemaConverter.builder()
                .model(model)
                .rootShape(ShapeId.from("com.foo#StructureA"))
                .build()
                .convert();
        SchemaDocument document2 = JsonSchemaConverter.builder()
                .model(model)
                .rootShape(ShapeId.from("com.foo#StructureB"))
                .build()
                .convert();

        assertThat(document1.getDefinitions().keySet(), containsInAnyOrder("#/definitions/ReferencedA"));
        assertThat(document2.getDefinitions().keySet(), containsInAnyOrder("#/definitions/ReferencedB"));
    }

    @Test
    public void canConvertShapesThatHaveConflictsOutsideOfClosure() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("multiple-closures-with-conflicting-shapes.json"))
                .assemble()
                .unwrap();
        JsonSchemaConfig config1 = new JsonSchemaConfig();
        config1.setService(ShapeId.from("com.foo#ServiceA"));
        SchemaDocument document1 = JsonSchemaConverter.builder()
                .model(model)
                .rootShape(ShapeId.from("com.foo#ServiceA"))
                .config(config1)
                .build()
                .convert();

        JsonSchemaConfig config2 = new JsonSchemaConfig();
        config2.setService(ShapeId.from("com.bar#ServiceB"));
        SchemaDocument document2 = JsonSchemaConverter.builder()
                .model(model)
                .rootShape(ShapeId.from("com.bar#ServiceB"))
                .config(config2)
                .build()
                .convert();

        Schema string1Def = document1.getDefinitions().get("#/definitions/ConflictString");
        assertNotNull(string1Def);
        assertThat(string1Def.getEnumValues().get(), containsInAnyOrder("y", "z"));

        Schema string2Def = document2.getDefinitions().get("#/definitions/ConflictString");
        assertNotNull(string2Def);
        assertThat(string2Def.getEnumValues().get(), containsInAnyOrder("a", "b"));
    }

    @Test
    public void canFilterShapesWithCustomPredicate() {
        Predicate<Shape> predicate = shape -> !shape.getId().getName().equals("Foo");
        Model model = Model.builder()
                .addShape(StructureShape.builder().id("smithy.example#Foo").build())
                .addShape(StructureShape.builder().id("smithy.example#Baz").build())
                .build();
        SchemaDocument doc = JsonSchemaConverter.builder()
                .shapePredicate(predicate)
                .model(model)
                .build()
                .convert();

        assertFalse(doc.getDefinition("#/definitions/Foo").isPresent());
        assertTrue(doc.getDefinition("#/definitions/Baz").isPresent());
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
        SchemaDocument doc = JsonSchemaConverter.builder()
                .propertyNamingStrategy(strategy)
                .model(model)
                .build()
                .convert();

        assertThat(doc.getDefinition("#/definitions/Foo").get().getProperties().keySet(),
                   contains("BAR"));
    }

    @Test
    public void canAddCustomSchemaMapper() {
        Shape struct = StructureShape.builder().id("smithy.example#Foo").build();
        Model model = Model.builder().addShape(struct).build();
        class CustomMapper implements JsonSchemaMapper {
            @Override
            public Schema.Builder updateSchema(Shape shape, Schema.Builder builder, JsonSchemaConfig conf) {
                return builder.putExtension("Hi", Node.from("There"));
            }
        }
        JsonSchemaMapper mapper = new CustomMapper();
        SchemaDocument doc = JsonSchemaConverter.builder().addMapper(mapper).model(model).build().convert();

        assertTrue(doc.getDefinition("#/definitions/Foo").isPresent());
        assertTrue(doc.getDefinition("#/definitions/Foo").get().getExtension("Hi").isPresent());
    }

    @Test
    public void canAddCustomSchemaMapperContextMethod() {
        Shape struct = StructureShape.builder().id("smithy.example#Foo").build();
        Model model = Model.builder().addShape(struct).build();
        class CustomMapper implements JsonSchemaMapper {
            @Override
            public Schema.Builder updateSchema(JsonSchemaMapperContext context, Schema.Builder builder) {
                return builder.putExtension("Hi", Node.from("There"));
            }
        }
        JsonSchemaMapper mapper = new CustomMapper();
        SchemaDocument doc = JsonSchemaConverter.builder().addMapper(mapper).model(model).build().convert();

        assertTrue(doc.getDefinition("#/definitions/Foo").isPresent());
        assertTrue(doc.getDefinition("#/definitions/Foo").get().getExtension("Hi").isPresent());
    }

    @Test
    public void excludesServiceShapes() {
        Model model = Model.builder()
                .addShape(ServiceShape.builder().id("smithy.example#Service").version("X").build())
                .build();
        SchemaDocument doc = JsonSchemaConverter.builder().model(model).build().convert();

        assertThat(doc.getDefinitions().keySet(), empty());
    }

    @Test
    public void excludesPrivateShapes() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("smithy.example#String").addTrait(new PrivateTrait()).build())
                .build();
        SchemaDocument doc = JsonSchemaConverter.builder().model(model).build().convert();

        assertThat(doc.getDefinitions().keySet(), empty());
    }

    @Test
    public void addsExtensionsFromConfig() {
        Model model = Model.builder().build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setSchemaDocumentExtensions(Node.objectNode().withMember("foo", Node.from("bar")));
        SchemaDocument doc = JsonSchemaConverter.builder().config(config).model(model).build().convert();

        assertThat(doc.getDefinitions().keySet(), empty());
        assertThat(doc.getExtension("foo").get(), equalTo(Node.from("bar")));
    }

    @Test
    public void convertsRootSchemas() {
        StringShape shape = StringShape.builder().id("smithy.example#String").build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
    }

    @Test
    public void convertsBlobToString() {
        BlobShape shape = BlobShape.builder().id("smithy.example#Blob").build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(shape);

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
            SchemaDocument document = JsonSchemaConverter.builder().model(model).build()
                    .convertShape(shape);

            assertThat(document.getRootSchema().getType().get(), equalTo("number"));
        }
    }

    @Test
    public void convertsIntegersWhenConfigSet() {
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setUseIntegerType(true);
        Model model = Model.assembler()
                .addImport(getClass().getResource("integer-types.smithy"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.builder()
                .model(model)
                .config(config)
                .build()
                .convert();

        Node expected = Node.parse(
                IoUtils.toUtf8String(getClass().getResourceAsStream("integer-types.jsonschema.v07.json")));
        Node.assertEquals(document.toNode(), expected);
    }

    @Test
    public void supportsRangeTrait() {
        IntegerShape shape = IntegerShape.builder()
                .id("smithy.example#Number")
                .addTrait(RangeTrait.builder().min(BigDecimal.valueOf(10)).max(BigDecimal.valueOf(100)).build())
                .build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build()
                .convertShape(shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("number"));
        assertThat(document.getRootSchema().getMinimum().get(), equalTo(BigDecimal.valueOf(10)));
        assertThat(document.getRootSchema().getMaximum().get(), equalTo(BigDecimal.valueOf(100)));
    }

    @Test
    public void convertsBooleanToBoolean() {
        BooleanShape shape = BooleanShape.builder().id("smithy.example#Boolean").build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("boolean"));
    }

    @Test
    public void supportsStringLengthTrait() {
        StringShape shape = StringShape.builder()
                .id("smithy.example#String")
                .addTrait(LengthTrait.builder().min(10L).max(100L).build())
                .build();
        Model model = Model.builder().addShape(shape).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(shape);

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
        assertThat(document.getRootSchema().getMinLength().get(), equalTo(10L));
        assertThat(document.getRootSchema().getMaxLength().get(), equalTo(100L));
    }

    @Test
    @SuppressWarnings("deprecation")
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
            Model model = Model.builder().addShapes(shape, string).build();
            SchemaDocument document = JsonSchemaConverter.builder()
                    .model(model)
                    .build()
                    .convertShape(shape);

            Schema def = document.getRootSchema();

            assertTrue(def.getItems().isPresent());
            Schema items = def.getItems().get();

            assertThat(items.getMinLength().get(), equalTo(10L));
            assertThat(items.getMaxLength().get(), equalTo(100L));
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
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(shape);
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
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(shape);

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
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(string);
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
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(string);
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
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(string);
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
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getDescription().get(), equalTo(docs));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void supportsEnum() {
        StringShape string = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(EnumTrait.builder().addEnum(EnumDefinition.builder().value("foo").build()).build())
                .build();
        Model model = Model.builder().addShapes(string).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(string);
        Schema schema = document.getRootSchema();

        assertThat(schema.getEnumValues().get(), contains("foo"));
    }

    @Test
    public void supportsUnionOneOf() {
        StringShape string = StringShape.builder().id("smithy.api#String").build();
        MemberShape member = MemberShape.builder().id("a.b#Union$foo").target("smithy.api#String").build();
        UnionShape union = UnionShape.builder().id("a.b#Union").addMember(member).build();
        Model model = Model.builder().addShapes(union, member, string).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(union);
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
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setUnionStrategy(JsonSchemaConfig.UnionStrategy.OBJECT);
        SchemaDocument document = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convertShape(union);
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
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setUnionStrategy(JsonSchemaConfig.UnionStrategy.STRUCTURE);
        SchemaDocument document = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convertShape(union);
        Schema schema = document.getRootSchema();

        assertThat(schema.getOneOf(), empty());
        assertThat(schema.getType().get(), equalTo("object"));
        assertThat(schema.getProperties().keySet(), contains("foo"));
    }

    @Test
    public void supportsMapPropertyNames() {
        ShapeId shapeId = ShapeId.from("smithy.api#String");
        StringShape string = StringShape.builder().id(shapeId).build();
        MapShape map = MapShape.builder().id("a.b#Map").key(shapeId).value(shapeId).build();
        Model model = Model.builder().addShapes(map, string).build();
        SchemaDocument document = JsonSchemaConverter.builder().model(model).build().convertShape(map);
        Schema schema = document.getRootSchema();

        assertTrue(schema.getPropertyNames().isPresent());
        assertThat(schema.getPropertyNames().get().getType().get(), equalTo("string"));
        assertTrue(schema.getAdditionalProperties().isPresent());
        assertThat(schema.getAdditionalProperties().get().getType().get(), equalTo("string"));
    }

    @Test
    public void supportsMapPatternProperties() {
        ShapeId shapeId = ShapeId.from("smithy.api#String");
        StringShape string = StringShape.builder().id(shapeId).build();
        String pattern = "[a-z]{1,16}";
        StringShape key = StringShape.builder().id("a.b#Key")
                .addTrait(new PatternTrait(pattern)).build();
        MapShape map = MapShape.builder().id("a.b#Map").key(key.getId()).value(shapeId).build();
        Model model = Model.builder().addShapes(map, key, string).build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setMapStrategy(JsonSchemaConfig.MapStrategy.PATTERN_PROPERTIES);
        SchemaDocument document = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convertShape(map);
        Schema schema = document.getRootSchema();

        assertThat(schema.getPatternProperties().size(), equalTo(1));
        assertTrue(schema.getPatternProperties().containsKey(pattern));
        assertThat(schema.getPatternProperties().get(pattern).getType().get(), equalTo("string"));
    }

    @Test
    public void supportsMapPatternPropertiesWithDefaultPattern() {
        ShapeId shapeId = ShapeId.from("smithy.api#String");
        StringShape string = StringShape.builder().id(shapeId).build();
        MapShape map = MapShape.builder().id("a.b#Map").key(shapeId).value(shapeId).build();
        Model model = Model.builder().addShapes(map, string).build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setMapStrategy(JsonSchemaConfig.MapStrategy.PATTERN_PROPERTIES);
        SchemaDocument document = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convertShape(map);
        Schema schema = document.getRootSchema();

        assertThat(schema.getPatternProperties().size(), equalTo(1));
        assertTrue(schema.getPatternProperties().containsKey(".+"));
        assertThat(schema.getPatternProperties().get(".+").getType().get(), equalTo("string"));
    }

    @Test
    public void sensitiveTraitHasNoImpact() {
        StringShape string1 = StringShape.builder()
                .id("smithy.api#String")
                .addTrait(new SensitiveTrait())
                .build();
        Model model1 = Model.builder().addShapes(string1).build();
        SchemaDocument document1 = JsonSchemaConverter.builder().model(model1).build().convertShape(string1);

        StringShape string2 = StringShape.builder()
                .id("smithy.api#String")
                .build();
        Model model2 = Model.builder().addShapes(string2).build();
        SchemaDocument document2 = JsonSchemaConverter.builder().model(model2).build().convertShape(string2);

        assertThat(document1, equalTo(document2));
    }

    @Test
    public void convertingToBuilderGivesSameResult() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        JsonSchemaConverter converter = JsonSchemaConverter.builder()
                .model(model)
                .rootShape(ShapeId.from("example.rest#RestService"))
                .build();

        SchemaDocument document1 = converter.convert();
        SchemaDocument document2 = converter.toBuilder().build().convert();
        assertThat(document1, equalTo(document2));

        // Make sure the tricky null handling of setting rootShape works.
        JsonSchemaConverter converter2 = converter.toBuilder().rootShape(null).build();
        SchemaDocument document3 = converter2.convert();
        SchemaDocument document4 = converter2.toBuilder().build().convert();
        assertThat(document3, equalTo(document4));
    }

    @Test
    public void canGetAndSetExtensionsAsPojo() {
        Ext ext = new Ext();
        ext.setBaz("hi");
        ext.setFoo(true);
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.putExtensions(ext);
        Ext ext2 = config.getExtensions(Ext.class);

        assertThat(ext2.getBaz(), equalTo("hi"));
        assertThat(ext2.isFoo(), equalTo(true));
    }

    public static final class Ext {
        private boolean foo;
        private String baz;

        public boolean isFoo() {
            return foo;
        }

        public void setFoo(boolean foo) {
            this.foo = foo;
        }

        public String getBaz() {
            return baz;
        }

        public void setBaz(String baz) {
            this.baz = baz;
        }
    }

    @Test
    public void removesMixins() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("model-with-mixins.smithy"))
                .assemble()
                .unwrap();
        JsonSchemaConverter converter = JsonSchemaConverter.builder()
                .model(model)
                .build();

        NodePointer mixin = NodePointer.parse("/definitions/Mixin");
        NodePointer properties = NodePointer.parse("/definitions/UsesMixin/properties");
        SchemaDocument document = converter.convert();

        // Mixin isn't there.
        assertThat(mixin.getValue(document.toNode()), equalTo(Node.nullNode()));

        // The mixin was flattened.
        assertThat(properties.getValue(document.toNode()).expectObjectNode().getStringMap().keySet(),
                   containsInAnyOrder("foo", "baz"));
    }

    @Test
    public void appliesDefaultsByDefault() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("default-values.smithy"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.builder()
                .model(model)
                .build()
                .convert();

        Node expected = Node.parse(
                IoUtils.toUtf8String(getClass().getResourceAsStream("default-values.jsonschema.v07.json")));
        Node.assertEquals(document.toNode(), expected);
    }

    @Test
    public void defaultsCanBeDisabled() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("default-values.smithy"))
                .assemble()
                .unwrap();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setDisableDefaultValues(true);
        SchemaDocument document = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convert();

        Node expected = Node.parse(
                IoUtils.toUtf8String(getClass().getResourceAsStream("default-values-disabled.jsonschema.v07.json")));
        Node.assertEquals(document.toNode(), expected);
    }

    @Test
    public void supportsIntEnumsByDefault() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("int-enums.smithy"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.builder()
                .model(model)
                .build()
                .convert();

        Node expected = Node.parse(
                IoUtils.toUtf8String(getClass().getResourceAsStream("int-enums.jsonschema.v07.json")));
        Node.assertEquals(document.toNode(), expected);
    }

    @Test
    public void intEnumsCanBeDisabled() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("int-enums.smithy"))
                .assemble()
                .unwrap();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setDisableIntEnums(true);
        SchemaDocument document = JsonSchemaConverter.builder()
                .config(config)
                .model(model)
                .build()
                .convert();

        Node expected = Node.parse(
                IoUtils.toUtf8String(getClass().getResourceAsStream("int-enums-disabled.jsonschema.v07.json")));
        Node.assertEquals(document.toNode(), expected);
    }
}
