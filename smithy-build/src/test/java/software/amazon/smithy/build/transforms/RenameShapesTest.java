/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.FunctionalUtils;

public class RenameShapesTest {

    @Test
    public void renameShapes() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("rename-shapes.json").toURI()))
                .assemble()
                .unwrap();
        ObjectNode renamed = Node.objectNode()
                .withMember("ns.foo#Bar", "ns.foo#Baz")
                .withMember("ns.foo#Qux", "ns.foo#Corge");
        ObjectNode config = Node.objectNode()
                .withMember("renamed", renamed);
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Model result = new RenameShapes().transform(context);
        List<String> ids = result.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(Shape::getId)
                .map(Object::toString)
                .collect(Collectors.toList());

        assertThat(ids, containsInAnyOrder("ns.foo#MyService", "ns.foo#Baz", "ns.foo#Corge", "ns.foo#Corge$foo"));
        assertThat(ids, not(containsInAnyOrder("ns.foo#Bar", "ns.foo#Qux")));
    }

    @Test
    public void throwsWhenRenamedPropertyIsNotConfigured() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("badConfig", Node.from("foo")))
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new RenameShapes().transform(context));
    }

    @Test
    public void throwsWhenRenamedPropertyMapIsEmpty() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("renamed", Node.nullNode()))
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new RenameShapes().transform(context));
    }

    @Test
    public void throwsWhenShapeToBeRenamedIsNotFound() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        ObjectNode renamed = Node.objectNode()
                .withMember("ns.foo#Bar", "ns.foo#Baz");
        ObjectNode config = Node.objectNode()
                .withMember("renamed", renamed);
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new RenameShapes().transform(context));
    }

    @Test
    public void throwsWhenFromShapeIsInvalid() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        ObjectNode renamed = Node.objectNode()
                .withMember("Bar", "ns.foo#Baz");
        ObjectNode config = Node.objectNode()
                .withMember("renamed", renamed);
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new RenameShapes().transform(context));
    }

    @Test
    public void throwsWhenToShapeIsInvalid() {
        Model model = Model.assembler()
                .addUnparsedModel("N/A", "{ \"smithy\": \"1.0\" }")
                .assemble()
                .unwrap();
        ObjectNode renamed = Node.objectNode()
                .withMember("ns.foo#Bar", "Baz");
        ObjectNode config = Node.objectNode()
                .withMember("renamed", renamed);
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();
        Assertions.assertThrows(SmithyBuildException.class, () -> new RenameShapes().transform(context));
    }
}
