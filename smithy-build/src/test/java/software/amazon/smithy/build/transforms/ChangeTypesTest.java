package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

public class ChangeTypesTest {
    @Test
    public void changesShapeTypes() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StructureShape b = StructureShape.builder().id("ns.foo#b").build();
        Model model = Model.assembler().addShapes(a, b).assemble().unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("shapeTypes", Node.objectNode()
                        .withMember("ns.foo#a", "boolean")
                        .withMember("ns.foo#b", "union")))
                .build();
        Model result = new ChangeTypes().transform(context);

        assertThat(result.expectShape(ShapeId.from("ns.foo#a")).getType(), is(ShapeType.BOOLEAN));
        assertThat(result.expectShape(ShapeId.from("ns.foo#b")).getType(), is(ShapeType.UNION));
    }
}
