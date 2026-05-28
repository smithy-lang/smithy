/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ShapeClosureIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class FlattenClosureNamespacesTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(FlattenClosureNamespacesTest.class.getResource("flatten-closure-namespaces.smithy"))
                .addImport(
                        FlattenClosureNamespacesTest.class.getResource("flatten-closure-namespaces-unrelated.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void renamesClosureShapesAndLeavesOthersAlone() {
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("namespace", "com.flat")
                        .withMember("closure", "com.example#Shapes"))
                .build();

        Model result = new FlattenClosureNamespaces().transform(context);

        // Rename map contributes the new name, the namespace is the target.
        assertTrue(result.getShape(ShapeId.from("com.flat#RenamedFoo")).isPresent());
        // Other in-closure shapes are flattened into the target namespace.
        assertTrue(result.getShape(ShapeId.from("com.flat#Bar")).isPresent());
        // Original ids in the closure are gone.
        assertFalse(result.getShape(ShapeId.from("com.example#Foo")).isPresent());
        assertFalse(result.getShape(ShapeId.from("com.example#Bar")).isPresent());
        // Out-of-closure shape is preserved untouched.
        assertTrue(result.getShape(ShapeId.from("com.unrelated#Unrelated")).isPresent());
    }

    @Test
    public void requiresNamespaceAndClosure() {
        TransformContext missingClosure = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("namespace", "com.flat"))
                .build();
        assertThrows(SmithyBuildException.class, () -> new FlattenClosureNamespaces().transform(missingClosure));

        TransformContext missingNamespace = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("closure", "com.example#Shapes"))
                .build();
        assertThrows(SmithyBuildException.class, () -> new FlattenClosureNamespaces().transform(missingNamespace));
    }

    @Test
    public void throwsWhenClosureIsUnknown() {
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("namespace", "com.flat")
                        .withMember("closure", "com.example#Missing"))
                .build();

        assertThrows(SmithyBuildException.class, () -> new FlattenClosureNamespaces().transform(context));
    }

    @Test
    public void rewritesClosureRenameKeysToTrackRenamedShapeIds() {
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("namespace", "com.flat")
                        .withMember("closure", "com.example#Shapes"))
                .build();

        Model result = new FlattenClosureNamespaces().transform(context);

        // RenameShapes serializes the model, including metadata, then rewrites
        // any string node whose value matches a renamed shape id. The rename
        // map's key (com.example#Foo) is rewritten to its new id, just like
        // service rename keys round-trip through the same machinery.
        ArrayNode shapeClosures = result.getMetadata().get("shapeClosures").expectArrayNode();
        List<Node> closures = shapeClosures.getElements();
        ObjectNode closure = closures.get(0).expectObjectNode();
        ObjectNode rename = closure.expectObjectMember("rename");
        assertTrue(rename.getStringMember("com.flat#RenamedFoo").isPresent());
        assertFalse(rename.getStringMember("com.example#Foo").isPresent());
    }

    @Test
    public void canFilterPreludeShapesFromClosure() {
        ShapeClosureIndex index = ShapeClosureIndex.of(model);

        boolean withPreludeHasString = false;
        for (Shape s : index.getShapesInClosure("com.example#Shapes", true)) {
            if (s.getId().equals(ShapeId.from("smithy.api#String"))) {
                withPreludeHasString = true;
                break;
            }
        }
        boolean withoutPreludeHasString = false;
        for (Shape s : index.getShapesInClosure("com.example#Shapes", false)) {
            if (s.getId().equals(ShapeId.from("smithy.api#String"))) {
                withoutPreludeHasString = true;
                break;
            }
        }

        assertTrue(withPreludeHasString);
        assertFalse(withoutPreludeHasString);
    }
}
