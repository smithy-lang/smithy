package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;

import java.util.ArrayList;

class ShapeLinkTest {

    @Test
    void toNode() {
        ShapeLink shapeLink = new ShapeLink();
        shapeLink.setTags(new ArrayList<>());

        shapeLink.setType("type");
        shapeLink.setId("id");
        shapeLink.getTags().get().add("tag");
        shapeLink.setFile("file");
        shapeLink.setLine(1);
        shapeLink.setColumn(2);

        ObjectNode node = shapeLink.toNode();

        assert node.getStringMember(ShapeLink.TYPE_TEXT).get().getValue().equals("type");
        assert node.getNumberMember(ShapeLink.LINE_TEXT).get().getValue().equals(1);
        assert node.getArrayMember(ShapeLink.TAGS_TEXT)
                .get()
                .get(0)
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag");
    }

    @Test
    void fromNode() {
        ShapeLink shapeLink = new ShapeLink();
        shapeLink.setTags(new ArrayList<>());

        shapeLink.setType("type");
        shapeLink.setId("id");
        shapeLink.getTags().get().add("tag");
        shapeLink.setFile("file");
        shapeLink.setLine(1);
        shapeLink.setColumn(2);

        ObjectNode node = shapeLink.toNode();

        ShapeLink shapeLink2 = new ShapeLink();
        //testing fromNode
        shapeLink2.fromNode(node);

        assert shapeLink.getColumn().equals(shapeLink2.getColumn());
        assert shapeLink.getId().equals(shapeLink2.getId());
        assert shapeLink.getFile().equals(shapeLink2.getFile());
        assert shapeLink.getTags().equals(shapeLink2.getTags());
        assert shapeLink.getId().equals(shapeLink2.getId());
        assert shapeLink.getType().equals(shapeLink2.getType());
    }
}