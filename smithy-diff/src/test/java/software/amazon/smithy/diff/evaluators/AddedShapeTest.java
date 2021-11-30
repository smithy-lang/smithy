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
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.validation.ValidationEvent;

public class AddedShapeTest {
    @Test
    public void detectsShapeAdded() {
        Shape shapeA = StringShape.builder().id("foo.baz#Baz").build();
        Shape shapeB = StringShape.builder().id("foo.baz#Bam").build();
        Model modelA = Model.assembler().addShapes(shapeA).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA, shapeB).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(1));
    }

    @Test
    public void doesNotEmitForMembersOfAddedContainerShapes() {
        Shape string = StringShape.builder().id("foo.baz#Baz").build();
        MemberShape member = MemberShape.builder().id("foo.baz#Bam$member").target(string).build();
        ListShape list = ListShape.builder().id("foo.baz#Bam").addMember(member).build();
        Model modelA = Model.assembler().addShapes(string).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(list, string).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedShape").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, list.getId()).size(), equalTo(1));
    }
}
