/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ShapeMatcher;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ServiceValidatorTest {
    @Test
    public void syntheticTraitsDoNotCauseConflicts() {
        // These two string conflict by name, but are functionally identical.
        // The fact that they have differing synthetic traits can be ignored.
        StringShape string1 = StringShape.builder()
                .id("smithy.example#String1")
                .addTrait(new OriginalShapeIdTrait(ShapeId.from("com.foo#Str")))
                .build();
        StringShape string2 = StringShape.builder()
                .id("smithy.other#String1")
                .addTrait(new BoxTrait()) // box is synthetic since IDL 2.0
                .build();

        StructureShape input = StructureShape.builder()
                .id("smithy.example#OperationInput")
                .addTrait(new InputTrait())
                .addMember("foo", string1.getId())
                .addMember("bar", string2.getId())
                .build();
        StructureShape output = StructureShape.builder()
                .id("smithy.example#OperationOutput")
                .addTrait(new OutputTrait())
                .build();
        OperationShape operation = OperationShape.builder()
                .id("smithy.example#Operation")
                .input(input)
                .output(output)
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("smithy.example#Service")
                .version("X")
                .addOperation(operation)
                .build();
        Model model = Model.builder()
                .addShapes(service, operation, input, output, string1, string2)
                .build();
        List<ValidationEvent> events = new ServiceValidator().validate(model);
        ValidatedResult<Model> result = new ValidatedResult<>(model, events);

        assertThat(string1.getId(), ShapeMatcher.hasEvent(result, "Service", Severity.NOTE, "conflicts"));
        assertThat(string2.getId(), ShapeMatcher.hasEvent(result, "Service", Severity.NOTE, "conflicts"));
    }

    @Test
    public void detectsTraitsThatDoNotMatch() {
        StringShape string1 = StringShape.builder().id("smithy.example#String1").build();
        StringShape string2 = StringShape.builder()
                .id("smithy.other#String1")
                .addTrait(new SensitiveTrait())
                .build();
        StructureShape input = StructureShape.builder()
                .id("smithy.example#OperationInput")
                .addTrait(new InputTrait())
                .addMember("foo", string1.getId())
                .addMember("bar", string2.getId())
                .build();
        StructureShape output = StructureShape.builder()
                .id("smithy.example#OperationOutput")
                .addTrait(new OutputTrait())
                .build();
        OperationShape operation = OperationShape.builder()
                .id("smithy.example#Operation")
                .input(input)
                .output(output)
                .build();
        ServiceShape service = ServiceShape.builder()
                .id("smithy.example#Service")
                .version("X")
                .addOperation(operation)
                .build();
        Model model = Model.builder()
                .addShapes(service, operation, input, output, string1, string2)
                .build();
        List<ValidationEvent> events = new ServiceValidator().validate(model);
        ValidatedResult<Model> result = new ValidatedResult<>(model, events);

        assertThat(string1.getId(), ShapeMatcher.hasEvent(result, "Service", Severity.ERROR, "conflicts"));
        assertThat(string2.getId(), ShapeMatcher.hasEvent(result, "Service", Severity.ERROR, "conflicts"));
    }
}
