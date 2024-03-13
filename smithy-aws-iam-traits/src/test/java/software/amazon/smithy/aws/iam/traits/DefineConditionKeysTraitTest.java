package software.amazon.smithy.aws.iam.traits;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


public class DefineConditionKeysTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("define-condition-keys.smithy"))
                .assemble()
                .unwrap();

        Shape shape = result.expectShape(ShapeId.from("smithy.example#MyService"));
        DefineConditionKeysTrait trait = shape.expectTrait(DefineConditionKeysTrait.class);
        assertEquals(3,trait.getConditionKeys().size());
        assertFalse(trait.getConditionKey("myservice:Bar").get().getRequired().get());
        assertFalse(trait.getConditionKey("myservice:Foo").get().getRequired().get());
        assertTrue(trait.getConditionKey("myservice:Baz").get().getRequired().get());
    }
}
