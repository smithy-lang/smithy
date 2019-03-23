package software.amazon.smithy.aws.traits.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitFactory;

public class ApiKeySourceTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        var id = ShapeId.from("smithy.example#Foo");
        var trait = factory.createTrait(ApiKeySourceTrait.NAME, id, Node.from("HEADER")).get();

        assertThat(trait, instanceOf(ApiKeySourceTrait.class));
        assertThat(factory.createTrait(ApiKeySourceTrait.NAME, id, trait.toNode()).get(), equalTo(trait));
    }
}
