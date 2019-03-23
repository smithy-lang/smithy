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
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class AddedRequiredTraitTest {
    @Test
    public void detectAdditionOfRequiredTrait() {
        MemberShape member1 = MemberShape.builder().id("foo.baz#Baz$bam").target("foo.baz#String").build();
        MemberShape member2 = member1.toBuilder().addTrait(new RequiredTrait()).build();
        StructureShape shapeA1 = StructureShape.builder().id("foo.baz#Baz").addMember(member1).build();
        StructureShape shapeA2 = StructureShape.builder().id("foo.baz#Baz").addMember(member2).build();
        StringShape target = StringShape.builder().id("foo.baz#String").build();
        Model modelA = Model.assembler().addShapes(shapeA1, member1, target).assemble().unwrap();
        Model modelB = Model.assembler().addShapes(shapeA2, member2, target).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ModifiedTrait").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, member1.getId()).size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }
}
