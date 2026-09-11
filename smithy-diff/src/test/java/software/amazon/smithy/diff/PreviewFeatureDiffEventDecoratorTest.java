/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Verifies that {@link PreviewFeatureDiffEventDecorator} downgrades backward-incompatible diff events to
 * WARNING when they occur within a PREVIEW closure, and leaves the intentional exceptions alone.
 *
 * <p>Each scenario has its own {@code decorators/<smithy-diff event>[-<variant>]-a.smithy} /
 * {@code -b.smithy} pair, and each test is named after the event it exercises. The changes themselves are
 * known to be breaking (that is smithy-diff's own behavior, tested elsewhere); these tests only check how the
 * decorator adjusts their severity.
 */
public class PreviewFeatureDiffEventDecoratorTest {

    private static final String UNSTABLE_ADDED_EVENT_ID = "TraitBreakingChange.Add.smithy.api#unstable";

    @Test
    public void changedMemberTarget() {
        assertDowngraded(compare("changed-member-target"), "ChangedMemberTarget", "smithy.example#PreviewIn$data");
    }

    @Test
    public void modifiedTraitOnPreviewMemberIsDowngraded() {
        // A backward-incompatible trait change (ModifiedTrait, via the legacy diff.error.update tag) on a shape
        // inside a preview closure is downgraded like any other in-closure breaking change. Preview features are
        // expected to evolve, including the traits applied to their shapes.
        assertDowngraded(compare("modified-trait-preview"),
                "ModifiedTrait.Update.smithy.example#customConst",
                "smithy.example#PreviewIn$data");
    }

    @Test
    public void addedRequiredMember() {
        assertDowngraded(compare("added-required-member"), "AddedRequiredMember", "smithy.example#PreviewIn$extra");
    }

    @Test
    public void changedNullability() {
        assertDowngraded(compare("changed-nullability"), "ChangedNullability", "smithy.example#PreviewIn$data");
    }

    @Test
    public void removedShape() {
        assertDowngraded(compare("removed-shape"), "RemovedShape", "smithy.example#PreviewIn$gone");
    }

    @Test
    public void changedLengthTrait() {
        assertDowngraded(compare("changed-length-trait"), "ChangedLengthTrait", "smithy.example#LenStr");
    }

    @Test
    public void changedRangeTrait() {
        assertDowngraded(compare("changed-range-trait"), "ChangedRangeTrait", "smithy.example#RangeInt");
    }

    @Test
    public void changedEnumTrait() {
        assertDowngraded(compare("changed-enum-trait"), "ChangedEnumTrait", "smithy.example#En");
    }

    @Test
    public void changedShapeType() {
        assertDowngraded(compare("changed-shape-type"), "ChangedShapeType", "smithy.example#Thing");
    }

    @Test
    public void changedDefault() {
        assertDowngraded(compare("changed-default"), "ChangedDefault", "smithy.example#PreviewIn$data");
    }

    @Test
    public void changedMemberOrder() {
        assertDowngraded(compare("changed-member-order"), "ChangedMemberOrder", "smithy.example#PreviewIn");
    }

    @Test
    public void changedOperationInput() {
        assertDowngraded(compare("changed-operation-input"), "ChangedOperationInput", "smithy.example#OpInputOp");
    }

    @Test
    public void changedResourceIdentifiers() {
        assertDowngraded(compare("changed-resource-identifiers"),
                "ChangedResourceIdentifiers",
                "smithy.example#IdResource");
    }

    @Test
    public void changedMemberTargetInPreviewServiceClosure() {
        assertDowngraded(compare("changed-member-target-preview-service"),
                "ChangedMemberTarget",
                "smithy.example#SvcMemberIn$data");
    }

    @Test
    public void changedNullabilityOnPreviewMember() {
        assertNotDowngraded(compare("changed-nullability-preview-member"),
                "ChangedNullability",
                "smithy.example#GaIn$previewMember");
    }

    @Test
    public void addedRequiredMemberOnPreviewMember() {
        assertNotDowngraded(compare("added-required-member-preview-member"),
                "AddedRequiredMember",
                "smithy.example#GaIn$addedRequiredPreviewMember");
    }

    @Test
    public void changedMemberTargetOnPreviewMember() {
        assertDowngraded(compare("changed-member-target-preview-member"),
                "ChangedMemberTarget",
                "smithy.example#GaIn$targetChange");
    }

    @Test
    public void removedShapeOnPreviewMember() {
        assertDowngraded(compare("removed-shape-preview-member"), "RemovedShape", "smithy.example#GaIn$goingAway");
    }

    @Test
    public void changedLengthTraitOnSharedShape() {
        assertNotDowngraded(compare("changed-length-trait-shared"),
                "ChangedLengthTrait",
                "smithy.example#SharedStr");
    }

    @Test
    public void changedLengthTraitOnSharedShapeWhoseGaConsumerIsRemoved() {
        assertNotDowngraded(compare("changed-length-trait-shared-ga-removed"),
                "ChangedLengthTrait",
                "smithy.example#AsymStr");
    }

    @Test
    public void nullabilityChangesOnSharedInputStayBlocking() {
        List<ValidationEvent> events = compare("changed-nullability-shared");
        assertNotDowngraded(events, "ChangedNullability", "smithy.example#SharedIn$extra");
        assertNotDowngraded(events, "AddedRequiredMember", "smithy.example#SharedIn$data");
    }

    @Test
    public void changedMemberTargetUnderFeatureWithNoReason() {
        assertNotDowngraded(compare("changed-member-target-no-reason"),
                "ChangedMemberTarget",
                "smithy.example#NoReasonIn$data");
    }

    @Test
    public void changedMemberTargetWithNoPreviewInTheModel() {
        assertNotDowngraded(compare("changed-member-target-ga"), "ChangedMemberTarget", "smithy.example#GaIn$data");
    }

    @Test
    public void removedEntityBindingFromPreviewParent() {
        assertDowngraded(compare("removed-entity-binding-preview-parent"),
                "RemovedOperationBinding.FromService.SvcGoneOp",
                "smithy.example#PreviewSvc");
    }

    @Test
    public void removedEntityBindingFromGaParent() {
        List<ValidationEvent> events = compare("removed-entity-binding-ga-parent");

        assertNotDowngraded(events, "RemovedOperationBinding.FromService.PreviewGoneOp", "smithy.example#Example");
        assertNotDowngraded(events, "RemovedResourceBinding.FromService.PreviewGoneResource", "smithy.example#Example");
        assertNotDowngraded(events,
                "RemovedOperationBinding.FromResource.PreviewBoundOp",
                "smithy.example#GaHostResource");
        assertNotDowngraded(events, "RemovedOperationBinding.FromService.GaGoneOp", "smithy.example#Example");
    }

    @Test
    public void traitBreakingChangeForFeatureIdAddedToExistingShape() {
        List<ValidationEvent> events = compare("trait-breaking-change");

        // The trait-level guard keeps the featureId addition itself blocking.
        assertSeverity(events, UNSTABLE_ADDED_EVENT_ID, "smithy.example#GuardFeatureIdOp", Severity.ERROR);

        // The decorator only downgrades a shape that resolves as preview in BOTH models, so the breaking input
        // change made in the same revision is not downgraded either.
        assertNotDowngraded(events, "ChangedMemberTarget", "smithy.example#GuardIn$data");

        // Allowed: a bare @unstable added to an existing shape, and a brand-new shape with a featureId.
        assertNoEvent(events, UNSTABLE_ADDED_EVENT_ID, "smithy.example#GuardBareOp");
        assertNoEvent(events, UNSTABLE_ADDED_EVENT_ID, "smithy.example#GuardNewOp");
    }

    @Test
    public void addedEntityBindingIsLeftAlone() {
        // Adding a preview operation is not blocking to begin with, so its severity is untouched.
        assertSeverity(compare("trait-breaking-change"),
                "AddedOperationBinding.ToService.GuardNewOp",
                "smithy.example#Example",
                Severity.NOTE);
    }

    private List<ValidationEvent> compare(String name) {
        return ModelDiff.compare(load(name + "-a.smithy"), load(name + "-b.smithy"));
    }

    private Model load(String file) {
        return Model.assembler().addImport(getClass().getResource("decorators/" + file)).assemble().unwrap();
    }

    private static void assertDowngraded(List<ValidationEvent> events, String idPrefix, String shape) {
        List<ValidationEvent> matched = matching(events, idPrefix, shape);
        assertThat("expected a " + idPrefix + " event on preview shape " + shape,
                matched.isEmpty(),
                equalTo(false));
        for (ValidationEvent e : matched) {
            assertThat(idPrefix + " on preview shape " + shape + " should be downgraded to WARNING",
                    e.getSeverity(),
                    equalTo(Severity.WARNING));
        }
    }

    private static void assertNotDowngraded(List<ValidationEvent> events, String idPrefix, String shape) {
        List<ValidationEvent> matched = matching(events, idPrefix, shape);
        assertThat("expected a " + idPrefix + " event on " + shape,
                matched.isEmpty(),
                equalTo(false));
        for (ValidationEvent e : matched) {
            assertThat(idPrefix + " on " + shape + " must NOT be downgraded",
                    e.getSeverity(),
                    not(equalTo(Severity.WARNING)));
        }
    }

    private static void assertSeverity(
            List<ValidationEvent> events,
            String idPrefix,
            String shape,
            Severity severity
    ) {
        List<ValidationEvent> matched = matching(events, idPrefix, shape);
        assertThat("expected a " + idPrefix + " event on " + shape, matched.isEmpty(), equalTo(false));
        for (ValidationEvent e : matched) {
            assertThat(idPrefix + " on " + shape + " should keep its severity", e.getSeverity(), equalTo(severity));
        }
    }

    private static void assertNoEvent(List<ValidationEvent> events, String eventId, String shape) {
        assertThat("expected no " + eventId + " event on " + shape,
                matching(events, eventId, shape).isEmpty(),
                equalTo(true));
    }

    // Matches events whose id starts with idPrefix and whose shapeId equals shape.
    private static List<ValidationEvent> matching(List<ValidationEvent> events, String idPrefix, String shape) {
        ShapeId id = ShapeId.from(shape);
        return events.stream()
                .filter(e -> e.getId().startsWith(idPrefix))
                .filter(e -> e.getShapeId().filter(id::equals).isPresent())
                .collect(Collectors.toList());
    }
}
