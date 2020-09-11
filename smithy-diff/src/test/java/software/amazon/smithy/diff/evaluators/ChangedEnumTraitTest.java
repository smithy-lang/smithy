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
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedEnumTraitTest {
    @Test
    public void detectsAddedEnums() {
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .addEnum(EnumDefinition.builder().value("baz").build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").get(0).getSeverity(), equalTo(Severity.NOTE));
    }

    @Test
    public void detectsRemovedEnums() {
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .addEnum(EnumDefinition.builder().value("baz").build())
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }

    @Test
    public void detectsRenamedEnums() {
        StringShape s1 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").name("OLD").build())
                        .build())
                .build();
        StringShape s2 = StringShape.builder()
                .id("foo.baz#Baz")
                .addTrait(EnumTrait.builder()
                        .addEnum(EnumDefinition.builder().value("foo").name("NEW").build())
                        .build())
                .build();
        Model modelA = Model.assembler().addShape(s1).assemble().unwrap();
        Model modelB = Model.assembler().addShape(s2).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedEnumTrait").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, Severity.ERROR).size(), equalTo(1));
    }
}
