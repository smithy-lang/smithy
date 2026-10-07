/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Tests that a WARNING event is emitted for each suppression that matched no validation events,
 * while suppressions that do match events continue to silence exactly those events.
 */
public class UnusedSuppressionTest {

    @Test
    public void usedTraitSuppressionsAreNotWarned() {
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata validators = [",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"StringEvent\",",
                "        message: \"Bad string!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"string\" }",
                "    }",
                "]",
                "namespace smithy.example",
                "@suppress([\"StringEvent\"])",
                "string Used");

        // The event was silenced by the suppress trait.
        assertThat(result.getValidationEvents(Severity.SUPPRESSED), hasSize(1));
        assertThat(result.getValidationEvents(Severity.SUPPRESSED).get(0).getShapeId().get().getName(),
                equalTo("Used"));

        // The suppression matched an event, so no warning is emitted for it.
        assertThat(unusedSuppressionWarnings(result), empty());
    }

    @Test
    public void unusedTraitSuppressionEmitsWarning() {
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata validators = [",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"StringEvent\",",
                "        message: \"Bad string!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"string\" }",
                "    }",
                "]",
                "namespace smithy.example",
                "@suppress([\"NoSuchEvent\"])",
                "string Unused");

        List<ValidationEvent> warnings = unusedSuppressionWarnings(result);
        assertThat(warnings, hasSize(1));
        assertThat(warnings.get(0).getSeverity(), equalTo(Severity.WARNING));
        assertThat(warnings.get(0).getShapeId().isPresent(), is(true));
        assertThat(warnings.get(0).getShapeId().get().getName(), equalTo("Unused"));
        assertThat(warnings.get(0).getMessage(), containsString("`NoSuchEvent`"));
    }

    @Test
    public void usedMetadataSuppressionIsNotWarned() {
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata validators = [",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"StringEvent\",",
                "        message: \"Bad string!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"string\" }",
                "    }",
                "]",
                "metadata suppressions = [",
                "    { id: \"StringEvent\", namespace: \"*\" }",
                "]",
                "namespace smithy.example",
                "string Used");

        // The event was silenced by the metadata suppression.
        assertThat(result.getValidationEvents(Severity.SUPPRESSED), hasSize(1));
        assertThat(result.getValidationEvents(Severity.SUPPRESSED).get(0).getShapeId().get().getName(),
                equalTo("Used"));

        // The suppression matched an event, so no warning is emitted for it.
        assertThat(unusedSuppressionWarnings(result), empty());
    }

    @Test
    public void unusedMetadataSuppressionEmitsWarning() {
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata validators = [",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"StringEvent\",",
                "        message: \"Bad string!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"string\" }",
                "    }",
                "]",
                "metadata suppressions = [",
                "    { id: \"NoSuchEvent\", namespace: \"smithy.example\" }",
                "]",
                "namespace smithy.example",
                "string Used");

        List<ValidationEvent> warnings = unusedSuppressionWarnings(result);
        assertThat(warnings, hasSize(1));
        assertThat(warnings.get(0).getSeverity(), equalTo(Severity.WARNING));
        assertThat(warnings.get(0).getShapeId().isPresent(), is(false));
        assertThat(warnings.get(0).getMessage(), containsString("`NoSuchEvent`"));
        assertThat(warnings.get(0).getMessage(), containsString("`smithy.example`"));
    }

    @Test
    public void onlyNoOpSuppressionsAreWarned() {
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata validators = [",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"StringEvent\",",
                "        message: \"Bad string!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"string\" }",
                "    },",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"IntegerEvent\",",
                "        message: \"Bad integer!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"integer\" }",
                "    }",
                "]",
                "metadata suppressions = [",
                "    { id: \"IntegerEvent\", namespace: \"*\" },",
                "    { id: \"NoSuchEvent\", namespace: \"smithy.example\" }",
                "]",
                "namespace smithy.example",
                "@suppress([\"StringEvent\"])",
                "string UsedString",
                "@suppress([\"NoSuchEvent\"])",
                "string UnusedString",
                "integer MyInteger");

        // The used suppressions silenced exactly one event each.
        assertThat(result.getValidationEvents(Severity.SUPPRESSED), hasSize(2));
        assertThat(result.getValidationEvents(Severity.SUPPRESSED)
                .stream()
                .map(event -> event.getShapeId().get().getName())
                .collect(Collectors.toList()),
                equalTo(List.of("UsedString", "MyInteger")));

        // Exactly the no-op suppressions are warned about.
        List<ValidationEvent> warnings = unusedSuppressionWarnings(result);
        assertThat(warnings, hasSize(2));
        assertThat(warnings.stream()
                .filter(event -> event.getShapeId().isPresent())
                .map(ValidationEvent::getMessage)
                .collect(Collectors.toList()),
                equalTo(List.of("The `@suppress` trait value `NoSuchEvent` did not match any validation events.")));
        assertThat(warnings.stream()
                .filter(event -> !event.getShapeId().isPresent())
                .map(ValidationEvent::getMessage)
                .collect(Collectors.toList()),
                equalTo(List.of("The suppression with ID `NoSuchEvent` in namespace `smithy.example` did not match "
                        + "any validation events.")));
    }

    @Test
    public void suppressionsStillSilenceOnlyMatchingEvents() {
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata validators = [",
                "    {",
                "        name: \"EmitEachSelector\",",
                "        id: \"StringEvent\",",
                "        message: \"Bad string!\",",
                "        severity: \"WARNING\",",
                "        namespace: \"smithy.example\",",
                "        configuration: { selector: \"string\" }",
                "    }",
                "]",
                "namespace smithy.example",
                "@suppress([\"StringEvent\"])",
                "string A",
                "string B");

        // The event for shape A was silenced, but the event for shape B was not.
        assertThat(result.getValidationEvents(Severity.SUPPRESSED), hasSize(1));
        assertThat(result.getValidationEvents(Severity.SUPPRESSED).get(0).getShapeId().get().getName(),
                equalTo("A"));
        assertThat(result.getValidationEvents(Severity.WARNING)
                .stream()
                .filter(event -> event.containsId("StringEvent") && event.getShapeId().get().getName().equals("B"))
                .collect(Collectors.toList()),
                hasSize(1));
        assertThat(unusedSuppressionWarnings(result), empty());
    }

    @Test
    public void noOpSuppressionWarningsCannotBeSuppressed() {
        // This suppression would silence the no-op suppression warnings themselves if the warnings
        // were run through the suppression pipeline. Because the suppression matches no other event,
        // it is a no-op, and a warning is emitted for it too.
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata suppressions = [",
                "    { id: \"UnusedSuppression\", namespace: \"*\" }",
                "]",
                "namespace smithy.example",
                "@suppress([\"NoSuchEvent\"])",
                "string Unused");

        List<ValidationEvent> warnings = unusedSuppressionWarnings(result);
        assertThat(warnings, hasSize(2));

        // Neither warning was suppressed; one for the trait value, one for the self-referential
        // metadata suppression.
        assertThat(result.getValidationEvents(Severity.SUPPRESSED), empty());
        assertThat(warnings.stream()
                .map(ValidationEvent::getMessage)
                .collect(Collectors.toList()),
                equalTo(List.of(
                        "The `@suppress` trait value `NoSuchEvent` did not match any validation events.",
                        "The suppression with ID `UnusedSuppression` in namespace `*` did not match any validation "
                                + "events.")));
    }

    @Test
    public void doesNotWarnWhenValidationFailsEarly() {
        // The model contains an ERROR, so validation is cut short and the events that the
        // suppressions would have matched may never have been emitted. No warnings are emitted.
        ValidatedResult<Model> result = assemble(
                "$version: \"2.0\"",
                "metadata suppressions = [",
                "    { id: \"NoSuchEvent\", namespace: \"smithy.example\" }",
                "]",
                "namespace smithy.example",
                "@suppress([\"NoSuchEvent\"])",
                "string Unused",
                "structure S {",
                "    member: Missing",
                "}");

        assertThat(result.getValidationEvents(Severity.ERROR), not(empty()));
        assertThat(unusedSuppressionWarnings(result), empty());
    }

    private static ValidatedResult<Model> assemble(String... lines) {
        return Model.assembler()
                .addUnparsedModel("test.smithy", String.join("\n", lines))
                .assemble();
    }

    private static List<ValidationEvent> unusedSuppressionWarnings(ValidatedResult<Model> result) {
        return result.getValidationEvents()
                .stream()
                .filter(event -> event.getId().equals(ModelBasedEventDecorator.UNUSED_SUPPRESSION_EVENT_ID))
                .collect(Collectors.toList());
    }
}
