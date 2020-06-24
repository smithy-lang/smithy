package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class DefinitionsTest {

    @Test
    void toJsonNode() {
        Definitions definitions1 = new Definitions();

        Map<String, String> typesMap = definitions1.getTypes();
        Map<String, String> tagsMap = definitions1.getTags();

        typesMap.put("t1", "t1val");
        typesMap.put("t2", "t2val");
        tagsMap.put("tag1", "tag1val");
        tagsMap.put("tag2", "tag2val");

        ObjectNode node = definitions1.toJsonNode();

        assert node.containsMember(definitions1.typeText);
        assert node.containsMember(definitions1.tagsText);
        assert node.getObjectMember(definitions1.typeText)
                .get()
                .getMember("t1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t1val");
        assert node.getObjectMember(definitions1.typeText)
                .get()
                .getMember("t2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t2val");
        assert node.getObjectMember(definitions1.tagsText)
                .get()
                .getMember("tag1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag1val");
        assert node.getObjectMember(definitions1.tagsText)
                .get()
                .getMember("tag2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag2val");
    }

    @Test
    void fromJsonNode() {
        Definitions definitions1 = new Definitions();

        Map<String, String> typesMap = definitions1.getTypes();
        Map<String, String> tagsMap = definitions1.getTags();

        typesMap.put("t1", "t1val");
        typesMap.put("t2", "t2val");
        tagsMap.put("tag1", "tag1val");
        tagsMap.put("tag2", "tag2val");

        ObjectNode node = definitions1.toJsonNode();

        Definitions definitions2 = new Definitions();
        definitions2.fromJsonNode(node);

        assertThat(definitions1, equalTo(definitions2));
    }

    @Test
    void fromDefinitionsFile() throws URISyntaxException, FileNotFoundException {
        Definitions definitions1 = new Definitions();
        ObjectNode node = definitions1.fromDefinitionsFile(getClass().getResource("definitions.txt").toURI());

        assert node.containsMember(definitions1.typeText);
        assert node.containsMember(definitions1.tagsText);
        assert node.getObjectMember(definitions1.typeText)
                .get()
                .getMember("t1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t1val");
        assert node.getObjectMember(definitions1.typeText)
                .get()
                .getMember("t2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t2val");
        assert node.getObjectMember(definitions1.tagsText)
                .get()
                .getMember("tag1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag1val");
        assert node.getObjectMember(definitions1.tagsText)
                .get()
                .getMember("tag2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag2val");
    }
}