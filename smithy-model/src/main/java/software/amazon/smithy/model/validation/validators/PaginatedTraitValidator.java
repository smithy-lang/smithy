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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.Trait;
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
 *     <li>The pageSize property, if set, must reference an optional integer
 *      input member.</li>
 *     <li>The inputToken property must reference an optional string input
 *      member.</li>
 *     <li>The outputToken property must reference an optional string output
 *      member.</li>
 *     <li>The pageSize property, if set, must reference an optional input
 *      member that targets an integer.</li>
 * </ul>
 */
public final class PaginatedTraitValidator extends AbstractValidator {
    private static final Set<ShapeType> ITEM_SHAPES = SetUtils.of(ShapeType.LIST, ShapeType.MAP);
    private static final Set<ShapeType> PAGE_SHAPES = SetUtils.of(ShapeType.INTEGER);
    private static final Set<ShapeType> STRING_SET = SetUtils.of(ShapeType.STRING);

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex shapeIndex = model.getShapeIndex();
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);
        TopDownIndex topDown = model.getKnowledge(TopDownIndex.class);

        return shapeIndex.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, PaginatedTrait.class))
                .flatMap(pair -> validateOperation(shapeIndex, topDown, opIndex, pair.left, pair.right).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(
            ShapeIndex index,
            TopDownIndex topDownIndex,
            OperationIndex opIndex,
            OperationShape operation,
            PaginatedTrait trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        if (!opIndex.getInput(operation).isPresent()) {
            events.add(error(operation, trait, "paginated operations require an input"));
        } else {
            events.addAll(validateMember(opIndex, index, null, operation, trait, new InputTokenValidator()));
            events.addAll(validateMember(opIndex, index, null, operation, trait, new PageSizeValidator()));
        }

        if (!opIndex.getOutput(operation).isPresent()) {
            events.add(error(operation, trait, "paginated operations require an output"));
        } else {
            events.addAll(validateMember(opIndex, index, null, operation, trait, new OutputTokenValidator()));
            events.addAll(validateMember(opIndex, index, null, operation, trait, new ItemValidator()));
        }

        if (events.isEmpty()) {
            index.shapes(ServiceShape.class).forEach(svc -> {
                if (topDownIndex.getContainedOperations(svc).contains(operation)) {
                    events.addAll(validateMember(opIndex, index, svc, operation, trait, new InputTokenValidator()));
                    events.addAll(validateMember(opIndex, index, svc, operation, trait, new PageSizeValidator()));
                    events.addAll(validateMember(opIndex, index, svc, operation, trait, new OutputTokenValidator()));
                    events.addAll(validateMember(opIndex, index, svc, operation, trait, new ItemValidator()));
                }
            });
        }

        return events;
    }

    private List<ValidationEvent> validateMember(
            OperationIndex opIndex,
            ShapeIndex index,
            ServiceShape service,
            OperationShape operation,
            PaginatedTrait trait,
            PropertyValidator validator
    ) {
        String prefix = service != null ? "When bound within the `" + service.getId() + "` service, " : "";
        String memberName = validator.getMemberName(opIndex, operation, trait).orElse(null);

        if (memberName == null) {
            return service != null && validator.isRequiredToBePresent()
                   ? Collections.singletonList(error(operation, trait, String.format(
                    "%spaginated trait `%s` is not configured", prefix, validator.propertyName())))
                   : Collections.emptyList();
        }

        MemberShape member = validator.getMember(opIndex, operation, trait).orElse(null);
        if (member == null) {
            return Collections.singletonList(error(operation, trait, String.format(
                    "%spaginated trait `%s` targets a member `%s` that does not exist",
                    prefix, validator.propertyName(), memberName)));
        }

        List<ValidationEvent> events = new ArrayList<>();
        if (validator.mustBeOptional() && member.isRequired()) {
            events.add(error(operation, trait, String.format(
                    "%spaginated trait `%s` member `%s` must not be required",
                    prefix, validator.propertyName(), member.getMemberName())));
        }

        Shape target = index.getShape(member.getTarget()).orElse(null);
        if (target != null && !validator.validTargets().contains(target.getType())) {
            events.add(error(operation, trait, String.format(
                    "%spaginated trait `%s` member `%s` targets a %s shape, but must target one of "
                    + "the following: [%s]",
                    prefix, validator.propertyName(), member.getId().getName(), target.getType(),
                    ValidationUtils.tickedList(validator.validTargets()))));
        }

        return events;
    }

    private abstract static class PropertyValidator {
        abstract boolean mustBeOptional();

        abstract boolean isRequiredToBePresent();

        abstract String propertyName();

        abstract Set<ShapeType> validTargets();

        abstract Optional<String> getMemberName(OperationIndex index, OperationShape operation, PaginatedTrait trait);

        abstract Optional<MemberShape> getMember(OperationIndex index, OperationShape operation, PaginatedTrait trait);
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
            return STRING_SET;
        }

        Optional<String> getMemberName(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return trait.getInputToken();
        }

        Optional<MemberShape> getMember(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return getMemberName(index, operation, trait)
                    .flatMap(memberName -> index.getInput(operation).flatMap(input -> input.getMember(memberName)));
        }
    }

    private static final class OutputTokenValidator extends PropertyValidator {
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
            return STRING_SET;
        }

        Optional<String> getMemberName(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return trait.getOutputToken();
        }

        Optional<MemberShape> getMember(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return getMemberName(index, operation, trait)
                    .flatMap(memberName -> index.getOutput(operation).flatMap(output -> output.getMember(memberName)));
        }
    }

    private static final class PageSizeValidator extends PropertyValidator {
        boolean mustBeOptional() {
            return true;
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

        Optional<String> getMemberName(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return trait.getPageSize();
        }

        Optional<MemberShape> getMember(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return getMemberName(index, operation, trait)
                    .flatMap(memberName -> index.getInput(operation).flatMap(input -> input.getMember(memberName)));
        }
    }

    private static final class ItemValidator extends PropertyValidator {
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

        Optional<String> getMemberName(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return trait.getItems();
        }

        Optional<MemberShape> getMember(OperationIndex index, OperationShape operation, PaginatedTrait trait) {
            return getMemberName(index, operation, trait)
                    .flatMap(memberName -> index.getOutput(operation).flatMap(output -> output.getMember(memberName)));
        }
    }
}
