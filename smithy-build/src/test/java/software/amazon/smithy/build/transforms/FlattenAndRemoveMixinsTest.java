/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

public class FlattenAndRemoveMixinsTest {

    @Test
    void compareTransform() {
        Model before = Model.assembler()
                .addImport(FlattenAndRemoveMixinsTest.class.getResource("flatten-and-remove-mixins.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().flattenAndRemoveMixins(before);

        assertTrue(result.getShapesWithTrait(MixinTrait.class).isEmpty());
        assertThat(result.expectShape(ShapeId.from("smithy.example#Foo"), StructureShape.class).getMemberNames(),
                contains("bar", "foo"));
    }
}
