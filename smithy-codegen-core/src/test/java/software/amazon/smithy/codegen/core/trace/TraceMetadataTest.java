/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class TraceMetadataTest {

    @Test
    void assertsToNodeWorksWithRequiredFields() {
        TraceMetadata am = new TraceMetadata.Builder()
                .id("a")
                .version("b")
                .type("c")
                .timestamp("d")
                .homepage("hp")
                .typeVersion("tv")
                .build();

        ObjectNode node = am.toNode();

        assertThat(node.expectStringMember(TraceMetadata.ID_TEXT).getValue(), equalTo("a"));
        assertThat(node.expectStringMember(TraceMetadata.VERSION_TEXT).getValue(), equalTo("b"));
        assertThat(node.expectStringMember(TraceMetadata.TYPE_TEXT).getValue(), equalTo("c"));
        assertThat(node.expectStringMember(TraceMetadata.TIMESTAMP_TEXT).getValue(), equalTo("d"));
        assertThat(node.expectStringMember(TraceMetadata.HOMEPAGE_TEXT).getValue(), equalTo("hp"));
        assertThat(node.expectStringMember(TraceMetadata.TYPE_VERSION_TEXT).getValue(), equalTo("tv"));
    }

    @Test
    void assertsFromNodeWorksWithRequiredFields() {
        Node node = ObjectNode.objectNodeBuilder()
                .withMember(TraceMetadata.ID_TEXT, "id")
                .withMember(TraceMetadata.VERSION_TEXT, "version")
                .withMember(TraceMetadata.TYPE_TEXT, "type")
                .withMember(TraceMetadata.TIMESTAMP_TEXT, "timestamp")
                .withOptionalMember(TraceMetadata.TYPE_VERSION_TEXT, Optional.of("type").map(Node::from))
                .withOptionalMember(TraceMetadata.HOMEPAGE_TEXT, Optional.of("homepage").map(Node::from))
                .build();

        TraceMetadata am2 = TraceMetadata.fromNode(node);

        assertThat("id", equalTo(am2.getId()));
        assertThat("version", equalTo(am2.getVersion()));
        assertThat("timestamp", equalTo(am2.getTimestamp()));
        assertThat("type", equalTo(am2.getType()));
        assertThat("type", equalTo(am2.getTypeVersion().get()));
        assertThat("homepage", equalTo(am2.getHomepage().get()));
    }

    @Test
    void assertBuildThrowsWithoutRequiredId() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TraceMetadata am = new TraceMetadata.Builder()
                    .version("b")
                    .type("c")
                    .setTimestampAsNow()
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredVersion() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TraceMetadata am = new TraceMetadata.Builder()
                    .id("a")
                    .type("c")
                    .setTimestampAsNow()
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredType() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TraceMetadata am = new TraceMetadata.Builder()
                    .id("a")
                    .version("b")
                    .setTimestampAsNow()
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredTimestamp() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TraceMetadata am = new TraceMetadata.Builder()
                    .id("a")
                    .version("b")
                    .type("c")
                    .build();
        });
    }

}
