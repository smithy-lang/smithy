/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedShapeTest {
    @Test
    public void detectsShapeRemoval() {
        Shape shapeA1 = StructureShape.builder().id("foo.baz#Baz").build();
        Shape shapeB1 = StructureShape.builder().id("foo.baz#Bam").build();
        Model modelA = Model.assembler().addShapes(shapeA1, shapeB1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeB1).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedShape").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, shapeA1.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void emitsWarningsForScalarShapes() {
        Shape[] scalarShapes = new Shape[] {
                IntegerShape.builder().id("foo.baz#BazOne").build(),
                BigDecimalShape.builder().id("foo.baz#BazTwo").build(),
                BigIntegerShape.builder().id("foo.baz#BazThree").build(),
                BlobShape.builder().id("foo.baz#BazFour").build(),
                BooleanShape.builder().id("foo.baz#BazFive").build(),
                ByteShape.builder().id("foo.baz#BazSix").build(),
                DoubleShape.builder().id("foo.baz#BazSeven").build(),
                FloatShape.builder().id("foo.baz#BazEight").build(),
                ShortShape.builder().id("foo.baz#BazNine").build(),
                TimestampShape.builder().id("foo.baz#BazTen").build(),
                LongShape.builder().id("foo.baz#BazEleven").build(),
                StringShape.builder().id("foo.baz#BazTwelve").build()
        };
        Model modelA = Model.assembler().addShapes(scalarShapes).assemble().unwrap();
        Model modelB = Model.assembler().assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedShape.ScalarShape").size(), equalTo(12));
        assertThat("Scalar removals should be WARNING severity",
                events.stream().allMatch(event -> Severity.WARNING.equals(event.getSeverity())));
    }

    @Test
    public void emitsErrorForEnumString() {
        Shape shapeA1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder().addEnum(EnumDefinition.builder().value("val").build()).build())
                .build();
        Model modelA = Model.assembler().addShapes(shapeA1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes().assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedShape").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, shapeA1.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void emitsErrorForIntEnum() {
        Shape shapeA1 = IntEnumShape.builder()
                .id("foo.baz#Baz")
                .addMember("FOO", 1)
                .build();
        Model modelA = Model.assembler().addShapes(shapeA1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes().assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedShape").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, shapeA1.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void doesNotEmitForPrivateShapes() {
        Shape shape = StringShape.builder().id("foo.baz#Baz").addTrait(new PrivateTrait()).build();
        Model modelA = Model.assembler().addShapes(shape).assemble().unwrap();
        Model modelB = Model.assembler().assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedShape"), empty());
    }

    @Test
    public void doesNotEmitForMembersOfRemovedShapes() {
        Shape string = StringShape.builder().id("foo.baz#Baz").build();
        MemberShape member = MemberShape.builder().id("foo.baz#Bam$member").target(string).build();
        ListShape list = ListShape.builder().id("foo.baz#Bam").addMember(member).build();
        Model modelA = Model.assembler().addShapes(list, string).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(string).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "RemovedShape").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, list.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }
}
