/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedLengthTraitTest {
    @Test
    public void detectsMinRaised() {
        Shape s1 = StringShape.builder().id("foo.baz#Baz").addTrait(LengthTrait.builder().min(1L).build()).build();
        Shape s2 = StringShape.builder().id("foo.baz#Baz").addTrait(LengthTrait.builder().min(2L).build()).build();
        Model modelA = Model.assembler().addShapes(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedLengthTrait").size(), equalTo(1));
        assertThat(events.get(0).getMessage(), containsString("`min`"));
    }

    @Test
    public void detectsMaxLowered() {
        Shape s1 = StringShape.builder().id("foo.baz#Baz").addTrait(LengthTrait.builder().max(2L).build()).build();
        Shape s2 = StringShape.builder().id("foo.baz#Baz").addTrait(LengthTrait.builder().max(1L).build()).build();
        Model modelA = Model.assembler().addShapes(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedLengthTrait").size(), equalTo(1));
        assertThat(events.get(0).getMessage(), containsString("`max`"));
    }
}
