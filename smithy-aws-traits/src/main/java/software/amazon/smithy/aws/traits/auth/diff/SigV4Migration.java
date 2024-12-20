/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.auth.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.diff.evaluators.AbstractDiffEvaluator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Emit diff validation for SigV4 migration in the {@code @auth} trait.
 *
 * Specifically, SigV4 ({@code aws.auth#sigv4}) to SigV4A ({@code aws.auth#sigv4a}) due to a subset of credentials
 * usable with SigV4 that are not usable with SigV4A.
 *
 * @see <a href="https://smithy.io/2.0/aws/aws-auth.html">AWS Authentication Traits</a>
 */
@SmithyInternalApi
public final class SigV4Migration extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();

        Model oldModel = differences.getOldModel();
        ServiceIndex oldServiceIndex = ServiceIndex.of(oldModel);
        Model newModel = differences.getNewModel();
        ServiceIndex newServiceIndex = ServiceIndex.of(newModel);

        // Validate Service effective auth schemes
        List<ChangedShape<ServiceShape>> serviceChanges = differences
                .changedShapes(ServiceShape.class)
                .collect(Collectors.toList());
        for (ChangedShape<ServiceShape> change : serviceChanges) {
            ServiceShape oldServiceShape = change.getOldShape();
            List<ShapeId> oldAuthSchemes = oldServiceIndex
                    .getEffectiveAuthSchemes(oldServiceShape)
                    .keySet()
                    .stream()
                    .collect(Collectors.toList());
            ServiceShape newServiceShape = change.getNewShape();
            List<ShapeId> newAuthSchemes = newServiceIndex
                    .getEffectiveAuthSchemes(newServiceShape)
                    .keySet()
                    .stream()
                    .collect(Collectors.toList());
            validateMigration(
                    newServiceShape,
                    oldAuthSchemes,
                    newAuthSchemes,
                    events);
        }

        Map<OperationShape, Set<ServiceShape>> operationToContainedServiceBindings =
                computeOperationToContainedServiceBindings(newModel);
        List<ChangedShape<OperationShape>> operationChanges = differences
                .changedShapes(OperationShape.class)
                .collect(Collectors.toList());
        for (ChangedShape<OperationShape> change : operationChanges) {
            OperationShape newOperationShape = change.getNewShape();
            Set<ServiceShape> newOperationServiceBindings = operationToContainedServiceBindings.get(newOperationShape);
            if (newOperationServiceBindings == null) {
                continue;
            }
            // Validate Operation effective auth schemes
            for (ServiceShape newServiceShape : newOperationServiceBindings) {
                oldModel.getShape(newServiceShape.getId())
                        .filter(Shape::isServiceShape)
                        .map(s -> (ServiceShape) s)
                        .ifPresent(oldServiceShape -> {
                            OperationShape oldOperationShape = change.getOldShape();
                            List<ShapeId> oldAuthSchemes = oldServiceIndex
                                    .getEffectiveAuthSchemes(oldServiceShape, oldOperationShape)
                                    .keySet()
                                    .stream()
                                    .collect(Collectors.toList());
                            List<ShapeId> newAuthSchemes = newServiceIndex
                                    .getEffectiveAuthSchemes(newServiceShape, newOperationShape)
                                    .keySet()
                                    .stream()
                                    .collect(Collectors.toList());
                            validateMigration(
                                    newOperationShape,
                                    oldAuthSchemes,
                                    newAuthSchemes,
                                    events);
                        });
            }
        }

        return events;
    }

    private void validateMigration(
            Shape shape,
            List<ShapeId> oldAuthSchemes,
            List<ShapeId> newAuthSchemes,
            List<ValidationEvent> events
    ) {
        boolean isOldSigV4Present = oldAuthSchemes.contains(SigV4Trait.ID);
        boolean isOldSigV4APresent = oldAuthSchemes.contains(SigV4ATrait.ID);
        boolean isNewSigV4Present = newAuthSchemes.contains(SigV4Trait.ID);
        boolean isNewSigV4APresent = newAuthSchemes.contains(SigV4ATrait.ID);

        boolean isSigV4Replaced = isOldSigV4Present && !isNewSigV4Present && !isOldSigV4APresent && isNewSigV4APresent;
        boolean isSigV4AReplaced = !isOldSigV4Present && isNewSigV4Present && isOldSigV4APresent && !isNewSigV4APresent;
        boolean noSigV4XRemoved = isOldSigV4Present && isNewSigV4Present && isOldSigV4APresent && isNewSigV4APresent;
        boolean isSigV4Added = !isOldSigV4Present && isNewSigV4Present && isOldSigV4APresent && isNewSigV4APresent;
        boolean isSigV4AAdded = isOldSigV4Present && isNewSigV4Present && !isOldSigV4APresent && isNewSigV4APresent;
        if (isSigV4Replaced) {
            events.add(danger(
                    shape,
                    "The `aws.auth#sigv4` authentication scheme was replaced by the `aws.auth#sigv4a` authentication "
                            + "scheme in the effective auth schemes for `" + shape.getId() + "`. "
                            + "Replacing the `aws.auth#sigv4` authentication scheme with the `aws.auth#sigv4a` authentication "
                            + "scheme directly is not backward compatible since not all credentials usable by `aws.auth#sigv4` are "
                            + "compatible with `aws.auth#sigv4a`, and can break existing clients' authentication."));
        } else if (isSigV4AReplaced) {
            events.add(danger(
                    shape,
                    "The `aws.auth#sigv4a` authentication scheme was replaced by the `aws.auth#sigv4` authentication "
                            + "scheme in the effective auth schemes for `" + shape.getId() + "`. "
                            + "Replacing the `aws.auth#sigv4` authentication scheme with the `aws.auth#sigv4a` authentication "
                            + "scheme directly may not be backward compatible if the signing scope was narrowed (typically from "
                            + "`*`)."));
        } else if (noSigV4XRemoved) {
            int oldSigV4Index = oldAuthSchemes.indexOf(SigV4Trait.ID);
            int oldSigV4aIndex = oldAuthSchemes.indexOf(SigV4ATrait.ID);
            int sigV4Index = newAuthSchemes.indexOf(SigV4Trait.ID);
            int sigV4aIndex = newAuthSchemes.indexOf(SigV4ATrait.ID);
            boolean isOldSigV4BeforeSigV4A = oldSigV4Index < oldSigV4aIndex;
            boolean isSigV4BeforeSigV4A = sigV4Index < sigV4aIndex;
            if (isOldSigV4BeforeSigV4A && !isSigV4BeforeSigV4A) {
                events.add(danger(
                        shape,
                        "The `aws.auth#sigv4a` authentication scheme was moved before the `aws.auth#sigv4` authentication "
                                + "scheme in the effective auth schemes for `" + shape.getId() + "`. "
                                + "Moving the `aws.auth#sigv4a` authentication scheme before the `aws.auth#sigv4` authentication "
                                + "scheme is not backward compatible since not all credentials usable by `aws.auth#sigv4` are "
                                + "compatible with `aws.auth#sigv4a`, and can break existing clients' authentication."));
            }
            if (!isOldSigV4BeforeSigV4A && isSigV4BeforeSigV4A) {
                events.add(danger(
                        shape,
                        "The `aws.auth#sigv4` authentication scheme was moved before the `aws.auth#sigv4a` authentication "
                                + "scheme in the effective auth schemes for `" + shape.getId() + "`. "
                                + "Moving the `aws.auth#sigv4` authentication scheme before the `aws.auth#sigv4a` authentication "
                                + "scheme may not be backward compatible if the signing scope was narrowed (typically from `*`)."));
            }
        } else if (isSigV4Added) {
            int sigV4Index = newAuthSchemes.indexOf(SigV4Trait.ID);
            int sigV4aIndex = newAuthSchemes.indexOf(SigV4ATrait.ID);
            boolean isSigV4AddedBeforeSigV4A = sigV4Index < sigV4aIndex;
            if (isSigV4AddedBeforeSigV4A) {
                events.add(danger(
                        shape,
                        "The `aws.auth#sigv4` authentication scheme was added before the `aws.auth#sigv4a` authentication "
                                + "scheme in the effective auth schemes for `" + shape.getId() + "`. "
                                + "Adding the `aws.auth#sigv4` authentication scheme before an existing `aws.auth#sigv4a` "
                                + "authentication scheme may not be backward compatible if the signing scope was narrowed "
                                + "(typically from `*`)."));
            }
        } else if (isSigV4AAdded) {
            int sigV4Index = newAuthSchemes.indexOf(SigV4Trait.ID);
            int sigV4aIndex = newAuthSchemes.indexOf(SigV4ATrait.ID);
            boolean isSigV4AAddedBeforeSigV4 = sigV4aIndex < sigV4Index;
            if (isSigV4AAddedBeforeSigV4) {
                events.add(danger(
                        shape,
                        "The `aws.auth#sigv4a` authentication scheme was added before the `aws.auth#sigv4` authentication "
                                + "scheme in the effective auth schemes for `" + shape.getId() + "`. "
                                + "Adding the `aws.auth#sigv4a` authentication scheme before an existing `aws.auth#sigv4` "
                                + "authentication scheme is not backward compatible since not all credentials usable by "
                                + "`aws.auth#sigv4` are compatible with `aws.auth#sigv4a`, and can break existing clients' "
                                + "authentication."));
            }
        }
    }

    private static Map<OperationShape, Set<ServiceShape>> computeOperationToContainedServiceBindings(Model model) {
        Map<OperationShape, Set<ServiceShape>> operationToContainedServiceBindings = new HashMap<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        for (OperationShape operationShape : model.getOperationShapes()) {
            Set<ServiceShape> operationEntry = operationToContainedServiceBindings
                    .getOrDefault(operationShape, new HashSet());
            for (ServiceShape serviceShape : model.getServiceShapes()) {
                if (topDownIndex.getContainedOperations(serviceShape).contains(operationShape)) {
                    operationEntry.add(serviceShape);
                }
            }
            operationToContainedServiceBindings.put(operationShape, operationEntry);
        }
        return operationToContainedServiceBindings;
    }
}
