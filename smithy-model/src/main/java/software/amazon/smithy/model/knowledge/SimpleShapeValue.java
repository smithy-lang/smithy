package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class SimpleShapeValue implements ShapeValue {

    private final ShapeId shapeId;
    private final Node value;

    public SimpleShapeValue(ShapeId shapeId, Node value) {
        this.shapeId = shapeId;
        this.value = value;
    }

    @Override
    public ShapeId toShapeId() {
        return shapeId;
    }

    @Override
    public Node toNode() {
        return value;
    }
}
