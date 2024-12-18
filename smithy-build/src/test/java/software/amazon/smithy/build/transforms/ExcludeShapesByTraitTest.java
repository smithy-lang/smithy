/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ExcludeShapesByTraitTest {
    @ParameterizedTest
    @MethodSource("traitValues")
    public void removesShapesByTrait(String trait) {
        Model model = Model.assembler()
                .addImport(getClass().getResource("internal-shapes.smithy"))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("traits", Node.fromStrings(trait)))
                .build();
        Model result = new ExcludeShapesByTrait().transform(context);

        // Structure removal also removes members.
        assertThat(result.getShapeIds(), not(hasItem(ShapeId.from("smithy.example#InternalStructure"))));
        assertThat(result.getShapeIds(), not(hasItem(ShapeId.from("smithy.example#InternalStructure$foo"))));

        // Can remove specific members from structures.
        assertThat(result.getShapeIds(), hasItem(ShapeId.from("smithy.example#ExternalStructure")));
        assertThat(result.getShapeIds(), hasItem(ShapeId.from("smithy.example#ExternalStructure$external")));
        assertThat(result.getShapeIds(), not(hasItem(ShapeId.from("smithy.example#ExternalStructure$internal"))));
    }

    public static List<String> traitValues() {
        return Arrays.asList(
                // Relative IDs are assumed to be in "smithy.api".
                "internal",
                // Absolute IDs are used as-is.
                "smithy.api#internal");
    }
}
