package software.amazon.smithy.aws.traits.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class IntegrationTraitTest {
    @Test
    public void loadsValidTrait() {
        IntegrationTrait trait = IntegrationTrait.builder()
                .uri("foo")
                .httpMethod("POST")
                .addCacheKeyParameter("foo")
                .cacheNamespace("baz")
                .connectionId("id")
                .connectionType("xyz")
                .contentHandling("CONVERT_TO_TEXT")
                .credentials("abc")
                .passThroughBehavior("when_no_templates")
                .putRequestParameter("x", "y")
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void loadsTraitFromModel() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("errorfiles/valid-integration.json"))
                .assemble()
                .unwrap();

        var trait = model.getShapeIndex().getShape(ShapeId.from("ns.foo#Operation"))
                .get()
                .getTrait(MockIntegrationTrait.class)
                .get();

        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}
