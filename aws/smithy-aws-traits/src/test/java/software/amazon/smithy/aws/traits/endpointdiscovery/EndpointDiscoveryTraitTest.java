package software.amazon.smithy.aws.traits.endpointdiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class EndpointDiscoveryTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        ServiceShape service = result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#FooService")).get()
                .asServiceShape().get();
        EndpointDiscoveryTrait trait = service.getTrait(EndpointDiscoveryTrait.class).get();

        assertEquals(trait.getOperation(), ShapeId.from("ns.foo#DescribeEndpoints"));
        assertEquals(trait.getError(), ShapeId.from("ns.foo#InvalidEndpointError"));
    }
}
