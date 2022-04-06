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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
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
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ModifiedTraitTest {
    private static final ShapeId ID = ShapeId.from("com.foo#baz");

    private static final class TestCaseData {
        private final Shape oldShape;
        private final Shape newShape;

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
        Shape definition = createDefinition("diff.error.const");
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
                {null, "hi", "diff.error.add", "Added"},
                {"hi", null, "diff.error.remove", "Removed"},
                {"foo", "baz", "diff.error.update", "Changed"},
        });
    }

    private static Shape createDefinition(String tag) {
        return StringShape.builder()
                .id(ID)
                .addTrait(TagsTrait.builder().addValue(tag).build())
                .addTrait(TraitDefinition.builder().build())
                .build();
    }

    @Test
    public void modifiedShapeNoTag() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-test-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-test-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(3));
        assertThat(events.stream().filter(e -> e.getSeverity() == Severity.WARNING).count(), equalTo(1L));
        assertThat(events.stream().filter(e -> e.getSeverity() == Severity.NOTE).count(), equalTo(2L));

        assertThat(messages, containsInAnyOrder(
                "Changed trait `smithy.example#b` from \"hello\" to \"hello!\"",
                "Removed trait `smithy.example#a`. Previous trait value: {}",
                "Added trait `smithy.example#c` with value \"foo\""
        ));
    }

    @Test
    public void findsDifferencesInTraitValues() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(2));
        assertThat(events.stream().filter(e -> e.getSeverity() == Severity.ERROR).count(), equalTo(1L));
        assertThat(events.stream().filter(e -> e.getSeverity() == Severity.WARNING).count(), equalTo(1L));

        assertThat(messages, containsInAnyOrder(
                "Added trait contents to `smithy.example#aTrait` at path `/bar` with value \"no\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/baz/foo` from \"bye\" to \"adios\""
        ));
    }

    @Test
    public void findsDifferencesInListTraitValues() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-list-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-list-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(4));
        assertThat(messages, containsInAnyOrder(
                "Changed trait contents of `smithy.example#aTrait` at path `/foo/1` from \"b\" to \"B\"",
                "Added trait contents to `smithy.example#aTrait` at path `/foo/3` with value \"4\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/foo/2`. Removed value: \"3\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/foo`. Removed value: "
                + "[\n    \"1\",\n    \"2\",\n    \"3\"\n]"
        ));
    }

    @Test
    public void findsDifferencesInSetTraitValues() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-set-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-set-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(4));
        assertThat(messages, containsInAnyOrder(
                "Changed trait contents of `smithy.example#aTrait` at path `/foo/1` from \"b\" to \"B\"",
                "Added trait contents to `smithy.example#aTrait` at path `/foo/3` with value \"4\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/foo/2`. Removed value: \"3\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/foo`. Removed value: [\n    \"1\",\n    \"2\",\n    \"3\"\n]"
        ));
    }

    @Test
    public void findsDifferencesInMapTraitValues() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-map-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-map-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(4));
        assertThat(messages, containsInAnyOrder(
                "Changed trait contents of `smithy.example#aTrait` at path `/foo/bam` from \"b\" to \"B\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/foo`. Removed value: {\n    \"baz\": \"1\",\n    \"bam\": \"2\",\n    \"boo\": \"3\"\n}",
                "Added trait contents to `smithy.example#aTrait` at path `/foo/qux` with value \"4\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/foo/boo`. Removed value: \"3\""
        ));
    }

    @Test
    public void findsDifferencesInUnionTraitValues() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-union-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-modified-contents-union-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(2));
        assertThat(messages, containsInAnyOrder(
                "Changed trait contents of `smithy.example#aTrait` at path `/baz/foo` from \"a\" to \"b\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/baz/baz` from \"a\" to \"b\""
        ));
    }

    @Test
    public void findsDifferencesInTraitValuesOfAllSeverities() {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-modified-all-severities-a.smithy"))
                .assemble()
                .unwrap();
        Model modelB = Model.assembler()
                .addImport(getClass().getResource("trait-modified-all-severities-b.smithy"))
                .assemble()
                .unwrap();
        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "ModifiedTrait");
        List<String> messages = events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());

        assertThat(events, hasSize(12));
        assertThat(messages, containsInAnyOrder(
                "Added trait contents to `smithy.example#aTrait` at path `/a` with value \"a\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/b`. Removed value: \"a\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/c` from \"a\" to \"c\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/d` from \"a\" to \"d\"",
                "Added trait contents to `smithy.example#aTrait` at path `/e` with value \"a\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/f`. Removed value: \"a\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/g` from \"a\" to \"h\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/h` from \"a\" to \"h\"",
                "Added trait contents to `smithy.example#aTrait` at path `/i` with value \"a\"",
                "Removed trait contents from `smithy.example#aTrait` at path `/j`. Removed value: \"a\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/k` from \"a\" to \"k\"",
                "Changed trait contents of `smithy.example#aTrait` at path `/l` from \"a\" to \"l\""
        ));
    }
}
