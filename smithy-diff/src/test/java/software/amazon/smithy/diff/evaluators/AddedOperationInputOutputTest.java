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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class AddedOperationInputOutputTest {
    @Test
    public void detectsCompatibleInput() {
        StructureShape a = StructureShape.builder().id("foo.baz#A").build();
        OperationShape o1 = OperationShape.builder().id("foo.baz#Bar").build();
        OperationShape o2 = OperationShape.builder().id("foo.baz#Bar").input(a).build();
        Model modelA = Model.assembler().addShapes(o1, a).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(o2, a).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationInput").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.NOTE).size(), equalTo(1));
    }

    @Test
    public void detectsCompatibleOutput() {
        StructureShape a = StructureShape.builder().id("foo.baz#A").build();
        OperationShape o1 = OperationShape.builder().id("foo.baz#Bar").build();
        OperationShape o2 = OperationShape.builder().id("foo.baz#Bar").output(a).build();
        Model modelA = Model.assembler().addShapes(o1, a).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(o2, a).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationOutput").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.NOTE).size(), equalTo(1));
    }

    @Test
    public void detectsIncompatibleInput() {
        StringShape string = StringShape.builder().id("foo.baz#String").build();
        MemberShape member = MemberShape.builder()
                .id("foo.baz#A$m")
                .target(string)
                .addTrait(new RequiredTrait())
                .build();
        StructureShape struct = StructureShape.builder().id("foo.baz#A").addMember(member).build();
        OperationShape o1 = OperationShape.builder().id("foo.baz#Bar").build();
        OperationShape o2 = OperationShape.builder().id("foo.baz#Bar").input(struct).build();
        Model modelA = Model.assembler().addShapes(o1, struct, member, string).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(o2, struct, member, string).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationInput").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void detectsIncompatibleOutput() {
        StringShape string = StringShape.builder().id("foo.baz#String").build();
        MemberShape member = MemberShape.builder()
                .id("foo.baz#A$m")
                .target(string)
                .addTrait(new RequiredTrait())
                .build();
        StructureShape struct = StructureShape.builder().id("foo.baz#A").addMember(member).build();
        OperationShape o1 = OperationShape.builder().id("foo.baz#Bar").build();
        OperationShape o2 = OperationShape.builder().id("foo.baz#Bar").output(struct).build();
        Model modelA = Model.assembler().addShapes(o1, struct, member, string).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(o2, struct, member, string).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "AddedOperationOutput").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }
}
