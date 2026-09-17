/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.CreatesResourcesTrait;
import software.amazon.smithy.model.traits.DeletesResourcesTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

public class CleanResourceLifecycleReferencesTest {

    @Test
    public void removesEntriesWhenResourceIsRemoved() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("clean-lifecycle-references.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model,
                shape -> shape.getId().equals(ShapeId.from("com.example#Bar")));

        // Model should still be valid.
        assertFalse(Model.assembler().addModel(result).assemble().isBroken());

        // The operation should still have @createsResources but only with Foo.
        CreatesResourcesTrait trait = result.expectShape(ShapeId.from("com.example#CreateBoth"))
                .asOperationShape()
                .get()
                .expectTrait(CreatesResourcesTrait.class);
        assertThat(trait.getBindings(), hasSize(1));
        assertThat(trait.getBindings().get(0).getResource(), is(ShapeId.from("com.example#Foo")));
        // Cleanup preserves the retained binding's other members.
        assertThat(trait.getBindings().get(0).getIdentifiersFrom(), is(Optional.of("fooResult")));
    }

    @Test
    public void removesTraitEntirelyWhenAllEntriesRemoved() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("clean-lifecycle-references.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        // Remove both Foo and Bar.
        Model result = transformer.removeShapesIf(model,
                shape -> shape.getId().equals(ShapeId.from("com.example#Foo"))
                        || shape.getId().equals(ShapeId.from("com.example#Bar")));

        // Model should still be valid.
        assertFalse(Model.assembler().addModel(result).assemble().isBroken());

        // The operation should no longer have @createsResources at all.
        assertFalse(result.expectShape(ShapeId.from("com.example#CreateBoth"))
                .asOperationShape()
                .get()
                .findTrait(CreatesResourcesTrait.ID)
                .isPresent());
    }

    @Test
    public void cleansDeletesResourcesToo() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("clean-lifecycle-references.smithy"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.removeShapesIf(model,
                shape -> shape.getId().equals(ShapeId.from("com.example#Foo")));

        // Model should still be valid.
        assertFalse(Model.assembler().addModel(result).assemble().isBroken());

        // @deletesResources on DeleteFoo should be gone since Foo was removed.
        assertFalse(result.expectShape(ShapeId.from("com.example#DeleteFoo"))
                .asOperationShape()
                .get()
                .findTrait(DeletesResourcesTrait.ID)
                .isPresent());
    }
}
