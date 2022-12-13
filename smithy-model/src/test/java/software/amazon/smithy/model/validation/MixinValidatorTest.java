package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.validation.validators.MixinValidator;

public class MixinValidatorTest {
    @Test
    public void ensuresManuallyBuiltModelsAreValid() {
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#mixin2")
                .addTrait(MixinTrait.builder().build())
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        StructureShape invalid = StructureShape.builder()
                .id("smithy.example#invalid")
                .addMixin(mixin1)
                .addMixin(mixin2)
                .build();
        Model model = Model.builder().addShapes(mixin1, mixin2, invalid).build();

        MixinValidator validator = new MixinValidator();
        List<ValidationEvent> events = validator.validate(model);

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getMessage(),
                   equalTo("Member `foo` conflicts with members defined in the following mixins: [smithy.example#mixin1, smithy.example#mixin2]"));
    }
}
