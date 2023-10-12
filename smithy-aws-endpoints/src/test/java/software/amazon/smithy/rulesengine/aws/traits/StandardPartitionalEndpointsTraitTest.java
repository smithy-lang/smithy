package software.amazon.smithy.rulesengine.aws.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StandardPartitionalEndpointsTraitTest {
    @Test
    public void loadsFromModel() {
        final Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("standardPartitionalEndpoints.smithy"))
                .assemble()
                .unwrap();

        StandardPartitionalEndpointsTrait trait;

        trait = getTraitFromService(model, "ns.foo#Service1");

        assertEquals(trait.getEndpointPatternType(), EndpointPatternType.SERVICE_DNSSUFFIX);
        assertEquals(trait.getPartitionEndpointSpecialCases().size(), 0);

        trait = getTraitFromService(model, "ns.foo#Service2");

        assertEquals(trait.getEndpointPatternType(), EndpointPatternType.SERVICE_REGION_DNSSUFFI);
        assertEquals(trait.getPartitionEndpointSpecialCases().size(), 1);

        List<PartitionEndpointSpecialCase> cases = trait.getPartitionEndpointSpecialCases().get("aws-us-gov");

        PartitionEndpointSpecialCase case1 = cases.get(0);
        assertEquals(case1.getEndpoint(), "myservice.{region}.{dnsSuffix}");
        assertEquals(case1.getFips(), true);
        assertEquals(case1.getRegion(), "us-east-1");
        assertNull(case1.getDualStack());

        PartitionEndpointSpecialCase case2 = cases.get(1);
        assertEquals(case2.getEndpoint(), "myservice.global.amazonaws.com");
        assertEquals(case2.getDualStack(), true);
        assertEquals(case2.getRegion(), "us-west-2");
        assertNull(case2.getFips());
    }

    private StandardPartitionalEndpointsTrait getTraitFromService(Model model, String service) {
        return model
            .expectShape(ShapeId.from(service))
            .asServiceShape().get()
            .getTrait(StandardPartitionalEndpointsTrait.class).get();
    }
}
