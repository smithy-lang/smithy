/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ShapeMatcher;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class MemberNameValidatorTest {

    @Test
    public void acceptsValidLowerCamelCaseNames() {
        StringShape stringShape = StringShape.builder().id("smithy.example#MyString").build();
        StructureShape structure = StructureShape.builder()
                .id("smithy.example#MyStructure")
                .addMember("validName", stringShape.getId())
                .addMember("xmlRequest", stringShape.getId())
                .addMember("fooId", stringShape.getId())
                .addMember("a", stringShape.getId())
                .addMember("camelCase123", stringShape.getId())
                .build();

        Model model = Model.builder()
                .addShapes(stringShape, structure)
                .build();

        List<ValidationEvent> events = new MemberNameValidator().validate(model);
        assertThat(events, hasSize(0));
    }

    @Test
    public void rejectsInvalidMemberNames() {
        StringShape stringShape = StringShape.builder().id("smithy.example#MyString").build();
        StructureShape structure = StructureShape.builder()
                .id("smithy.example#MyStructure")
                .addMember("InvalidName", stringShape.getId()) // Starts with uppercase
                .addMember("invalid_name", stringShape.getId()) // Contains underscore
                .build();

        Model model = Model.builder()
                .addShapes(stringShape, structure)
                .build();

        List<ValidationEvent> events = new MemberNameValidator().validate(model);
        ValidatedResult<Model> result = new ValidatedResult<>(model, events);

        assertThat(events, hasSize(2));
        assertThat(ShapeId.from("smithy.example#MyStructure$InvalidName"),
                ShapeMatcher.hasEvent(result, "MemberName", Severity.WARNING, "does not follow strict lowerCamelCase"));
        assertThat(ShapeId.from("smithy.example#MyStructure$invalid_name"),
                ShapeMatcher.hasEvent(result, "MemberName", Severity.WARNING, "does not follow strict lowerCamelCase"));
    }

    @Test
    public void validatesUnionMembers() {
        StringShape stringShape = StringShape.builder().id("smithy.example#MyString").build();
        StructureShape union = StructureShape.builder()
                .id("smithy.example#MyUnion")
                .addMember("validName", stringShape.getId())
                .addMember("Invalid_Name", stringShape.getId()) // Contains underscore
                .build();

        Model model = Model.builder()
                .addShapes(stringShape, union)
                .build();

        List<ValidationEvent> events = new MemberNameValidator().validate(model);
        ValidatedResult<Model> result = new ValidatedResult<>(model, events);

        assertThat(events, hasSize(1));
        assertThat(ShapeId.from("smithy.example#MyUnion$Invalid_Name"),
                ShapeMatcher.hasEvent(result, "MemberName", Severity.WARNING, "does not follow strict lowerCamelCase"));
    }

    @Test
    public void handlesSingleCharacterNames() {
        StringShape stringShape = StringShape.builder().id("smithy.example#MyString").build();
        StructureShape structure = StructureShape.builder()
                .id("smithy.example#MyStructure")
                .addMember("a", stringShape.getId()) // Valid single lowercase letter
                .addMember("A", stringShape.getId()) // Invalid single uppercase letter
                .build();

        Model model = Model.builder()
                .addShapes(stringShape, structure)
                .build();

        List<ValidationEvent> events = new MemberNameValidator().validate(model);
        ValidatedResult<Model> result = new ValidatedResult<>(model, events);

        assertThat(events, hasSize(1));
        assertThat(ShapeId.from("smithy.example#MyStructure$A"),
                ShapeMatcher.hasEvent(result, "MemberName", Severity.WARNING, "does not follow strict lowerCamelCase"));
    }
}
