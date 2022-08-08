package software.amazon.smithy.build.transforms;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

public class ExcludeShapesByTypeTest {
    @ParameterizedTest
    @MethodSource("shapeTypeValues")
    public void removesShapesByType(List<String> shapeTypeValues) {
        OperationShape operationA = OperationShape.builder()
                .id(ShapeId.fromParts("ns", "baz"))
                .build();
        ServiceShape serviceA = ServiceShape.builder()
                .id(ShapeId.fromParts("ns", "bar"))
                .version("1.0")
                .build();
        StringShape stringA = StringShape.builder()
                .id(ShapeId.fromParts("ns", "foo"))
                .build();
        Model model = Model.builder()
                .addShapes(operationA, serviceA, stringA)
                .build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("shapeTypes",
                                Node.fromStrings(shapeTypeValues)))
                .build();
        Model result = new ExcludeShapesByType().transform(context);

        // Aggregate shape removal also removes members.
        assertThat(result.getShapeIds(),
                not(hasItem(ShapeId.fromParts("ns", "baz"))));
        assertThat(result.getShapeIds(),
                not(hasItem(ShapeId.fromParts("ns", "bar"))));
        assertThat(result.getShapeIds(),
                not(hasItem(ShapeId.fromParts("ns", "bar", "world"))));

        // Doesn't remove unmatched shapes.
        assertThat(result.getShapeIds(),
                hasItem(ShapeId.fromParts("ns", "foo")));
    }

    public static List<List<String>> shapeTypeValues() {
        return Arrays.asList(Arrays.asList(
                // Relative IDs are assumed to be in "smithy.api".
                "operation",
                // Absolute IDs are used as-is.
                "service"));
    }
}
