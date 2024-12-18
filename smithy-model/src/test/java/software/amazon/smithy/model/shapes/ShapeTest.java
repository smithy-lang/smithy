/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.Trait;

public class ShapeTest {

    private static class MyTrait implements Trait {
        private ShapeId id;
        private SourceLocation sourceLocation;

        MyTrait(ShapeId id, SourceLocation sourceLocation) {
            this.id = id;
            this.sourceLocation = sourceLocation != null ? sourceLocation : SourceLocation.none();
        }

        public ShapeId toShapeId() {
            return id;
        }

        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }

        @Override
        public Node toNode() {
            return Node.objectNode();
        }
    }

    private static class OtherTrait extends MyTrait {
        OtherTrait(ShapeId id, SourceLocation sourceLocation) {
            super(id, sourceLocation);
        }
    }

    private static class AnotherTrait extends OtherTrait {
        AnotherTrait(ShapeId id, SourceLocation sourceLocation) {
            super(id, sourceLocation);
        }
    }

    @Test
    public void requiresShapeId() {
        Assertions.assertThrows(IllegalStateException.class, () -> StringShape.builder().build());
    }

    @Test
    public void convertsShapeToBuilder() {
        Shape shape1 = StringShape.builder().id("ns.foo#baz").build();
        Shape shape2 = Shape.shapeToBuilder(shape1).build();
        StringShape shape3 = Shape.shapeToBuilder(StringShape.builder().id("ns.foo#baz").build()).build();

        assertThat(shape1, equalTo(shape2));
        assertThat(shape1, equalTo(shape3));
    }

    @Test
    public void castsToString() {
        Shape shape = StringShape.builder().id("ns.foo#baz").build();

        assertEquals("(string: `ns.foo#baz`)", shape.toString());
    }

    @Test
    public void hasSource() {
        Shape shape = StringShape.builder().id("ns.foo#baz").source("foo", 1, 2).build();

        assertEquals("foo", shape.getSourceLocation().getFilename());
        assertEquals(1, shape.getSourceLocation().getLine());
        assertEquals(2, shape.getSourceLocation().getColumn());
    }

    @Test
    public void sourceCannotBeNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StringShape.builder().source(null));
    }

    @Test
    public void hasTraits() {
        MyTrait trait = new MyTrait(ShapeId.from("foo.baz#foo"), null);
        MyTrait otherTrait = new OtherTrait(ShapeId.from("foo.baz#other"), null);
        ShapeId id = ShapeId.from("ns.foo#baz");
        DocumentationTrait documentationTrait = new DocumentationTrait("docs", SourceLocation.NONE);
        Shape shape = StringShape.builder()
                .id(id)
                .addTrait(trait)
                .addTrait(otherTrait)
                .addTrait(documentationTrait)
                .build();
        Model model = Model.builder()
                .addShapes(shape)
                .build();

        assertTrue(shape.getTrait(MyTrait.class).isPresent());
        assertTrue(shape.getMemberTrait(model, MyTrait.class).isPresent());
        assertEquals(shape.getTrait(MyTrait.class).get(), shape.expectTrait(MyTrait.class));

        assertTrue(shape.findTrait("foo.baz#foo").isPresent());
        assertTrue(shape.findMemberTrait(model, "foo.baz#foo").isPresent());

        assertTrue(shape.hasTrait("foo.baz#foo"));
        assertTrue(shape.getTrait(OtherTrait.class).isPresent());

        assertFalse(shape.getTrait(AnotherTrait.class).isPresent());
        assertFalse(shape.findTrait("notThere").isPresent());
        assertFalse(shape.hasTrait("notThere"));

        assertTrue(shape.getTrait(DocumentationTrait.class).isPresent());
        assertTrue(shape.hasTrait(DocumentationTrait.class));
        assertTrue(shape.findTrait("documentation").isPresent());
        assertTrue(shape.findTrait("smithy.api#documentation").isPresent());
        assertTrue(shape.findTrait("documentation").get() instanceof DocumentationTrait);

        Collection<Trait> traits = shape.getAllTraits().values();
        assertThat(traits, hasSize(3));
        assertThat(traits, hasItem(trait));
        assertThat(traits, hasItem(otherTrait));
        assertThat(traits, hasItem(documentationTrait));
    }

    @Test
    public void throwsWhenTraitNotFound() {
        Shape string = StringShape.builder().id("com.foo#example").build();

        Assertions.assertThrows(ExpectationNotMetException.class, () -> string.expectTrait(DeprecatedTrait.class));
    }

    @Test
    public void removesTraits() {
        MyTrait trait = new MyTrait(ShapeId.from("foo.baz#foo"), null);
        MyTrait otherTrait = new OtherTrait(ShapeId.from("foo.baz#other"), null);
        Shape shape = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(trait)
                .addTrait(otherTrait)
                .removeTrait("foo.baz#other")
                .build();

        assertThat(shape.getAllTraits(), hasKey(ShapeId.from("foo.baz#foo")));
        assertThat(shape.getAllTraits(), not(hasKey(ShapeId.from("foo.baz#other"))));
    }

    @Test
    public void differentShapeTypesAreNotEqual() {
        Shape shapeA = StringShape.builder().id("ns.foo#baz").build();
        Shape shapeB = TimestampShape.builder().id("ns.foo#baz").build();

        assertNotEquals(shapeA, shapeB);
    }

    @Test
    public void differentTypesAreNotEqual() {
        Shape shapeA = StringShape.builder().id("ns.foo#baz").build();

        assertNotEquals(shapeA, "");
    }

    @Test
    public void differentTraitNamesNotEqual() {
        MyTrait traitA = new MyTrait(ShapeId.from("foo.baz#foo"), null);
        MyTrait traitB = new MyTrait(ShapeId.from("foo.baz#other"), null);
        Shape shapeA = StringShape.builder().id("ns.foo#baz").addTrait(traitA).build();
        Shape shapeB = StringShape.builder().id("ns.foo#baz").addTrait(traitB).build();

        assertNotEquals(shapeA, shapeB);
    }

    @Test
    public void differentTraitsNotEqual() {
        MyTrait traitA = new MyTrait(ShapeId.from("foo.ba#foo"), null);
        MyTrait traitB = new OtherTrait(ShapeId.from("foo.baz#other"), null);
        Shape shapeA = StringShape.builder().id("ns.foo#baz").addTrait(traitA).build();
        Shape shapeB = StringShape.builder().id("ns.foo#baz").addTrait(traitB).build();

        assertNotEquals(shapeA, shapeB);
    }

    @Test
    public void differentIdsAreNotEqual() {
        Shape shapeA = StringShape.builder().id("ns.foo#baz").build();
        Shape shapeB = StringShape.builder().id("ns.foo#bar").build();

        assertNotEquals(shapeA, shapeB);
    }

    @Test
    public void sameInstanceIsEqual() {
        Shape shapeA = StringShape.builder().id("ns.foo#baz").build();

        assertEquals(shapeA, shapeA);
    }

    @Test
    public void sameValueIsEqual() {
        Shape shapeA = StringShape.builder().id("ns.foo#baz").build();
        Shape shapeB = StringShape.builder().id("ns.foo#baz").build();

        assertEquals(shapeA, shapeB);
    }

    @Test
    public void samesTraitsIsEqual() {
        MyTrait traitA = new MyTrait(ShapeId.from("foo.baz#foo"), null);
        Shape shapeA = StringShape.builder().id("ns.foo#baz").addTrait(traitA).build();
        Shape shapeB = StringShape.builder().id("ns.foo#baz").addTrait(traitA).build();

        assertEquals(shapeA, shapeB);
    }

    @Test
    public void validatesMemberShapeIds() {
        Assertions.assertThrows(SourceException.class, () -> {
            StructureShape.builder()
                    .id("ns.foo#bar")
                    .addMember(MemberShape.builder().id("ns.baz#Bar$boo").target(ShapeId.from("ns.foo#String")).build())
                    .build();
        });
    }
}
