/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class IncludeClosuresTest {

    private Model loadModel() {
        return Model.assembler()
                .addImport(IncludeClosuresTest.class.getResource("include-closures-test.smithy"))
                .addImport(IncludeClosuresTest.class.getResource("include-closures-test-unrelated.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void filtersToShapesInClosure() {
        Model model = loadModel();
        Model result = ModelTransformer.create()
                .includeClosures(model, Collections.singletonList("com.example#Shapes"));

        // Renames are NOT applied by includeClosures.
        assertThat(result.getShape(ShapeId.from("com.example#Foo")), not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("com.example#Bar")), not(Optional.empty()));
        // Out-of-closure shape is removed.
        assertThat(result.getShape(ShapeId.from("com.unrelated#Unrelated")), is(Optional.empty()));
    }

    @Test
    public void throwsOnUnknownClosure() {
        Model model = loadModel();
        assertThrows(ModelTransformException.class,
                () -> ModelTransformer.create()
                        .includeClosures(model, ListUtils.of("com.example#Missing")));
    }
}
