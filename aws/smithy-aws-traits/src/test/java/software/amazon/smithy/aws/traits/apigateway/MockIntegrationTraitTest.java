package software.amazon.smithy.aws.traits.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class MockIntegrationTraitTest {
    @Test
    public void loadsValidTrait() {
        MockIntegrationTrait trait = MockIntegrationTrait.builder()
                .passThroughBehavior("when_no_templates")
                .putRequestParameter("x", "y")
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}
