package software.amazon.smithy.model.validation;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.validation.validators.ExamplesTraitValidator;
import software.amazon.smithy.utils.ListUtils;

public class ExamplesTraitValidatorTest {
    // There was an NPE previously due to the input/output values
    // in the builder for an ExamplesTrait.Example not being
    // initialized to an empty ObjectNode.
    @Test
    public void noNpeWhenInputOrOutputAreOmitted() {
        ExamplesTrait.Example example = ExamplesTrait.Example.builder()
                .title("Title")
                .documentation("Documentation")
                .build();
        ExamplesTrait trait = ExamplesTrait.builder().addExample(example).build();
        Shape shape = OperationShape.builder()
                .id("foo.bar#Baz")
                .addTrait(trait)
                .build();
        Model model = Model.builder().addShape(shape).build();

        ExamplesTraitValidator validator = new ExamplesTraitValidator();

        // Just make sure it doesn't throw.
        validator.validate(model);
    }
}
