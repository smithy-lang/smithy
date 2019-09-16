package software.amazon.smithy.aws.traits.endpointdiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

public class CleanDiscoveryTraitTransformerTest {

    @Test
    public void removesTraitWhenOperationRemoved() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();

        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#DescribeEndpoints");
        });

        ServiceShape service = result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#FooService"))
                .flatMap(Shape::asServiceShape)
                .get();

        assertFalse(service.hasTrait(EndpointDiscoveryTrait.class));
    }

    @Test
    public void removesTraitWhenErrorRemoved() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();

        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#InvalidEndpointError");
        });

        ServiceShape service = result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#FooService"))
                .flatMap(Shape::asServiceShape)
                .get();

        assertFalse(service.hasTrait(EndpointDiscoveryTrait.class));
    }
}
