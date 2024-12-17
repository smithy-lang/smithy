/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that references are correct.
 */
public final class ReferencesTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape shape : model.getShapesWithTrait(ReferencesTrait.class)) {
            events.addAll(validateShape(model, shape, shape.expectTrait(ReferencesTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateShape(Model model, Shape shape, ReferencesTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ReferencesTrait.Reference reference : trait.getReferences()) {
            if (shape.isStringShape() && !reference.getIds().isEmpty()) {
                events.add(error(shape,
                        trait,
                        "References applied to string shapes cannot contain 'ids': "
                                + reference));
            }

            ShapeId shapeId = reference.getResource();
            Optional<Shape> targetedShape = model.getShape(shapeId);
            if (targetedShape.isPresent()) {
                if (!targetedShape.get().isResourceShape()) {
                    events.add(error(shape,
                            trait,
                            format(
                                    "`references` trait reference targets a %s shape not a resource: %s",
                                    targetedShape.get().getType(),
                                    reference)));
                } else {
                    ResourceShape resource = targetedShape.get().asResourceShape().get();
                    events.addAll(validateSingleReference(model, reference, shape, trait, resource));
                }
            }
        }

        return events;
    }

    private List<ValidationEvent> validateSingleReference(
            Model model,
            ReferencesTrait.Reference reference,
            Shape shape,
            ReferencesTrait trait,
            ResourceShape target
    ) {
        if (shape.asStructureShape().isPresent()) {
            return validateStructureRef(model, reference, shape.asStructureShape().get(), trait, target);
        } else if (shape.asStringShape().isPresent()) {
            return validateStringShapeRef(reference, shape.asStringShape().get(), trait, target);
        } else {
            return Collections.emptyList();
        }
    }

    private List<ValidationEvent> validateStringShapeRef(
            ReferencesTrait.Reference reference,
            StringShape shape,
            ReferencesTrait trait,
            ResourceShape target
    ) {
        // You can only reference a resource with a single ID on a string shape.
        if (target.getIdentifiers().size() != 1) {
            return ListUtils.of(error(shape,
                    trait,
                    String.format(
                            "This string shape contains an invalid reference to %s: %s. References on a string shape can "
                                    + "only refer to resource shapes with exactly one entry in its identifiers property, but this "
                                    + "shape has the following identifiers: [%s]",
                            target.getId(),
                            reference,
                            ValidationUtils.tickedList(target.getIdentifiers().keySet()))));
        }

        return ListUtils.of();
    }

    private enum ErrorReason {
        BAD_TARGET, NOT_FOUND, NOT_REQUIRED
    }

    private List<ValidationEvent> validateStructureRef(
            Model model,
            ReferencesTrait.Reference reference,
            StructureShape shape,
            ReferencesTrait trait,
            ResourceShape target
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        Map<String, String> resolvedIds = resolveIds(reference, target);
        boolean implicit = !resolvedIds.equals(reference.getIds());

        // Only validate the "ids" keys against the "identifiers" names
        // if it's an explicit reference.
        if (!implicit) {
            validateExplicitIds(reference, shape, trait, target, resolvedIds, events);
        }

        Map<String, ErrorReason> errors = new HashMap<>();
        for (String memberName : resolvedIds.values()) {
            if (!shape.getMember(memberName).isPresent()) {
                errors.put(memberName, ErrorReason.NOT_FOUND);
            } else {
                MemberShape structMember = shape.getMember(memberName).get();
                if (!model.getShape(structMember.getTarget()).filter(Shape::isStringShape).isPresent()) {
                    errors.put(memberName, ErrorReason.BAD_TARGET);
                } else if (!structMember.isRequired()) {
                    errors.put(memberName, ErrorReason.NOT_REQUIRED);
                }
            }
        }

        if (!errors.isEmpty()) {
            validateErrors(shape, trait, reference, implicit, errors).ifPresent(events::add);
        }

        return events;
    }

    private Map<String, String> resolveIds(ReferencesTrait.Reference reference, ResourceShape target) {
        return !reference.getIds().isEmpty()
                ? reference.getIds()
                : target.getIdentifiers().keySet().stream().collect(toMap(identity(), identity()));
    }

    private void validateExplicitIds(
            ReferencesTrait.Reference reference,
            StructureShape shape,
            ReferencesTrait trait,
            ResourceShape target,
            Map<String, String> resolvedIds,
            List<ValidationEvent> events
    ) {
        // References require the exact number of entries as the identifiers of the resource.
        if (resolvedIds.size() != target.getIdentifiers().size()) {
            events.add(wrongNumberOfIdentifiers(shape, trait, reference, target.getIdentifiers().keySet()));
        }

        // Make sure the keys of the "ids" are actually part of the identifiers of the resource.
        Set<String> providedKeys = new HashSet<>(resolvedIds.keySet());
        providedKeys.removeAll(target.getIdentifiers().keySet());
        if (!providedKeys.isEmpty()) {
            events.add(extraneousIdentifiers(shape, trait, reference, target, providedKeys));
        }
    }

    private ValidationEvent wrongNumberOfIdentifiers(
            Shape shape,
            ReferencesTrait trait,
            ReferencesTrait.Reference reference,
            Collection<String> expectedNames
    ) {
        String prefix = expectedNames.size() < reference.getIds().size()
                ? "Too many identifiers"
                : "Not enough identifiers";
        return error(shape,
                trait,
                String.format(
                        "%s were provided in the `ids` properties of %s. Expected %d identifiers(s), but "
                                + "found %d. This resource requires bindings for the following identifiers: [%s].",
                        prefix,
                        reference,
                        expectedNames.size(),
                        reference.getIds().size(),
                        ValidationUtils.tickedList(expectedNames)));
    }

    private ValidationEvent extraneousIdentifiers(
            Shape shape,
            ReferencesTrait trait,
            ReferencesTrait.Reference reference,
            ResourceShape resource,
            Collection<String> extraneousKeys
    ) {
        return error(shape,
                trait,
                String.format(
                        "`references` trait %s contains extraneous resource identifier bindings, [%s], that are "
                                + "not actually identifier names of the resource, `%s`. This resource has the following identifier "
                                + "names: [%s]",
                        reference,
                        ValidationUtils.tickedList(extraneousKeys),
                        reference.getResource(),
                        ValidationUtils.tickedList(resource.getIdentifiers().keySet())));
    }

    private Optional<ValidationEvent> validateErrors(
            StructureShape shape,
            ReferencesTrait trait,
            ReferencesTrait.Reference reference,
            boolean implicit,
            Map<String, ErrorReason> errors
    ) {
        List<String> messages = new ArrayList<>();
        errors.forEach((name, reason) -> {
            switch (reason) {
                case NOT_FOUND:
                    // Must be found when it's explicit or not a collection rel.
                    if (implicit) {
                        messages.add(format("implicit binding of `%s` is not part of the structure "
                                + "(set \"rel\" to \"collection\" to create a collection binding)", name));
                    } else {
                        messages.add(format("`%s` refers to a member that is not part of the structure", name));
                    }
                    break;
                case BAD_TARGET:
                    messages.add(format("`%s` refers to a member that does not target a string shape", name));
                    break;
                case NOT_REQUIRED:
                default:
                    break;
            }
        });

        if (messages.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(shape,
                trait,
                format(
                        "`references` trait %s is invalid for the following reasons: %s",
                        reference,
                        messages.stream().sorted().collect(Collectors.joining("; ")))));
    }
}
