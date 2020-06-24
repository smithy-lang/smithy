package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;

class ShapeLinkTest {

    @Test
    void toJsonNode() {
        ShapeLink shapeLink = new ShapeLink();

        shapeLink.setType("type");
        shapeLink.setId("id");
        shapeLink.getTags().get().add("tag");
        shapeLink.setFile("file");
        shapeLink.setLine(1);
        shapeLink.setColumn(2);

        ObjectNode node = shapeLink.toJsonNode();

        assert node.getStringMember(shapeLink.typeText).get().getValue().equals("type");
        assert node.getNumberMember(shapeLink.lineText).get().getValue().equals(1);
        assert node.getArrayMember(shapeLink.tagsText)
                .get()
                .get(0)
                .get()
                .expectStringNode()
                .getValue()
                .equals("tag");
    }

    @Test
    void fromJsonNode() {
        ShapeLink shapeLink = new ShapeLink();

        shapeLink.setType("type");
        shapeLink.setId("id");
        shapeLink.getTags().get().add("tag");
        shapeLink.setFile("file");
        shapeLink.setLine(1);
        shapeLink.setColumn(2);

        ObjectNode node = shapeLink.toJsonNode();

        ShapeLink shapeLink2 = new ShapeLink();
        //testing fromJsonNode
        shapeLink2.fromJsonNode(node);

        assert shapeLink.getColumn().equals(shapeLink2.getColumn());
        assert shapeLink.getId().equals(shapeLink2.getId());
        assert shapeLink.getFile().equals(shapeLink2.getFile());
        assert shapeLink.getTags().equals(shapeLink2.getTags());
        assert shapeLink.getId().equals(shapeLink2.getId());
        assert shapeLink.getType().equals(shapeLink2.getType());
    }
}