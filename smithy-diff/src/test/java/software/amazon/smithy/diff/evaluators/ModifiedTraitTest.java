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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ModifiedTraitTest {
    private static final ShapeId ID = ShapeId.from("com.foo#baz");

    private static final class TestCaseData {
        private Shape oldShape;
        private Shape newShape;

        public TestCaseData(String oldValue, String newValue) {
            StringShape.Builder builder1 = StringShape.builder().id("com.foo#String");
            if (oldValue != null) {
                builder1.addTrait(new DynamicTrait(ID, Node.from(oldValue)));
            }
            oldShape = builder1.build();

            StringShape.Builder builder2 = StringShape.builder().id("com.foo#String");
            if (newValue != null) {
                builder2.addTrait(new DynamicTrait(ID, Node.from(newValue)));
            }
            newShape = builder2.build();
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testConst(String oldValue, String newValue, String tag, String searchString) {
        TestCaseData data = new TestCaseData(oldValue, newValue);
        Shape definition = createDefinition(ModifiedTrait.DIFF_ERROR_CONST);
        Model modelA = Model.assembler().addShape(definition).addShape(data.oldShape).assemble().unwrap();
        Model modelB = Model.assembler().addShape(definition).addShape(data.newShape).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ModifiedTrait").size(), equalTo(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testWithTag(String oldValue, String newValue, String tag, String searchString) {
        TestCaseData data = new TestCaseData(oldValue, newValue);

        Shape definition = createDefinition(tag);
        Model modelA = Model.assembler().addShape(definition).addShape(data.oldShape).assemble().unwrap();
        Model modelB = Model.assembler().addShape(definition).addShape(data.newShape).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ModifiedTrait").size(), equalTo(1));
        assertThat(events.get(0).getMessage(), containsString(searchString));
    }

    public static Collection<String[]> data() {
        return Arrays.asList(new String[][] {
                {null, "hi", ModifiedTrait.DIFF_ERROR_ADD, "to add"},
                {"hi", null, ModifiedTrait.DIFF_ERROR_REMOVE, "to remove"},
                {"foo", "baz", ModifiedTrait.DIFF_ERROR_UPDATE, "to change"},
        });
    }

    private static Shape createDefinition(String tag) {
        return StringShape.builder()
                .id(ID)
                .addTrait(TagsTrait.builder().addValue(tag).build())
                .addTrait(TraitDefinition.builder().build())
                .build();
    }
}
