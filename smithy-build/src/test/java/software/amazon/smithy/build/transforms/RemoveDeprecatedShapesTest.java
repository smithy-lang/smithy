/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class RemoveDeprecatedShapesTest {
    @Test
    public void removesAllDeprecatedShapes() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("remove-deprecated.smithy").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("relativeDate", Node.from("2024-10-10"))
                        .withMember("relativeVersion", Node.from("1.1.0")))
                .build();
        Model result = new RemoveDeprecatedShapes().transform(context);

        // Deprecated by date removed
        assertFalse(result.getShape(ShapeId.from("smithy.example#FilteredBeforeHyphens")).isPresent());
        assertFalse(result.getShape(ShapeId.from("smithy.example#FilteredVersionBefore")).isPresent());

        // Equal to the filter retained
        assertTrue(result.getShape(ShapeId.from("smithy.example#NotFilteredDateEquals")).isPresent());
        assertTrue(result.getShape(ShapeId.from("smithy.example#NotFilteredVersionEquals")).isPresent());

        // After filter retained
        assertTrue(result.getShape(ShapeId.from("smithy.example#NotFilteredDateAfter")).isPresent());
        assertTrue(result.getShape(ShapeId.from("smithy.example#NotFilteredVersionAfter")).isPresent());
    }
}
