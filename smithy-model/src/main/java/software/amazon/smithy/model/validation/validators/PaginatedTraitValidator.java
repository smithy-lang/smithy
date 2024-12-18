/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates paginated traits.
 *
 * <ul>
 *     <li>The inputToken and outputToken properties must be set when an
 *      operation's properties are optionally merged with a service's.</li>
 *     <li>The items property, if set, must reference a list or map
 *      output member.</li>
 *     <li>The pageSize property, if set, should reference an optional integer
 *      input member. It may, but should not reference an optional byte, short,
 *      or long.</li>
 *     <li>The inputToken property should reference an optional string input
 *      member. It may, but should not reference an optional map.</li>
 *     <li>The outputToken property should reference an optional string output
 *      member. It may, but should not reference an optional map.</li>
 * </ul>
 */
public final class PaginatedTraitValidator extends AbstractValidator {
    private static final Set<ShapeType> ITEM_SHAPES = SetUtils.of(ShapeType.LIST, ShapeType.MAP);
    private static final Set<ShapeType> PAGE_SHAPES = SetUtils.of(ShapeType.BYTE,
            ShapeType.INTEGER,
            ShapeType.LONG,
            ShapeType.SHORT);
    private static final Set<ShapeType> DANGER_PAGE_SHAPES = SetUtils.of(ShapeType.BYTE,
            ShapeType.LONG,
            ShapeType.SHORT);
    private static final Set<ShapeType> TOKEN_SHAPES = SetUtils.of(ShapeType.STRING, ShapeType.MAP);
    private static final Set<ShapeType> DANGER_TOKEN_SHAPES = SetUtils.of(ShapeType.MAP);
    private static final Pattern PATH_PATTERN = Pattern.compile("\\.");
    private static final String DEEPLY_NESTED = "DeeplyNested";
    private static final String SHOULD_NOT_BE_REQUIRED = "ShouldNotBeRequired";
    private static final String WRONG_SHAPE_TYPE = "WrongShapeType";

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex opIndex = OperationIndex.of(model);
        TopDownIndex topDown = TopDownIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();

        for (OperationShape operation : model.getOperationShapesWithTrait(PaginatedTrait.class)) {
            PaginatedTrait paginatedTrait = operation.expectTrait(PaginatedTrait.class);
            events.addAll(validateOperation(model, topDown, opIndex, operation, paginatedTrait));
        }

        return events;
    }

    private List<ValidationEvent> validateOperation(
            Model model,
            TopDownIndex topDownIndex,
            OperationIndex opIndex,
            OperationShape operation,
            PaginatedTrait trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        // Validate input.
        events.addAll(validateMember(opIndex, model, null, operation, trait, new InputTokenValidator()));
        PageSizeValidator pageSizeValidator = new PageSizeValidator();
        events.addAll(validateMember(opIndex, model, null, operation, trait, pageSizeValidator));
        pageSizeValidator.getMember(model, opIndex, operation, trait)
                .filter(MemberShape::isRequired)
                .ifPresent(member -> events.add(warning(
                        operation,
                        trait,
                        String.format(
                                "paginated trait `%s` member `%s` should not be required",
                                pageSizeValidator.propertyName(),
                                member.getMemberName()),
                        SHOULD_NOT_BE_REQUIRED,
                        pageSizeValidator.propertyName())));

        // Validate output.
        events.addAll(validateMember(opIndex, model, null, operation, trait, new OutputTokenValidator()));
        events.addAll(validateMember(opIndex, model, null, operation, trait, new ItemValidator()));

        if (events.isEmpty()) {
            model.shapes(ServiceShape.class).forEach(svc -> {
                if (topDownIndex.getContainedOperations(svc).contains(operation)) {
                    // Create a merged trait if one is present on the service.
                    PaginatedTrait merged = svc.getTrait(PaginatedTrait.class).map(trait::merge).orElse(trait);
                    events.addAll(validateMember(opIndex, model, svc, operation, merged, new InputTokenValidator()));
                    events.addAll(validateMember(opIndex, model, svc, operation, merged, new PageSizeValidator()));
                    events.addAll(validateMember(opIndex, model, svc, operation, merged, new OutputTokenValidator()));
                    events.addAll(validateMember(opIndex, model, svc, operation, merged, new ItemValidator()));
                }
            });
        }

        return events;
    }

    private List<ValidationEvent> validateMember(
            OperationIndex opIndex,
            Model model,
            ServiceShape service,
            OperationShape operation,
            PaginatedTrait trait,
            PropertyValidator validator
    ) {
        String prefix = service != null ? "When bound within the `" + service.getId() + "` service, " : "";
        String memberPath = validator.getMemberPath(opIndex, operation, trait).orElse(null);

        if (memberPath == null) {
            return service != null && validator.isRequiredToBePresent()
                    ? Collections.singletonList(error(operation,
                            trait,
                            String.format(
                                    "%spaginated trait `%s` is not configured",
                                    prefix,
                                    validator.propertyName())))
                    : Collections.emptyList();
        }

        if (!validator.pathsAllowed() && memberPath.contains(".")) {
            return Collections.singletonList(error(operation,
                    trait,
                    String.format(
                            "%spaginated trait `%s` does not allow path values",
                            prefix,
                            validator.propertyName())));
        }

        MemberShape member = validator.getMember(model, opIndex, operation, trait).orElse(null);
        if (member == null) {
            return Collections.singletonList(error(operation,
                    trait,
                    String.format(
                            "%spaginated trait `%s` targets a member `%s` that does not exist",
                            prefix,
                            validator.propertyName(),
                            memberPath)));
        }

        List<ValidationEvent> events = new ArrayList<>();
        if (validator.mustBeOptional() && member.isRequired()) {
            events.add(error(operation,
                    trait,
                    String.format(
                            "%spaginated trait `%s` member `%s` must not be required",
                            prefix,
                            validator.propertyName(),
                            member.getMemberName())));
        }

        Shape target = model.getShape(member.getTarget()).orElse(null);
        if (target != null) {
            if (!validator.validTargets().contains(target.getType())) {
                events.add(error(operation,
                        trait,
                        String.format(
                                "%spaginated trait `%s` member `%s` targets a %s shape, but must target one of "
                                        + "the following: [%s]",
                                prefix,
                                validator.propertyName(),
                                member.getId().getMember().get(),
                                target.getType(),
                                ValidationUtils.tickedList(validator.validTargets()))));
            }
            if (validator.dangerTargets().contains(target.getType())) {
                Set<ShapeType> preferredTargets = new TreeSet<>(validator.validTargets());
                preferredTargets.removeAll(validator.dangerTargets());
                String traitName = validator.propertyName();
                String memberName = member.getId().getMember().get();
                String targetType = target.getType().toString();
                events.add(danger(operation,
                        trait,
                        String.format(
                                "%spaginated trait `%s` member `%s` targets a %s shape, but this is not recommended. "
                                        + "One of [%s] SHOULD be targeted.",
                                prefix,
                                traitName,
                                memberName,
                                targetType,
                                ValidationUtils.tickedList(preferredTargets)),
                        WRONG_SHAPE_TYPE,
                        traitName));
            }
        }

        if (validator.pathsAllowed() && PATH_PATTERN.split(memberPath).length > 2) {
            events.add(warning(operation,
                    trait,
                    String.format(
                            "%spaginated trait `%s` contains a path with more than two parts, which can make your API "
                                    + "cumbersome to use",
                            prefix,
                            validator.propertyName()),
                    DEEPLY_NESTED,
                    validator.propertyName()));
        }

        return events;
    }

    private abstract static class PropertyValidator {
        abstract boolean mustBeOptional();

        abstract boolean isRequiredToBePresent();

        abstract String propertyName();

        abstract Set<ShapeType> validTargets();

        Set<ShapeType> dangerTargets() {
            return Collections.emptySet();
        }

        abstract Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait);

        abstract Optional<MemberShape> getMember(
                Model model,
                OperationIndex opIndex,
                OperationShape operation,
                PaginatedTrait trait
        );

        boolean pathsAllowed() {
            return false;
        }
    }

    private abstract static class OutputPropertyValidator extends PropertyValidator {

        @Override
        boolean pathsAllowed() {
            return true;
        }

        Optional<MemberShape> getMember(
                Model model,
                OperationIndex opIndex,
                OperationShape operation,
                PaginatedTrait trait
        ) {
            StructureShape outputShape = opIndex.expectOutputShape(operation);
            return getMemberPath(opIndex, operation, trait)
                    .map(path -> PaginatedTrait.resolveFullPath(path, model, outputShape))
                    .flatMap(memberShapes -> {
                        if (memberShapes.size() == 0) {
                            return Optional.empty();
                        }
                        return Optional.of(memberShapes.get(memberShapes.size() - 1));
                    });
        }
    }

    private static final class InputTokenValidator extends PropertyValidator {
        boolean mustBeOptional() {
            return true;
        }

        boolean isRequiredToBePresent() {
            return true;
        }

        String propertyName() {
            return "inputToken";
        }

        Set<ShapeType> validTargets() {
            return TOKEN_SHAPES;
        }

        Set<ShapeType> dangerTargets() {
            return DANGER_TOKEN_SHAPES;
        }

        Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
            return trait.getInputToken();
        }

        Optional<MemberShape> getMember(
                Model model,
                OperationIndex opIndex,
                OperationShape operation,
                PaginatedTrait trait
        ) {
            StructureShape input = opIndex.expectInputShape(operation);
            return getMemberPath(opIndex, operation, trait).flatMap(input::getMember);
        }
    }

    private static final class OutputTokenValidator extends OutputPropertyValidator {
        boolean mustBeOptional() {
            return true;
        }

        boolean isRequiredToBePresent() {
            return true;
        }

        String propertyName() {
            return "outputToken";
        }

        Set<ShapeType> validTargets() {
            return TOKEN_SHAPES;
        }

        Set<ShapeType> dangerTargets() {
            return DANGER_TOKEN_SHAPES;
        }

        Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
            return trait.getOutputToken();
        }
    }

    private static final class PageSizeValidator extends PropertyValidator {
        boolean mustBeOptional() {
            return false;
        }

        boolean isRequiredToBePresent() {
            return false;
        }

        String propertyName() {
            return "pageSize";
        }

        Set<ShapeType> validTargets() {
            return PAGE_SHAPES;
        }

        Set<ShapeType> dangerTargets() {
            return DANGER_PAGE_SHAPES;
        }

        Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
            return trait.getPageSize();
        }

        Optional<MemberShape> getMember(
                Model model,
                OperationIndex opIndex,
                OperationShape operation,
                PaginatedTrait trait
        ) {
            StructureShape input = opIndex.expectInputShape(operation);
            return getMemberPath(opIndex, operation, trait).flatMap(input::getMember);
        }
    }

    private static final class ItemValidator extends OutputPropertyValidator {
        boolean mustBeOptional() {
            return false;
        }

        boolean isRequiredToBePresent() {
            return false;
        }

        String propertyName() {
            return "items";
        }

        Set<ShapeType> validTargets() {
            return ITEM_SHAPES;
        }

        Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
            return trait.getItems();
        }
    }
}
