/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.validators.TraitValueValidator;

public class PreludeTest {
    @Test
    public void checksIfShapeIdIsInPrelude() {
        assertTrue(Prelude.isPreludeShape(ShapeId.from("smithy.api#String")));
        assertTrue(Prelude.isPreludeShape(ShapeId.from("smithy.api#PrimitiveLong")));
        assertTrue(Prelude.isPreludeShape(ShapeId.from("smithy.api#Foo")));
        assertFalse(Prelude.isPreludeShape(ShapeId.from("foo.baz#Bar")));
    }

    @Test
    public void checksIfShapeIdIsInPublicPrelude() {
        assertTrue(Prelude.isPublicPreludeShape(ShapeId.from("smithy.api#String")));
        assertTrue(Prelude.isPublicPreludeShape(ShapeId.from("smithy.api#PrimitiveLong")));
        assertFalse(Prelude.isPublicPreludeShape(ShapeId.from("smithy.api#IdRefTrait")));
        assertFalse(Prelude.isPreludeShape(ShapeId.from("foo.baz#Bar")));
    }

    @Test
    public void checkIfPrivateShapesAreReferenced() {
        Model model = Prelude.getPreludeModel();

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.scrubTraitDefinitions(model);
        Set<ShapeId> unreferencedPrivateShapes = result.shapes()
                .filter(shape -> shape.hasTrait(PrivateTrait.class))
                .map(Shape::getId)
                .collect(Collectors.toSet());

        assertThat(unreferencedPrivateShapes, emptyCollectionOf(ShapeId.class));
    }

    /**
     * This test ensures that the prelude is valid.
     *
     * <p>Under normal circumstances, the prelude is not validated using the
     * {@link TraitValueValidator} when validating a model. This cuts down on
     * how much work this expensive validator needs to do. However, we still need
     * to make sure that changes made to the prelude itself is valid. That's the
     * job of this test. It sets a special property in the metadata of the model
     * to turn on prelude validation.
     */
    @Test
    public void ensurePreludeIsValid() {
        Model.assembler()
                .putMetadata(TraitValueValidator.VALIDATE_PRELUDE, Node.from(true))
                .assemble()
                // If the prelude is invalid, then this will throw an exception.
                .unwrap();
    }

    @Test
    public void preludeShapesAreAlwaysBoxed() {
        Model model = Model.assembler().assemble().unwrap();

        assertThat(model.expectShape(ShapeId.from("smithy.api#Boolean")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Byte")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Short")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Integer")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Long")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Float")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#Double")).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(ShapeId.from("smithy.api#PrimitiveBoolean")).hasTrait(BoxTrait.class), is(false));
    }
}
