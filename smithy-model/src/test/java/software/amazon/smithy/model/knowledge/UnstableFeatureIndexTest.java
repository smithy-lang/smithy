/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait.UnstableReason;
import software.amazon.smithy.utils.SetUtils;

public class UnstableFeatureIndexTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        // Not unwrapped: the fixture intentionally nests one owner inside another, which the index must
        // report and UnstableFeaturesValidator rejects with an ERROR.
        model = Model.assembler()
                .addImport(UnstableFeatureIndexTest.class.getResource("unstable-feature-index.smithy"))
                .assemble()
                .getResult()
                .get();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void operationOwnerCoversItsInputOutputClosure() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#PreviewOperation");

        assertThat(index.getFeatureOwners(operation), equalTo(SetUtils.of(operation)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOperationInput$previewOpInputMember")),
                equalTo(SetUtils.of(operation)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOperationOutput$previewOpOutputMember")),
                equalTo(SetUtils.of(operation)));
    }

    @Test
    public void memberOwnerCoversOnlyItself() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId member = ShapeId.from("smithy.example#GaOperationInput$previewMember");

        assertThat(index.getFeatureOwners(member), equalTo(SetUtils.of(member)));
        // The GA operation and its non-preview members belong to no feature.
        assertThat(index.getFeatureOwners(ShapeId.from("smithy.example#GaOperation")), equalTo(SetUtils.of()));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GaOperationInput$gaMember")),
                equalTo(SetUtils.of()));
    }

    @Test
    public void resourceOwnerCoversItsLifecycleClosure() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId resource = ShapeId.from("smithy.example#PreviewResource");

        assertThat(index.getFeatureOwners(resource), equalTo(SetUtils.of(resource)));
        // The resource's lifecycle operation and its I/O members belong to the resource's feature.
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GetPreviewResource")),
                equalTo(SetUtils.of(resource)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GetPreviewResourceInput$id")),
                equalTo(SetUtils.of(resource)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GetPreviewResourceOutput$detail")),
                equalTo(SetUtils.of(resource)));
    }

    @Test
    public void serviceOwnerCoversWholeClosure() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#PreviewService");

        assertThat(index.getFeatureOwners(service), equalTo(SetUtils.of(service)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewServiceOp")),
                equalTo(SetUtils.of(service)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewServiceNested$value")),
                equalTo(SetUtils.of(service)));
    }

    @Test
    public void shapesOutsideEveryFeatureHaveNoOwner() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);

        assertThat(index.getFeatureOwners(ShapeId.from("smithy.example#Service")), equalTo(SetUtils.of()));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GaOperationInput$gaMember")),
                equalTo(SetUtils.of()));
    }

    @Test
    public void shapesReachedOutsideEveryFeatureHaveNoOwner() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);

        // SharedStr is reachable from both the preview operation and the GA operation, so changing it would
        // affect the GA operation too.
        assertThat(index.getFeatureOwners(ShapeId.from("smithy.example#SharedStr")), equalTo(SetUtils.of()));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GaOperationInput$gaSharedMember")),
                equalTo(SetUtils.of()));
        // The preview operation's own member is not reached from outside, even though its target is.
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOperationInput$sharedMember")),
                equalTo(SetUtils.of(ShapeId.from("smithy.example#PreviewOperation"))));
    }

    @Test
    public void shapesReachableOnlyThroughOwnerAreOwned() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#PreviewOperation");

        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOnlyStruct")),
                equalTo(SetUtils.of(operation)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOnlyStruct$detail")),
                equalTo(SetUtils.of(operation)));
    }

    @Test
    public void shapesBoundToNoServiceAreNotConsumers() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);

        // UnboundStruct references PreviewOnlyStruct but is bound to no service, so it does not put
        // PreviewOnlyStruct outside the feature, and it is not part of the feature either.
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOnlyStruct")),
                equalTo(SetUtils.of(ShapeId.from("smithy.example#PreviewOperation"))));
        assertThat(index.getFeatureOwners(ShapeId.from("smithy.example#UnboundStruct")), equalTo(SetUtils.of()));
    }

    @Test
    public void resolvesAggregateShapes() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#PreviewOperation");

        // The list / map and their element shapes are reachable only through the preview operation, so
        // ownership must traverse the list-member and map-value edges.
        assertThat(index.getFeatureOwners(ShapeId.from("smithy.example#PreviewList")), equalTo(SetUtils.of(operation)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewListElement")),
                equalTo(SetUtils.of(operation)));
        assertThat(index.getFeatureOwners(ShapeId.from("smithy.example#PreviewMap")), equalTo(SetUtils.of(operation)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewMapValue")),
                equalTo(SetUtils.of(operation)));
    }

    @Test
    public void resolvesRecursiveShapes() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#PreviewOperation");

        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#RecursiveStruct")),
                equalTo(SetUtils.of(operation)));
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#RecursiveStruct$self")),
                equalTo(SetUtils.of(operation)));
    }

    @Test
    public void preludeShapesAreNeverOwned() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);

        assertThat(index.getFeatureOwners(ShapeId.from("smithy.api#String")), equalTo(SetUtils.of()));
    }

    @Test
    public void reportsEveryOwnerOfANestedOwner() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);

        // nestedOwnerMember declares its own @unstable while already inside PreviewOperation's closure, so it
        // reports both itself and PreviewOperation. This is what the validator uses to reject nesting.
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOperationInput$nestedOwnerMember")),
                equalTo(SetUtils.of(
                        ShapeId.from("smithy.example#PreviewOperationInput$nestedOwnerMember"),
                        ShapeId.from("smithy.example#PreviewOperation"))));

        // A top-level owner reports only itself.
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#PreviewOperation")),
                equalTo(SetUtils.of(ShapeId.from("smithy.example#PreviewOperation"))));
        // A member-level owner in a GA operation reports only itself.
        assertThat(
                index.getFeatureOwners(ShapeId.from("smithy.example#GaOperationInput$previewMember")),
                equalTo(SetUtils.of(ShapeId.from("smithy.example#GaOperationInput$previewMember"))));
    }

    @Test
    public void resolvesFeatureFromEnclosingService() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#PreviewOperation");

        assertThat(index.getFeature(operation).flatMap(f -> f.getMessage()), equalTo(Optional.of("Preview feature.")));
        assertThat(index.getFeature(operation).flatMap(f -> f.getReason()),
                equalTo(Optional.of(UnstableReason.PREVIEW)));
        assertThat(index.isInPreviewClosure(operation), equalTo(true));

        // Shapes in the owner's closure resolve to the same feature.
        assertThat(
                index.getFeature(ShapeId.from("smithy.example#PreviewOperationInput$previewOpInputMember")),
                equalTo(index.getFeature(operation)));
        assertThat(
                index.isInPreviewClosure(ShapeId.from("smithy.example#PreviewOperationInput$previewOpInputMember")),
                equalTo(true));

        // Shapes that belong to no feature have no metadata.
        assertThat(index.getFeature(ShapeId.from("smithy.example#GaOperation")), equalTo(Optional.empty()));
        assertThat(index.getFeature(ShapeId.from("smithy.example#SharedStr")), equalTo(Optional.empty()));
        assertThat(index.isInPreviewClosure(ShapeId.from("smithy.example#SharedStr")), equalTo(false));
    }

    @Test
    public void ignoresFeaturesDefinedByServicesThatDoNotEncloseTheOwner() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#MultiWrongOperation");

        // MultiWrongService and MultiRightService both define MULTI_PREVIEW, but MultiWrongOperation is only
        // enclosed by MultiWrongService, so it resolves to that entry. That entry has no reason, so the
        // feature is not a preview.
        assertThat(index.getFeature(operation).flatMap(f -> f.getMessage()), equalTo(Optional.of("Wrong service.")));
        assertThat(index.getFeature(operation).flatMap(f -> f.getReason()), equalTo(Optional.empty()));
        assertThat(index.isInPreviewClosure(operation), equalTo(false));
    }

    @Test
    public void doesNotResolveFeaturesTheEnclosingServicesDisagreeAbout() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId operation = ShapeId.from("smithy.example#MultiSharedOperation");

        // The operation is bound to both services, which define MULTI_PREVIEW differently. Rather than
        // letting one of them win, the feature does not resolve, so nothing about it is treated as preview.
        assertThat(
                index.getContainingServices(operation),
                equalTo(SetUtils.of(
                        ShapeId.from("smithy.example#MultiWrongService"),
                        ShapeId.from("smithy.example#MultiRightService"))));
        assertThat(index.getFeature(operation), equalTo(Optional.empty()));
        assertThat(index.isInPreviewClosure(operation), equalTo(false));
    }

    @Test
    public void resolvesServicesOwnersResideIn() {
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);
        ShapeId service = ShapeId.from("smithy.example#Service");

        assertThat(
                index.getContainingServices(ShapeId.from("smithy.example#PreviewOperation")),
                equalTo(SetUtils.of(service)));
        assertThat(
                index.getContainingServices(ShapeId.from("smithy.example#PreviewResource")),
                equalTo(SetUtils.of(service)));
        assertThat(
                index.getContainingServices(ShapeId.from("smithy.example#GaOperationInput$previewMember")),
                equalTo(SetUtils.of(service)));
        // An owner nested inside another feature still resides in the service, so its featureId can be
        // resolved against that service's unstableFeatures trait.
        assertThat(
                index.getContainingServices(ShapeId.from("smithy.example#PreviewOperationInput$nestedOwnerMember")),
                equalTo(SetUtils.of(service)));
        // An owner that is itself a service resides in itself.
        assertThat(
                index.getContainingServices(ShapeId.from("smithy.example#PreviewService")),
                equalTo(SetUtils.of(ShapeId.from("smithy.example#PreviewService"))));
        // Only populated for owners.
        assertThat(index.getContainingServices(ShapeId.from("smithy.example#GaOperation")), equalTo(SetUtils.of()));
        assertThat(index.getContainingServices(ShapeId.from("smithy.example#Missing")), equalTo(SetUtils.of()));
    }
}
