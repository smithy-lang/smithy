/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.UnstableFeatureIndex;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnstableFeaturesTrait;
import software.amazon.smithy.model.traits.UnstableTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that every feature id used by an unstable trait resolves to an unstableFeatures entry on each
 * enclosing service, that defined features are referenced, and that preview features are not nested.
 */
public final class UnstableFeaturesValidator extends AbstractValidator {

    private static final String UNDEFINED_FEATURE = "UndefinedFeature";
    private static final String UNREFERENCED_FEATURE = "UnreferencedFeature";
    private static final String NESTED_FEATURE = "NestedFeature";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        UnstableFeatureIndex index = UnstableFeatureIndex.of(model);

        // The featureIds that shapes of each service reference, used to find definitions nothing uses.
        Map<ShapeId, Set<String>> referencedFeatures = new HashMap<>();

        for (Shape shape : model.getShapesWithTrait(UnstableTrait.class)) {
            UnstableTrait trait = shape.expectTrait(UnstableTrait.class);
            String featureId = trait.getFeatureId().orElse(null);
            if (featureId == null) {
                continue;
            }

            Set<ShapeId> services = index.getContainingServices(shape.getId());
            for (ShapeId serviceId : services) {
                referencedFeatures.computeIfAbsent(serviceId, id -> new HashSet<>()).add(featureId);
            }

            validateFeatureIsDefined(model, shape, trait, featureId, services, events);
            validateFeatureIsNotNested(index, shape, trait, featureId, events);
        }

        validateFeaturesAreReferenced(model, referencedFeatures, events);
        return events;
    }

    // Every service the shape is part of must define the featureId; a shape bound to no service can never
    // resolve one.
    private void validateFeatureIsDefined(
            Model model,
            Shape shape,
            UnstableTrait trait,
            String featureId,
            Set<ShapeId> services,
            List<ValidationEvent> events
    ) {
        if (services.isEmpty()) {
            events.add(error(shape,
                    trait,
                    String.format(
                            "The `@unstable` trait references featureId `%s`, but the shape is bound to no "
                                    + "service, so the featureId cannot be resolved against an "
                                    + "`@unstableFeatures` trait.",
                            featureId),
                    UNDEFINED_FEATURE));
            return;
        }

        for (ShapeId serviceId : services) {
            if (!definesFeature(model, serviceId, featureId)) {
                events.add(error(shape,
                        trait,
                        String.format(
                                "The `@unstable` trait references featureId `%s`, but that featureId is not defined "
                                        + "in the `@unstableFeatures` trait of its enclosing service `%s`.",
                                featureId,
                                serviceId),
                        UNDEFINED_FEATURE));
            }
        }
    }

    // A shape is nested when an owner other than itself also owns it.
    private void validateFeatureIsNotNested(
            UnstableFeatureIndex index,
            Shape shape,
            UnstableTrait trait,
            String featureId,
            List<ValidationEvent> events
    ) {
        Set<ShapeId> enclosingOwners = new TreeSet<>(index.getFeatureOwners(shape.getId()));
        enclosingOwners.remove(shape.getId());

        if (!enclosingOwners.isEmpty()) {
            events.add(error(shape,
                    trait,
                    String.format(
                            "The `@unstable` trait references featureId `%s`, but this shape is already within "
                                    + "the closure of the preview shape(s) %s. Nesting one preview feature inside "
                                    + "another is not supported; a shape covered by an enclosing preview shape "
                                    + "must not carry its own `@unstable` trait.",
                            featureId,
                            enclosingOwners),
                    NESTED_FEATURE));
        }
    }

    private void validateFeaturesAreReferenced(
            Model model,
            Map<ShapeId, Set<String>> referencedFeatures,
            List<ValidationEvent> events
    ) {
        for (Shape service : model.getServiceShapesWithTrait(UnstableFeaturesTrait.class)) {
            UnstableFeaturesTrait trait = service.expectTrait(UnstableFeaturesTrait.class);
            Set<String> referenced = referencedFeatures.getOrDefault(service.getId(), Collections.emptySet());
            for (String featureId : trait.getFeatures().keySet()) {
                if (!referenced.contains(featureId)) {
                    events.add(warning(service,
                            trait,
                            String.format(
                                    "The `@unstableFeatures` trait defines featureId `%s`, but no shape of this "
                                            + "service references it with the `@unstable` trait.",
                                    featureId),
                            UNREFERENCED_FEATURE));
                }
            }
        }
    }

    private static boolean definesFeature(Model model, ShapeId serviceId, String featureId) {
        return model.getShape(serviceId)
                .flatMap(service -> service.getTrait(UnstableFeaturesTrait.class))
                .map(features -> features.getFeatures().containsKey(featureId))
                .orElse(false);
    }
}
