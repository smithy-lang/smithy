/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ChangedDefaultTest {
    @Test
    public void errorWhenDefaultIsRemovedFromShape() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "@default(0)\n"
                        + "integer Integer\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "integer Integer\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.ERROR));
    }

    @Test
    public void errorWhenDefaultIsAddedToRoot() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "integer Integer\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "@default(0)\n"
                        + "integer Integer\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.ERROR));
    }

    @Test
    public void errorWhenDefaultIsChangedOnRoot() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "@default(10)\n"
                        + "integer Integer\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "@default(20)\n"
                        + "integer Integer\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.ERROR));
    }

    @Test
    public void dangerWhenDefaultIsChangedOnMember() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 1\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 2\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.DANGER));
    }

    @Test
    public void errorWhenDefaultIsAddedToMemberWithNoAddedDefault() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 1\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.ERROR));
        assertThat(TestHelper.findEvents(events, "ChangedNullability").size(), equalTo(1));
    }

    @Test
    public void updateModelWithAddedDefault() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    @required\n"
                        + "    bar: Integer\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    @addedDefault\n"
                        + "    bar: Integer = 1\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault"), empty());
        assertThat(TestHelper.findEvents(events, "ChangedNullability"), empty());
    }

    @Test
    public void errorWhenDefaultChangesFromZeroToNonZeroValue() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 0\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 1\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.ERROR));
    }

    @Test
    public void errorWhenDefaultChangesFromNonZeroToZeroValue() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 1\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 0\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedDefault").get(0).getSeverity(), equalTo(Severity.ERROR));
    }

    @Test
    public void addingTheDefaultTraitToNullableMemberEmitsNoEvents() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = null\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(0));
        assertThat(TestHelper.findEvents(events, "ChangedNullability").size(), equalTo(0));
    }

    @Test
    public void changingFromNullDefaultToOneIsBreaking() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = null\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 1\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedNullability").size(), equalTo(1));
    }

    @Test
    public void changingFromNullDefaultToZeroIsBreaking() {
        String originalModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = null\n"
                        + "}\n";
        String updatedModel =
                "$version: \"2\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    bar: Integer = 0\n"
                        + "}\n";
        Model modelA = Model.assembler().addUnparsedModel("test.smithy", originalModel).assemble().unwrap();
        Model modelB = Model.assembler().addUnparsedModel("test.smithy", updatedModel).assemble().unwrap();
        List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);

        assertThat(TestHelper.findEvents(events, "ChangedDefault").size(), equalTo(1));
        assertThat(TestHelper.findEvents(events, "ChangedNullability").size(), equalTo(1));
    }
}
