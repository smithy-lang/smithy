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
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedMemberTargetTest {
    @Test
    public void detectsIncompatibleTypeChanges() {
        Shape shape1 = StringShape.builder().id("foo.baz#String").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();
        Shape shape2 = TimestampShape.builder().id("foo.baz#Timestamp").build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();
        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void detectsCompatibleTypeChanges() {
        StringShape shape1 = StringShape.builder().id("foo.baz#String1").build();
        MemberShape member1 = MemberShape.builder().id("foo.baz#List$member").target(shape1.getId()).build();
        ListShape list1 = ListShape.builder().id("foo.baz#List").member(member1).build();
        StringShape shape2 = StringShape.builder().id("foo.baz#String2").build();
        MemberShape member2 = MemberShape.builder().id("foo.baz#List$member").target(shape2.getId()).build();
        ListShape list2 = ListShape.builder().id("foo.baz#List").member(member2).build();
        Model modelA = Model.assembler().addShapes(shape1, shape2, member1, list1).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shape1, shape2, member2, list2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedMemberTarget").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member2.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.WARNING).size(), equalTo(1));
    }
}
