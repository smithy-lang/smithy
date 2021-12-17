/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;

public class RemovedRequiredTraitTest {
    @Test
    public void replacingRequiredTraitWithDefaultIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new RequiredTrait()))
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b2 -> b2.addTrait(new DefaultTrait()))
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getId().equals("RemovedRequiredTrait"))
                           .count(), equalTo(0L));
    }

    @Test
    public void removingTheRequiredTraitOnInputStructureIsOk() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .addTrait(new InputTrait())
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new RequiredTrait()))
                .build();
        StructureShape b = StructureShape.builder()
                .addTrait(new InputTrait())
                .id("smithy.example#A")
                .addMember("foo", s.getId())
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getId().equals("RemovedRequiredTrait"))
                           .count(), equalTo(0L));
    }

    @Test
    public void detectsInvalidRemovalOfRequired() {
        StringShape s = StringShape.builder().id("smithy.example#Str").build();
        StructureShape a = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId(), b1 -> b1.addTrait(new RequiredTrait()))
                .build();
        StructureShape b = StructureShape.builder()
                .id("smithy.example#A")
                .addMember("foo", s.getId())
                .build();
        Model model1 = Model.builder().addShapes(s, a).build();
        Model model2 = Model.builder().addShapes(s, b).build();
        ModelDiff.Result result = ModelDiff.builder().oldModel(model1).newModel(model2).compare();

        assertThat(result.isDiffBreaking(), is(true));
        assertThat(result.getDiffEvents().stream()
                           .filter(event -> event.getSeverity() == Severity.ERROR)
                           .filter(event -> event.getId().equals("RemovedRequiredTrait"))
                           .filter(event -> event.getShapeId().get().equals(a.getAllMembers().get("foo").getId()))
                           .filter(event -> event.getMessage().contains("Removed the @required trait"))
                           .count(), equalTo(1L));
    }
}
