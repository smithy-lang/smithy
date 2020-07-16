package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ArtifactMetadataTest {

    @Test
    void assertsToNodeWorksWithRequiredFields() {
        ArtifactMetadata am = new ArtifactMetadata.Builder()
                .id("a")
                .version("b")
                .type("c")
                .timestamp("d")
                .homepage("hp")
                .typeVersion("tv")
                .build();

        ObjectNode node = am.toNode();

        assertThat(node.expectStringMember(ArtifactMetadata.ID_TEXT).getValue(), equalTo("a"));
        assertThat(node.expectStringMember(ArtifactMetadata.VERSION_TEXT).getValue(), equalTo("b"));
        assertThat(node.expectStringMember(ArtifactMetadata.TYPE_TEXT).getValue(), equalTo("c"));
        assertThat(node.expectStringMember(ArtifactMetadata.TIMESTAMP_TEXT).getValue(), equalTo("d"));
        assertThat(node.expectStringMember(ArtifactMetadata.HOMEPAGE_TEXT).getValue(), equalTo("hp"));
        assertThat(node.expectStringMember(ArtifactMetadata.TYPE_VERSION_TEXT).getValue(), equalTo("tv"));
    }

    @Test
    void assertsFromNodeWorksWithRequiredFields() {
        Node node = ObjectNode.objectNodeBuilder()
                .withMember(ArtifactMetadata.ID_TEXT, "id")
                .withMember(ArtifactMetadata.VERSION_TEXT, "version")
                .withMember(ArtifactMetadata.TYPE_TEXT, "type")
                .withMember(ArtifactMetadata.TIMESTAMP_TEXT, "timestamp")
                .withOptionalMember(ArtifactMetadata.TYPE_VERSION_TEXT, Optional.of("type").map(Node::from))
                .withOptionalMember(ArtifactMetadata.HOMEPAGE_TEXT, Optional.of("homepage").map(Node::from))
                .build();

        ArtifactMetadata am2 = ArtifactMetadata.createFromNode(node);

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
            ArtifactMetadata am = new ArtifactMetadata.Builder()
                    .version("b")
                    .type("c")
                    .setTimestampAsNow()
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredVersion() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ArtifactMetadata am = new ArtifactMetadata.Builder()
                    .id("a")
                    .type("c")
                    .setTimestampAsNow()
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredType() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ArtifactMetadata am = new ArtifactMetadata.Builder()
                    .id("a")
                    .version("b")
                    .setTimestampAsNow()
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredTimestamp() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ArtifactMetadata am = new ArtifactMetadata.Builder()
                    .id("a")
                    .version("b")
                    .type("c")
                    .build();
        });
    }

}
