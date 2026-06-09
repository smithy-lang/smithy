/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class IncludeClosuresTest {

    @Test
    public void filtersByClosure() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("include-closures.smithy"))
                .addImport(getClass().getResource("include-closures-unrelated.smithy"))
                .assemble()
                .unwrap();

        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("closures", Node.fromStrings("com.example#Shapes")))
                .build();

        Model result = new IncludeClosures().transform(context);

        // Renames are not applied by includeClosures.
        assertThat(result.getShape(ShapeId.from("com.example#Foo")), not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("com.example#Bar")), not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("com.unrelated#Unrelated")), is(Optional.empty()));
    }

    @Test
    public void requiresClosures() {
        Model model = Model.assembler().assemble().unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode())
                .build();

        assertThrows(SmithyBuildException.class, () -> new IncludeClosures().transform(context));
    }
}
