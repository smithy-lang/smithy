/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;

public class RemoveShapesTest {

    private static final ShapeId STRING_TARGET = ShapeId.from("ns.foo#String");
    private static Model mixinsModel;

    @BeforeAll
    public static void before() {
        mixinsModel = Model.assembler()
                .addImport(RemoveShapesTest.class.getResource("mixin-removal/model.smithy"))
                .assemble()
                .unwrap();
    }

    private void assertContainerMembersAreRemoved(Shape container, List<Shape> members) {
        Model.Builder builder = Model.builder()
                .addShape(container)
                .addShape(StringShape.builder().id(STRING_TARGET).build());
        members.forEach(builder::addShape);
        Model model = builder.build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Collections.singleton(container));

        assertThat(result.shapes().count(), Matchers.equalTo(1L));
        assertThat(result.getShape(STRING_TARGET), Matchers.not(Optional.empty()));
        assertThat(result.getShape(container.getId()), Matchers.is(Optional.empty()));
    }

    @Test
    public void removesListMemberWhenRemoved() {
        MemberShape member = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$member"))
                .target(STRING_TARGET)
                .build();
        ListShape container = ListShape.builder().id(ShapeId.from("ns.foo#Container")).member(member).build();
        assertContainerMembersAreRemoved(container, Collections.singletonList(member));
    }

    @Test
    public void removesMapMembersWhenRemoved() {
        MemberShape key = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$key"))
                .target(STRING_TARGET)
                .build();
        MemberShape value = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$value"))
                .target(STRING_TARGET)
                .build();
        MapShape container = MapShape.builder().id(ShapeId.from("ns.foo#Container")).key(key).value(value).build();
        assertContainerMembersAreRemoved(container, Arrays.asList(key, value));
    }

    @Test
    public void removesEnumMembersWhenRemoved() {
        EnumShape container = EnumShape.builder()
                .id(ShapeId.from("ns.foo#Enum"))
                .addMember("foo", "foo")
                .build();
        assertContainerMembersAreRemoved(container, new ArrayList<>(container.members()));
    }

    @Test
    public void notAllEnumMembersCanBeRemoved() {
        EnumShape container = EnumShape.builder()
                .id(ShapeId.from("ns.foo#Enum"))
                .addMember("foo", "foo")
                .build();

        Model.Builder builder = Model.builder()
                .addShape(container)
                .addShape(StringShape.builder().id(STRING_TARGET).build());
        container.members().forEach(builder::addShape);
        Model model = builder.build();
        ModelTransformer transformer = ModelTransformer.create();
        assertThrows(SourceException.class, () -> transformer.removeShapes(model, container.members()));
    }

    @Test
    public void removesIntEnumMembersWhenRemoved() {
        IntEnumShape container = IntEnumShape.builder()
                .id(ShapeId.from("ns.foo#Enum"))
                .addMember("foo", 1)
                .build();
        assertContainerMembersAreRemoved(container, new ArrayList<>(container.members()));
    }

    @Test
    public void notAllIntEnumMembersCanBeRemoved() {
        IntEnumShape container = IntEnumShape.builder()
                .id(ShapeId.from("ns.foo#Enum"))
                .addMember("foo", 1)
                .build();

        Model.Builder builder = Model.builder()
                .addShape(container)
                .addShape(StringShape.builder().id(STRING_TARGET).build());
        container.members().forEach(builder::addShape);
        Model model = builder.build();
        ModelTransformer transformer = ModelTransformer.create();
        assertThrows(SourceException.class, () -> transformer.removeShapes(model, container.members()));
    }

    @Test
    public void removesStructureMembersWhenRemoved() {
        MemberShape a = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$a"))
                .target(STRING_TARGET)
                .build();
        MemberShape b = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$b"))
                .target(STRING_TARGET)
                .build();
        StructureShape container = StructureShape.builder()
                .id(ShapeId.from("ns.foo#Container"))
                .addMember(a)
                .addMember(b)
                .build();
        assertContainerMembersAreRemoved(container, Arrays.asList(a, b));
    }

    @Test
    public void removesTaggedUnionMembersWhenRemoved() {
        MemberShape a = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$a"))
                .target(STRING_TARGET)
                .build();
        MemberShape b = MemberShape.builder()
                .id(ShapeId.from("ns.foo#Container$b"))
                .target(STRING_TARGET)
                .build();
        UnionShape container = UnionShape.builder()
                .id(ShapeId.from("ns.foo#Container"))
                .addMember(a)
                .addMember(b)
                .build();
        assertContainerMembersAreRemoved(container, Arrays.asList(a, b));
    }

    @Test
    public void removesOperationsFromResourcesWhenOperationRemoved() {
        ResourceShape container = ResourceShape.builder()
                .id(ShapeId.from("ns.foo#Container"))
                .addOperation("ns.foo#A")
                .create(ShapeId.from("ns.foo#B"))
                .addOperation("ns.foo#C")
                .build();
        OperationShape a = OperationShape.builder()
                .id("ns.foo#A")
                .addTrait(new ReadonlyTrait())
                .build();
        OperationShape b = OperationShape.builder().id("ns.foo#B").build();
        OperationShape c = OperationShape.builder().id("ns.foo#C").build();

        Model model = Model.builder().addShapes(container, a, b, c).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Arrays.asList(a, b));

        assertThat(result.shapes().count(), Matchers.equalTo(2L));
        assertThat(result.getShape(container.getId()), Matchers.not(Optional.empty()));
        assertThat(result.getShape(c.getId()), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(container.getId()).asResourceShape().get().getOperations(),
                Matchers.contains(c.getId()));
    }

    @Test
    public void removesTraitsFromAuthDefinitionWhenReferenceRemoved() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("remove-shapes.json"))
                .assemble()
                .unwrap();
        ShapeId removedId = ShapeId.from("ns.foo#bar");
        Shape removedShape = model.expectShape(removedId);

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Collections.singletonList(removedShape));

        ShapeId subjectId = ShapeId.from("ns.foo#auth");
        Shape subject = result.expectShape(subjectId);
        AuthDefinitionTrait trait = subject.getTrait(AuthDefinitionTrait.class).get();

        assertFalse(trait.getTraits().contains(removedId));
    }

    @Test
    public void removesTraitsFromProtocolDefinitionWhenReferenceRemoved() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("remove-shapes.json"))
                .assemble()
                .unwrap();
        ShapeId removedId = ShapeId.from("ns.foo#baz");
        Shape removedShape = model.expectShape(removedId);

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Collections.singletonList(removedShape));

        ShapeId subjectId = ShapeId.from("ns.foo#protocol");
        Shape subject = result.expectShape(subjectId);
        ProtocolDefinitionTrait trait = subject.getTrait(ProtocolDefinitionTrait.class).get();

        assertFalse(trait.getTraits().contains(removedId));
    }

    @Test
    public void removingShapeUpdatesServiceRename() {
        StringShape string = StringShape.builder().id("example.foo#A").build();
        ServiceShape service = ServiceShape.builder()
                .id("com.foo#Example")
                .version("1")
                .putRename(ShapeId.from("example.foo#A"), "AA")
                .build();
        Model model = Model.builder().addShapes(string, service).build();

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapes(model, Collections.singletonList(string));
        ServiceShape updatedService = result.expectShape(service.getId(), ServiceShape.class);

        assertThat(updatedService.getRename().keySet(), empty());
    }

    @Test
    public void removingMixinsRemovesThemFromShapes() {
        ModelTransformer transformer = ModelTransformer.create();
        Model.Builder builder = Model.builder();
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#Mixin2")
                .addMember("b", string.getId())
                .addTrait(MixinTrait.builder().build())
                .build();
        StructureShape mixin3 = StructureShape.builder()
                .id("smithy.example#Mixin3")
                .addMember("c", string.getId())
                .addTrait(MixinTrait.builder().build())
                .addMixin(mixin2)
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addMember("d", string.getId())
                .addMixin(mixin1)
                .addMixin(mixin3)
                .build();
        builder.addShapes(mixin1, mixin2, mixin3, concrete);
        Model model = builder.build();

        Model result1 = transformer.removeShapes(model, Collections.singletonList(mixin3));
        assertThat(result1.expectShape(concrete.getId(), StructureShape.class).getAllMembers(), not(hasKey("c")));
        assertThat(result1.expectShape(concrete.getId(), StructureShape.class).getAllMembers(), not(hasKey("b")));
        assertThat(result1.expectShape(concrete.getId(), StructureShape.class).getAllMembers(), hasKey("a"));
        assertThat(result1.expectShape(concrete.getId(), StructureShape.class).getAllMembers(), hasKey("d"));
        assertThat(result1.getShape(mixin3.getId()), equalTo(Optional.empty()));
        assertThat(result1.getShape(mixin2.getId()), equalTo(Optional.of(mixin2)));

        Model result2 = transformer.removeShapes(model, Collections.singletonList(mixin2));
        assertThat(result2.getShape(mixin2.getId()), equalTo(Optional.empty()));

        Model result3 = transformer.removeShapes(model, Collections.singletonList(mixin1));
        assertThat(result3.getShape(mixin1.getId()), equalTo(Optional.empty()));
        assertThat(result3.expectShape(concrete.getId(), StructureShape.class).getAllMembers(), not(hasKey("a")));
        assertThat(result3.expectShape(concrete.getId(), StructureShape.class).getAllMembers(), hasKey("d"));
    }

    @ParameterizedTest
    @MethodSource("removeMixinData")
    public void RemoveMixinsTest(String mixinFile, String[] shapeNamesToRemove) {
        Model start = mixinsModel;

        Collection<Shape> shapesToRemove = new ArrayList<>(shapeNamesToRemove.length);
        for (String name : shapeNamesToRemove) {
            shapesToRemove.add(start.expectShape(ShapeId.from("smithy.example#" + name)));
        }

        Model result = ModelTransformer.create().removeShapes(start, shapesToRemove);
        Model expected = Model.assembler()
                .addImport(RemoveShapesTest.class.getResource("mixin-removal/" + mixinFile))
                .assemble()
                .unwrap();
        ModelSerializer serializer = ModelSerializer.builder().build();

        Node.assertEquals(serializer.serialize(result), serializer.serialize(expected));
    }

    public static Collection<Object[]> removeMixinData() {
        return Arrays.asList(new Object[][] {
                {"without-a.smithy", new String[] {"A"}},
                {"without-a2.smithy", new String[] {"A2"}},
                {"without-a3.smithy", new String[] {"A3"}},
                {"without-a-a2.smithy", new String[] {"A", "A2"}},
                {"without-a-a2-a3.smithy", new String[] {"A", "A2", "A3"}},
                {"without-a-a2-a3-b-b2-b3.smithy", new String[] {"A", "A2", "A3", "B", "B2", "B3"}},
                {"without-a-b.smithy", new String[] {"A", "B"}},
                {"without-b.smithy", new String[] {"B"}},
                {"without-b2.smithy", new String[] {"B2"}},
                {"without-b3.smithy", new String[] {"B3"}},
                {"without-c.smithy", new String[] {"C"}},
                {"without-d.smithy", new String[] {"D"}}
        });
    }
}
