package software.amazon.smithy.aws.iam.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ConditionKeyValueTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("condition-key-value.smithy"))
                .assemble()
                .unwrap();

        Shape shape = result.expectShape(ShapeId.from("smithy.example#EchoInput$id1"));
        ConditionKeyValueTrait trait = shape.expectTrait(ConditionKeyValueTrait.class);
        assertThat(trait.getValue(), equalTo("smithy:ActionContextKey1"));
    }
}
