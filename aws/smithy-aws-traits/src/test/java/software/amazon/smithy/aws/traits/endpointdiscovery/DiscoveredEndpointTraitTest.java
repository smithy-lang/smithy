package software.amazon.smithy.aws.traits.endpointdiscovery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class DiscoveredEndpointTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        OperationShape operation = result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#GetObject")).get()
                .asOperationShape().get();
        DiscoveredEndpointTrait trait = operation.getTrait(DiscoveredEndpointTrait.class).get();

        assertTrue(trait.isRequired());
    }
}
