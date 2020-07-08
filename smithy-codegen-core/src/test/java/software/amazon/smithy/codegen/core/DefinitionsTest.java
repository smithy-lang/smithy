package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class DefinitionsTest {

    @Test
    void toNode() {
        Definitions definitions1 = new Definitions();

        definitions1.setTags(new HashMap<>());
        definitions1.setTypes(new HashMap<>());

        Map<String, String> typesMap = definitions1.getTypes();
        Map<String, String> tagsMap = definitions1.getTags();

        typesMap.put("t1", "t1val");
        typesMap.put("t2", "t2val");
        tagsMap.put("tag1", "tag1val");
        tagsMap.put("tag2", "tag2val");

        ObjectNode node = definitions1.toNode();

        assert node.containsMember(Definitions.TYPE_TEXT);
        assert node.containsMember(Definitions.TAGS_TEXT);
        assert node.getObjectMember(Definitions.TYPE_TEXT)
                .get()
                .getMember("t1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t1val");
        assert node.getObjectMember(Definitions.TYPE_TEXT)
                .get()
                .getMember("t2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t2val");
        assert node.getObjectMember(Definitions.TAGS_TEXT)
                .get()
                .getMember("tag1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag1val");
        assert node.getObjectMember(Definitions.TAGS_TEXT)
                .get()
                .getMember("tag2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag2val");
    }

    @Test
    void fromNode() {
        Definitions definitions1 = new Definitions();

        definitions1.setTags(new HashMap<>());
        definitions1.setTypes(new HashMap<>());

        Map<String, String> typesMap = definitions1.getTypes();
        Map<String, String> tagsMap = definitions1.getTags();

        typesMap.put("t1", "t1val");
        typesMap.put("t2", "t2val");
        tagsMap.put("tag1", "tag1val");
        tagsMap.put("tag2", "tag2val");

        ObjectNode node = definitions1.toNode();

        Definitions definitions2 = new Definitions();
        definitions2.fromNode(node);

        assertThat(definitions1.getTags(), equalTo(definitions2.getTags()));
        assertThat(definitions1.getTypes(), equalTo(definitions2.getTypes()));
    }

    @Test
    void fromDefinitionsFile() throws URISyntaxException, FileNotFoundException {
        Definitions definitions1 = new Definitions();
        ObjectNode node = definitions1.fromDefinitionsFile(getClass().getResource("definitions.txt").toURI());

        assert node.containsMember(Definitions.TYPE_TEXT);
        assert node.containsMember(Definitions.TAGS_TEXT);
        assert node.getObjectMember(Definitions.TYPE_TEXT)
                .get()
                .getMember("t1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t1val");
        assert node.getObjectMember(Definitions.TYPE_TEXT)
                .get()
                .getMember("t2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("t2val");
        assert node.getObjectMember(Definitions.TAGS_TEXT)
                .get()
                .getMember("tag1")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag1val");
        assert node.getObjectMember(Definitions.TAGS_TEXT)
                .get()
                .getMember("tag2")
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag2val");
    }
}