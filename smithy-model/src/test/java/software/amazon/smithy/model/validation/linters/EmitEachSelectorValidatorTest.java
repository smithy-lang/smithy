/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.linters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EmitEachSelectorValidatorTest {

    @Test
    public void messageTemplateCanBeCastToString() {
        EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();

        assertThat(config.getMessageTemplate(), nullValue());

        config.setMessageTemplate("Hi");

        assertThat(config.getMessageTemplate(), equalTo("Hi"));
    }

    @Test
    public void expandsMessageTemplates() {
        EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
        Model model = Model.builder()
                .addShape(StringShape.builder()
                        .id(ShapeId.from("foo.bar#Baz"))
                        .addTrait(new DocumentationTrait("hello"))
                        .build())
                .build();
        config.setSelector(Selector.parse("$foo(*)"));
        config.setMessageTemplate("before `@{trait|documentation}` after. ID: @{id}. Var: @{var|foo|id}.");
        EmitEachSelectorValidator validator = new EmitEachSelectorValidator(config);
        List<ValidationEvent> events = validator.validate(model);

        assertThat(events.get(0).getMessage(),
                equalTo("before `\"hello\"` after. ID: foo.bar#Baz. Var: [foo.bar#Baz]."));
    }

    @Test
    public void onlyEmitsEventsWhenShapeHasBoundTraitAndNoTemplate() {
        EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
        Model model = Model.builder()
                .addShape(StringShape.builder()
                        .id(ShapeId.from("foo.bar#A"))
                        .addTrait(new DocumentationTrait("hello"))
                        .build())
                .addShape(StringShape.builder().id(ShapeId.from("foo.bar#B")).build())
                .build();
        config.setSelector(Selector.parse("*"));
        config.setBindToTrait(DocumentationTrait.ID);
        EmitEachSelectorValidator validator = new EmitEachSelectorValidator(config);
        List<ValidationEvent> events = validator.validate(model);

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getShapeId(), equalTo(Optional.of(ShapeId.from("foo.bar#A"))));
    }

    @Test
    public void onlyEmitsEventsWhenShapeHasBoundTraitAndHasTemplate() {
        EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
        Model model = Model.builder()
                .addShape(StringShape.builder()
                        .id(ShapeId.from("foo.bar#A"))
                        .addTrait(new DocumentationTrait("hello"))
                        .build())
                .addShape(StringShape.builder().id(ShapeId.from("foo.bar#B")).build())
                .build();
        config.setSelector(Selector.parse("*"));
        config.setMessageTemplate("This is only set to test the necessary code path of using templates...");
        config.setBindToTrait(DocumentationTrait.ID);
        EmitEachSelectorValidator validator = new EmitEachSelectorValidator(config);
        List<ValidationEvent> events = validator.validate(model);

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getShapeId(), equalTo(Optional.of(ShapeId.from("foo.bar#A"))));
    }

    @Test
    public void skipsUsingTheActualValidatorIfNoTraitsUseTheBoundTrait() {
        EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
        Model model = Model.builder()
                .addShape(StringShape.builder().id(ShapeId.from("foo.bar#A")).build())
                .build();
        config.setSelector(Selector.parse("*"));
        config.setBindToTrait(DocumentationTrait.ID);
        EmitEachSelectorValidator validator = new EmitEachSelectorValidator(config);

        assertThat(validator.validate(model), empty());
    }

    @Test
    public void handlesEscapesAtSymbols() {
        EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
        config.setSelector(Selector.parse("string"));
        config.setMessageTemplate("A@@@@B");
        EmitEachSelectorValidator validator = new EmitEachSelectorValidator(config);
        Model model = Model.builder()
                .addShape(StringShape.builder().id(ShapeId.from("foo.bar#Baz")).build())
                .build();
        List<ValidationEvent> events = validator.validate(model);

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getMessage(), equalTo("A@@B"));
    }

    @Test
    public void validatesMessageTemplateIsNotUnclosed() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
            config.setSelector(Selector.parse("string"));
            config.setMessageTemplate("...hello @{");
            new EmitEachSelectorValidator(config);
        });

        assertThat(e.getMessage(),
                containsString("Syntax error at line 1 column 12 of EmitEachSelector message template"));
    }

    @Test
    public void validatesMessageTemplateIsNotEmpty() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
            config.setSelector(Selector.parse("string"));
            config.setMessageTemplate("@{}");
            new EmitEachSelectorValidator(config);
        });

        assertThat(e.getMessage(),
                containsString("Syntax error at line 1 column 3 of EmitEachSelector message template"));
    }

    @Test
    public void validatesMessageTemplateWithTrailingPipe() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            EmitEachSelectorValidator.Config config = new EmitEachSelectorValidator.Config();
            config.setSelector(Selector.parse("string"));
            config.setMessageTemplate("@{var|}");
            new EmitEachSelectorValidator(config);
        });

        assertThat(e.getMessage(),
                containsString("Syntax error at line 1 column 7 of EmitEachSelector message template"));
    }
}
