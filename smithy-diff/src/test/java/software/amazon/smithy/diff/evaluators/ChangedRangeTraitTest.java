/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedRangeTraitTest {
    @Test
    public void detectsMinRaised() {
        Shape i1 = IntegerShape.builder()
                .id("foo.baz#Baz")
                .addTrait(RangeTrait.builder().min(BigDecimal.valueOf(1)).build())
                .build();
        Shape i2 = IntegerShape.builder()
                .id("foo.baz#Baz")
                .addTrait(RangeTrait.builder().min(BigDecimal.valueOf(2)).build())
                .build();
        Model modelA = Model.assembler().addShapes(i1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(i2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedRangeTrait").size(), equalTo(1));
        assertThat(events.get(0).getMessage(), containsString("`min`"));
    }

    @Test
    public void detectsMaxLowered() {
        Shape i1 = IntegerShape.builder()
                .id("foo.baz#Baz")
                .addTrait(RangeTrait.builder().max(BigDecimal.valueOf(2)).build())
                .build();
        Shape i2 = IntegerShape.builder()
                .id("foo.baz#Baz")
                .addTrait(RangeTrait.builder().max(BigDecimal.valueOf(1)).build())
                .build();
        Model modelA = Model.assembler().addShapes(i1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(i2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedRangeTrait").size(), equalTo(1));
        assertThat(events.get(0).getMessage(), containsString("`max`"));
    }
}
