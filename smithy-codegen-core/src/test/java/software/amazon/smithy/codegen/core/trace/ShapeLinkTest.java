/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class ShapeLinkTest {

    @Test
    void assertsToNodeWorksWithAllFields() {
        ShapeLink shapeLink = ShapeLink.builder()
                .addTag("tag")
                .file("file")
                .id("id")
                .type("type")
                .line(1)
                .column(2)
                .build();

        ObjectNode node = shapeLink.toNode();

        assertThat(node.getStringMember(ShapeLink.TYPE_TEXT).get().getValue(), equalTo("type"));
        assertThat(node.getNumberMember(ShapeLink.LINE_TEXT).get().getValue(), equalTo(1));
        assertThat(node.getArrayMember(ShapeLink.TAGS_TEXT)
                .get()
                .get(0)
                .get()
                .expectStringNode()
                .getValue(),
                equalTo("tag"));
    }

    @Test
    void assertsFromNodeWorksWithAllFields() {
        ArrayList<String> tags = new ArrayList<>();
        tags.add("tag");

        Node node = ObjectNode.objectNodeBuilder()
                .withMember(ShapeLink.ID_TEXT, "id")
                .withMember(ShapeLink.TYPE_TEXT, "type")
                .withOptionalMember(ShapeLink.TAGS_TEXT, Optional.of(tags).map(Node::fromStrings))
                .withOptionalMember(ShapeLink.FILE_TEXT, Optional.of("file").map(Node::from))
                .withOptionalMember(ShapeLink.LINE_TEXT, Optional.of(1).map(Node::from))
                .withOptionalMember(ShapeLink.COLUMN_TEXT, Optional.of(2).map(Node::from))
                .build();

        ShapeLink shapeLink2 = ShapeLink.fromNode(node);

        assertThat(Optional.of(2), equalTo(shapeLink2.getColumn()));
        assertThat(Optional.of(1), equalTo(shapeLink2.getLine()));
        assertThat("id", equalTo(shapeLink2.getId()));
        assertThat(Optional.of("file"), equalTo(shapeLink2.getFile()));
        assertThat(tags, equalTo(shapeLink2.getTags()));
        assertThat("type", equalTo(shapeLink2.getType()));
    }

    @Test
    void assertBuildThrowsWithoutRequiredTypesField() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ShapeLink.builder()
                    .addTag("tag")
                    .file("file")
                    .id("id")
                    .line(1)
                    .column(2)
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredIdField() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ShapeLink.builder()
                    .addTag("tag")
                    .file("file")
                    .line(1)
                    .type("type")
                    .column(2)
                    .build();
        });
    }

}
