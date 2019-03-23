package software.amazon.smithy.aws.traits.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitFactory;

public class RequestValidatorTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        var id = ShapeId.from("smithy.example#Foo");
        var trait = factory.createTrait(RequestValidatorTrait.NAME, id, Node.from("full")).get();

        assertThat(trait, instanceOf(RequestValidatorTrait.class));
        assertThat(factory.createTrait(RequestValidatorTrait.NAME, id, trait.toNode()).get(), equalTo(trait));
    }
}
