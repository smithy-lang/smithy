/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;

public class ModelTest {

    @Test
    public void buildsModel() {
        Model model = Model.builder()
                .putMetadataProperty("name.name", Node.objectNode())
                .addShape(StringShape.builder()
                        .id("smithy.example#String")
                        .addTrait(TraitDefinition.builder().build())
                        .build())
                .build();

        assertTrue(model.getMetadataProperty("name.name").isPresent());
        assertThat(model.getShapesWithTrait(TraitDefinition.class), hasSize(1));
    }

    @Test
    public void modelEquality() {
        Model modelA = Model.builder()
                .putMetadataProperty("foo", Node.from("baz"))
                .addShape(StringShape.builder().id("ns.foo#baz").build())
                .build();
        Model modelB = Model.builder()
                .putMetadataProperty("foo", Node.from("baz"))
                .build();

        assertThat(modelA, equalTo(modelA));
        assertThat(modelA, not(equalTo(modelB)));
        assertThat(modelA, not(equalTo(null)));
    }

    @Test
    public void modelEqualityExamples() {
        StructureShape opInput = StructureShape.builder()
                .id(ShapeId.fromParts("foo", "FooInput"))
                .addMember("test", ShapeId.from("smithy.api#String"))
                .build();
        Supplier<OperationShape> op = () -> {
            ExamplesTrait.Example example = ExamplesTrait.Example.builder()
                    .title("anything")
                    .input(ObjectNode.builder().withMember("test", StringNode.from("something")).build())
                    .build();
            ExamplesTrait examples = ExamplesTrait.builder().addExample(example).build();
            return OperationShape.builder()
                    .id(ShapeId.fromParts("foo", "Foo"))
                    .input(opInput)
                    .addTrait(examples)
                    .build();
        };

        Model modelA = Model.builder()
                .addShape(op.get())
                .build();
        Model modelB = Model.builder()
                .addShape(op.get())
                .build();

        assertThat(modelA, equalTo(modelA));
        assertThat(modelA, equalTo(modelB));
    }

    @Test
    public void successfullyExpectsShapesOfType() {
        StringShape shape = StringShape.builder().id("ns.foo#A").build();
        Model model = Model.builder().addShape(shape).build();

        assertThat(model.expectShape(ShapeId.from("ns.foo#A"), StringShape.class), equalTo(shape));
    }

    @Test
    public void throwsIfShapeNotOfRightType() {
        StringShape shape = StringShape.builder().id("ns.foo#A").build();
        Model model = Model.builder().addShape(shape).build();

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            model.expectShape(ShapeId.from("ns.foo#A"), IntegerShape.class);
        });
    }

    @Test
    public void hasShapes() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        Model model = Model.builder().addShape(a).build();

        assertTrue(model.getShape(ShapeId.from("ns.foo#a")).isPresent());
        assertFalse(model.getShape(ShapeId.from("ns.foo#baz")).isPresent());
    }

    @Test
    public void getsShapesAsType() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        Model model = Model.builder()
                .addShape(a)
                .addShape(b)
                .addShape(c)
                .build();
        List<StringShape> shapes = model.shapes(StringShape.class).collect(Collectors.toList());

        assertThat(shapes, hasSize(2));
        assertThat(shapes, hasItem(a));
        assertThat(shapes, hasItem(b));
    }

    @Test
    public void createsModelFromCollection() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        Model model = Model.builder().addShapes(Arrays.asList(a, b, c)).build();
        List<Shape> shapes = model.shapes().collect(Collectors.toList());

        assertThat(shapes, hasSize(3));
        assertThat(shapes, hasItem(a));
        assertThat(shapes, hasItem(b));
        assertThat(shapes, hasItem(c));
    }

    @Test
    public void canRemoveShapeById() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        Model model = Model.builder()
                .addShapes(Arrays.asList(a, b))
                .removeShape(ShapeId.from("ns.foo#a"))
                .build();
        List<Shape> shapes = model.shapes().collect(Collectors.toList());

        assertThat(shapes, hasSize(1));
        assertThat(shapes, hasItem(b));
        assertThat(shapes, not(hasItem(a)));
    }

    @Test
    public void createsModelFromModel() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        Model modelA = Model.builder().addShapes(Arrays.asList(a, b, c)).build();
        Model modelB = Model.builder().addShapes(modelA).build();
        List<Shape> shapesA = modelA.shapes().collect(Collectors.toList());
        List<Shape> shapesB = modelB.shapes().collect(Collectors.toList());

        assertEquals(shapesA, shapesB);
        assertEquals(modelA, modelB);
        assertEquals(modelA.hashCode(), modelB.hashCode());
    }

    @Test
    public void comparesModel() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        Model modelA = Model.builder().addShapes(Arrays.asList(a, b, c)).build();
        Model modelB = Model.builder().addShapes(modelA).build();

        assertEquals(modelA, modelB);
    }

    @Test
    public void differentiatesModels() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        Model modelA = Model.builder().addShapes(Arrays.asList(a, b)).build();
        Model modelB = Model.builder().addShape(a).build();

        assertNotEquals(modelA, modelB);
        assertNotEquals(modelA.hashCode(), modelB.hashCode());
    }

    @Test
    public void computesHashCode() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        Model model = Model.builder().addShape(a).build();

        assertThat(model.hashCode(), not(0));
    }

    @Test
    public void convertsToSet() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        Model model = Model.builder().addShapes(a, b).build();

        assertThat(model.toSet(), containsInAnyOrder(a, b));
    }

    @Test
    public void toSetWithTypeRespectsSubclassing() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        EnumShape.Builder enumBuilder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#b");
        EnumShape b = enumBuilder.addMember("FOO", "foo").build();
        Model model = Model.builder().addShapes(a, b).build();

        assertThat(model.toSet(StringShape.class), containsInAnyOrder(a, b));
    }

    @Test
    public void addsMembersAutomatically() {
        StringShape string = StringShape.builder().id("ns.foo#a").build();
        ListShape list = ListShape.builder()
                .id("ns.foo#list")
                .member(ShapeId.from("ns.foo#a"))
                .build();
        Model model = Model.builder().addShapes(string, list).build();
        Set<Shape> shapes = model.toSet();

        assertThat(shapes, hasSize(3));
        assertThat(shapes, containsInAnyOrder(string, list, list.getMember()));
    }

    @Test
    public void removesMembersAutomatically() {
        StringShape string = StringShape.builder().id("ns.foo#a").build();
        ListShape list = ListShape.builder()
                .id("ns.foo#list")
                .member(ShapeId.from("ns.foo#a"))
                .build();
        Model model = Model.builder()
                .addShapes(string, list)
                .removeShape(list.getId())
                .build();
        Set<Shape> shapes = model.toSet();

        assertThat(shapes, hasSize(1));
        assertThat(shapes, contains(string));
    }

    @Test
    public void syntheticTraitsCanBeQueriedLikeNormalTraits() {
        ShapeId originalId = ShapeId.from("com.foo.nested#Str");
        StringShape stringShape = StringShape.builder()
                .id("com.foo#Str")
                .addTrait(new OriginalShapeIdTrait(originalId))
                .build();
        Model model = Model.builder()
                .addShape(stringShape)
                .build();

        assertThat(model.getShapesWithTrait(OriginalShapeIdTrait.class), contains(stringShape));
    }

    /**
     * This test ensures that the old deprecated behavior of using reflection
     * to create a KnowledgeIndex is maintained (for now at least).
     */
    @Test
    public void canCreateKnowledgeIndexUsingReflection() {
        Model model = Model.builder().build();
        model.getKnowledge(TopDownIndex.class);
    }

    @Test
    public void doesNotDeadlockWhenReenteringBlackboard() {
        Model model = Model.builder().build();
        model.getKnowledge(FooFooFoo.class, FooFooFoo::new);
    }

    private static final class FooFooFoo implements KnowledgeIndex {
        public FooFooFoo(Model model) {
            model.getKnowledge(Baz.class, Baz::new);
        }
    }

    private static final class Baz implements KnowledgeIndex {
        public Baz(Model model) {
            model.getKnowledge(Bar.class, Bar::new);
        }
    }

    private static final class Bar implements KnowledgeIndex {
        public Bar(Model model) {
            model.getKnowledge(Qux.class, Qux::new);
        }
    }

    private static final class Qux implements KnowledgeIndex {
        public Qux(Model model) {
            HttpBindingIndex.of(model);
        }
    }
}
