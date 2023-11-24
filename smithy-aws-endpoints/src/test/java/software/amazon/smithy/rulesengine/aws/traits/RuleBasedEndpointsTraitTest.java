package software.amazon.smithy.rulesengine.aws.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedEndpointsTraitTest {
    @Test
    public void loadsFromModel() {
        final Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("ruleBasedEndpoints.smithy"))
                .assemble()
                .unwrap();

        Optional<RuleBasedEndpointsTrait>  trait = model
                .expectShape(ShapeId.from("ns.foo#Service1"))
                .asServiceShape().get()
                .getTrait(RuleBasedEndpointsTrait.class);

        assertTrue(trait.isPresent());
    }
}
