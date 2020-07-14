package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class DefinitionsTest {
    @Test
    void assertsToNodeWorksWithCorrectFields() {
        Definitions definitions = Definitions.builder()
                .addType("t1", "t1val")
                .addType("t2", "t2val")
                .addTag("tag1", "tag1val")
                .addTag("tag2", "tag2val")
                .build();

        ObjectNode node = definitions.toNode();

        ObjectNode builtNode = ObjectNode.objectNodeBuilder()
                .withMember(Definitions.TAGS_TEXT, ObjectNode.fromStringMap(definitions.getTags()))
                .withMember(Definitions.TYPE_TEXT, ObjectNode.fromStringMap(definitions.getTypes()))
                .build();

        Assertions.assertDoesNotThrow(() -> Node.assertEquals(node, builtNode));
    }

    @Test
    void assertsFromNodeWorksWithCorrectFields() {
        Definitions definitions = Definitions.builder()
                .addType("t1", "t1val")
                .addType("t2", "t2val")
                .addTag("tag1", "tag1val")
                .addTag("tag2", "tag2val")
                .build();

        ObjectNode node = ObjectNode.objectNodeBuilder()
                .withMember(Definitions.TAGS_TEXT, ObjectNode.fromStringMap(definitions.getTags()))
                .withMember(Definitions.TYPE_TEXT, ObjectNode.fromStringMap(definitions.getTypes()))
                .build();

        Definitions definitions2 = Definitions.createFromNode(node);

        assertThat(definitions.getTags(), equalTo(definitions2.getTags()));
        assertThat(definitions.getTypes(), equalTo(definitions2.getTypes()));
    }

    @Test
    void assertsFromDefinitionsFileWorksWithRequiredFields() throws URISyntaxException, FileNotFoundException {
        Definitions definitions = Definitions.createFromFile(getClass().getResource("definitions.json").toURI());

        Definitions definitions2 = Definitions.builder()
                .addType("t1", "t1val")
                .addType("t2", "t2val")
                .addTag("tag1", "tag1val")
                .addTag("tag2", "tag2val")
                .build();

        assertThat(definitions.getTags(), equalTo(definitions2.getTags()));
        assertThat(definitions.getTypes(), equalTo(definitions2.getTypes()));
    }

    @Test
    void assertBuildThrowsWithoutRequiredTypesField() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Definitions.builder()
                    .addTag("tag1", "tag1val")
                    .addTag("tag2", "tag2val")
                    .build();
        });
    }

    @Test
    void assertBuildThrowsWithoutRequiredTagsField() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Definitions.builder()
                    .addType("t1", "t1val")
                    .addType("t2", "t2val")
                    .build();
        });
    }

}
