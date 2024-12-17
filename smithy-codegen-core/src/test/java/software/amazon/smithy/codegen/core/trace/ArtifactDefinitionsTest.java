/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class ArtifactDefinitionsTest {
    @Test
    void assertsToNodeWorksWithCorrectFields() {
        ArtifactDefinitions artifactDefinitions = ArtifactDefinitions.builder()
                .addType("t1", "t1val")
                .addType("t2", "t2val")
                .addTag("tag1", "tag1val")
                .addTag("tag2", "tag2val")
                .build();

        ObjectNode node = artifactDefinitions.toNode();

        ObjectNode builtNode = ObjectNode.objectNodeBuilder()
                .withMember(ArtifactDefinitions.TAGS_TEXT, ObjectNode.fromStringMap(artifactDefinitions.getTags()))
                .withMember(ArtifactDefinitions.TYPE_TEXT, ObjectNode.fromStringMap(artifactDefinitions.getTypes()))
                .build();

        Assertions.assertDoesNotThrow(() -> Node.assertEquals(node, builtNode));
    }

    @Test
    void assertsFromNodeWorksWithCorrectFields() {
        ArtifactDefinitions artifactDefinitions = ArtifactDefinitions.builder()
                .addType("t1", "t1val")
                .addType("t2", "t2val")
                .addTag("tag1", "tag1val")
                .addTag("tag2", "tag2val")
                .build();

        ObjectNode node = ObjectNode.objectNodeBuilder()
                .withMember(ArtifactDefinitions.TAGS_TEXT, ObjectNode.fromStringMap(artifactDefinitions.getTags()))
                .withMember(ArtifactDefinitions.TYPE_TEXT, ObjectNode.fromStringMap(artifactDefinitions.getTypes()))
                .build();

        ArtifactDefinitions artifactDefinitions2 = ArtifactDefinitions.fromNode(node);

        assertThat(artifactDefinitions.getTags(), equalTo(artifactDefinitions2.getTags()));
        assertThat(artifactDefinitions.getTypes(), equalTo(artifactDefinitions2.getTypes()));
    }

    @Test
    void assertsFromDefinitionsFileWorksWithRequiredFields() throws URISyntaxException, FileNotFoundException {
        ArtifactDefinitions artifactDefinitions =
                createFromFileHelper(getClass().getResource("definitions.json").toURI());

        ArtifactDefinitions artifactDefinitions2 = ArtifactDefinitions.builder()
                .addType("t1", "t1val")
                .addType("t2", "t2val")
                .addTag("tag1", "tag1val")
                .addTag("tag2", "tag2val")
                .build();

        assertThat(artifactDefinitions.getTags(), equalTo(artifactDefinitions2.getTags()));
        assertThat(artifactDefinitions.getTypes(), equalTo(artifactDefinitions2.getTypes()));
    }

    @Test
    void assertBuildThrowsWithoutRequiredTypesField() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ArtifactDefinitions.builder()
                    .addTag("tag1", "tag1val")
                    .addTag("tag2", "tag2val")
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredTagsField() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ArtifactDefinitions.builder()
                    .addType("t1", "t1val")
                    .addType("t2", "t2val")
                    .build();
        });
    }

    ArtifactDefinitions createFromFileHelper(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        return ArtifactDefinitions.fromNode(Node.parse(stream).expectObjectNode());
    }

}
