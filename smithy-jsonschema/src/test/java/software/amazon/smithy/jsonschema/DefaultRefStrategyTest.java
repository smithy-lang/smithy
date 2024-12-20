/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static software.amazon.smithy.utils.FunctionalUtils.alwaysTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

public class DefaultRefStrategyTest {

    private PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();

    @Test
    public void usesDefaultPointer() {
        RefStrategy ref = RefStrategy.createDefaultStrategy(
                Model.builder().build(),
                new JsonSchemaConfig(),
                propertyNamingStrategy,
                alwaysTrue());
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/definitions/Foo"));
    }

    @Test
    public void usesCustomPointerAndAppendsSlashWhenNecessary() {
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setDefinitionPointer("#/components/schemas");
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(Model.builder().build(), config, propertyNamingStrategy, alwaysTrue());
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/components/schemas/Foo"));
    }

    @Test
    public void usesCustomPointerAndOmitsSlashWhenNecessary() {
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setDefinitionPointer("#/components/schemas");
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(Model.builder().build(), config, propertyNamingStrategy, alwaysTrue());
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/components/schemas/Foo"));
    }

    @Test
    public void stripsNonAlphanumericCharactersWhenRequested() {
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setAlphanumericOnlyRefs(true);
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(Model.builder().build(), config, propertyNamingStrategy, alwaysTrue());
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo_Bar"));

        assertThat(pointer, equalTo("#/definitions/FooBar"));
    }

    @Test
    public void addsListAndSetMembers() {
        StringShape string = StringShape.builder().id("foo.bar#String").build();
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Scripts$member")
                .target("foo.bar#String")
                .build();
        ListShape list = ListShape.builder()
                .id("foo.bar#Scripts")
                .member(member)
                .build();
        Model model = Model.builder().addShapes(string, list, member).build();
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());
        String pointer = ref.toPointer(member.getId());

        assertThat(pointer, equalTo("#/definitions/Scripts/items"));
    }

    @Test
    public void addsMapMembers() {
        StringShape string = StringShape.builder().id("foo.bar#String").build();
        MemberShape key = MemberShape.builder()
                .id("foo.bar#Scripts$key")
                .target("foo.bar#String")
                .build();
        MemberShape value = MemberShape.builder()
                .id("foo.bar#Scripts$value")
                .target("foo.bar#String")
                .build();
        MapShape map = MapShape.builder()
                .id("foo.bar#Scripts")
                .key(key)
                .value(value)
                .build();
        Model model = Model.builder().addShapes(string, map, key, value).build();
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());

        assertThat(ref.toPointer(key.getId()), equalTo("#/definitions/Scripts/propertyNames"));
        assertThat(ref.toPointer(value.getId()), equalTo("#/definitions/Scripts/additionalProperties"));
    }

    @Test
    public void addsStructureMembers() {
        StringShape string = StringShape.builder().id("foo.bar#String").build();
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Scripts$pages")
                .target("foo.bar#String")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("foo.bar#Scripts")
                .addMember(member)
                .build();
        Model model = Model.builder().addShapes(string, struct, member).build();
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());

        assertThat(ref.toPointer(struct.getId()), equalTo("#/definitions/Scripts"));
        assertThat(ref.toPointer(member.getId()), equalTo("#/definitions/Scripts/properties/pages"));
    }

    @Test
    public void usesRefForStructureMembers() {
        StructureShape baz = StructureShape.builder()
                .id("foo.bar#Baz")
                .addMember("bam", ShapeId.from("foo.bar#Bam"))
                .build();
        StructureShape bam = StructureShape.builder()
                .id("foo.bar#Bam")
                .build();
        Model model = Model.builder().addShapes(baz, bam).build();
        RefStrategy ref = RefStrategy
                .createDefaultStrategy(model, new JsonSchemaConfig(), propertyNamingStrategy, alwaysTrue());

        assertThat(ref.toPointer(baz.getMember("bam").get().getId()), equalTo("#/definitions/Bam"));
    }

    @Test
    public void usesServiceRenames() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-renames.json"))
                .assemble()
                .unwrap();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setService(ShapeId.from("smithy.example#MyService"));
        RefStrategy ref = RefStrategy.createDefaultStrategy(model, config, propertyNamingStrategy, alwaysTrue());

        assertThat(ref.toPointer(ShapeId.from("smithy.example#Widget")), equalTo("#/definitions/Widget"));
        assertThat(ref.toPointer(ShapeId.from("foo.example#Widget")), equalTo("#/definitions/FooWidget"));
    }
}
