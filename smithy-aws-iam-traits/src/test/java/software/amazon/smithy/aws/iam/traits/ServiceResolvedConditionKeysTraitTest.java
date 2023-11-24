package software.amazon.smithy.aws.iam.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ServiceResolvedConditionKeysTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("service-resolved-condition-keys.smithy"))
                .assemble()
                .unwrap();

        Shape shape = result.expectShape(ShapeId.from("smithy.example#MyService"));
        ServiceResolvedConditionKeysTrait trait = shape.expectTrait(ServiceResolvedConditionKeysTrait.class);
        assertThat(trait.getValues(), equalTo(Collections.singletonList("smithy:ServiceResolveContextKey")));
    }
}
