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
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class RemovedShapeTest {
    @Test
    public void detectsShapeRemoval() {
        Shape shapeA1 = StringShape.builder().id("foo.baz#Baz").build();
        Shape shapeB1 = StringShape.builder().id("foo.baz#Bam").build();
        Model modelA = Model.assembler().addShapes(shapeA1, shapeB1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeB1).assemble().unwrap();
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
