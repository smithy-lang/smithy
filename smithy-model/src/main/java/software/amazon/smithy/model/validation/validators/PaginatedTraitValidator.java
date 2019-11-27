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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    private static final Pattern PATH_PATTERN = Pattern.compile("\\.");

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);
        TopDownIndex topDown = model.getKnowledge(TopDownIndex.class);

        return model.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, PaginatedTrait.class))
                .flatMap(pair -> validateOperation(model, topDown, opIndex, pair.left, pair.right).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(
            Model model,
            TopDownIndex topDownIndex,
            OperationIndex opIndex,
            OperationShape operation,
            PaginatedTrait trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        if (!opIndex.getInput(operation).isPresent()) {
            events.add(error(operation, trait, "paginated operations require an input"));
        } else {
            events.addAll(validateMember(opIndex, model, null, operation, trait, new InputTokenValidator()));
            PageSizeValidator pageSizeValidator = new PageSizeValidator();
            events.addAll(validateMember(opIndex, model, null, operation, trait, pageSizeValidator));
            pageSizeValidator.getMember(model, opIndex, operation, trait)
                    .filter(MemberShape::isRequired)
                    .ifPresent(member -> events.add(danger(operation, trait, String.format(
                            "paginated trait `%s` member `%s` should not be required",
                            pageSizeValidator.propertyName(), member.getMemberName()))));
        }

        if (!opIndex.getOutput(operation).isPresent()) {
            events.add(error(operation, trait, "paginated operations require an output"));
        } else {
            events.addAll(validateMember(opIndex, model, null, operation, trait, new OutputTokenValidator()));
            events.addAll(validateMember(opIndex, model, null, operation, trait, new ItemValidator()));
        }

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
                   ? Collections.singletonList(error(operation, trait, String.format(
                    "%spaginated trait `%s` is not configured", prefix, validator.propertyName())))
                   : Collections.emptyList();
        }

        if (!validator.pathsAllowed() && memberPath.contains(".")) {
            return Collections.singletonList(error(operation, trait, String.format(
                    "%spaginated trait `%s` does not allow path values", prefix, validator.propertyName()
            )));
        }

        MemberShape member = validator.getMember(model, opIndex, operation, trait).orElse(null);
        if (member == null) {
            return Collections.singletonList(error(operation, trait, String.format(
                    "%spaginated trait `%s` targets a member `%s` that does not exist",
                    prefix, validator.propertyName(), memberPath)));
        }

        List<ValidationEvent> events = new ArrayList<>();
        if (validator.mustBeOptional() && member.isRequired()) {
            events.add(error(operation, trait, String.format(
                    "%spaginated trait `%s` member `%s` must not be required",
                    prefix, validator.propertyName(), member.getMemberName())));
        }

        Shape target = model.getShape(member.getTarget()).orElse(null);
        if (target != null && !validator.validTargets().contains(target.getType())) {
            events.add(error(operation, trait, String.format(
                    "%spaginated trait `%s` member `%s` targets a %s shape, but must target one of "
                    + "the following: [%s]",
                    prefix, validator.propertyName(), member.getId().getName(), target.getType(),
                    ValidationUtils.tickedList(validator.validTargets()))));
        }

        if (validator.pathsAllowed() && PATH_PATTERN.split(memberPath).length > 2) {
            events.add(warning(operation, trait, String.format(
                    "%spaginated trait `%s` contains a path with more than two parts, which can make your API "
                        + "cumbersome to use",
                    prefix, validator.propertyName()
            )));
        }

        return events;
    }

    private abstract static class PropertyValidator {
        abstract boolean mustBeOptional();

        abstract boolean isRequiredToBePresent();

        abstract String propertyName();

        abstract Set<ShapeType> validTargets();

        abstract Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait);

        abstract Optional<MemberShape> getMember(
                Model model, OperationIndex opIndex, OperationShape operation, PaginatedTrait trait
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
                Model model, OperationIndex opIndex, OperationShape operation, PaginatedTrait trait
        ) {
            // Split up the path expression into a list of member names
            List<String> memberNames = getMemberPath(opIndex, operation, trait)
                    .map(value -> Arrays.asList(PATH_PATTERN.split(value)))
                    .orElse(Collections.emptyList());
            if (memberNames.isEmpty()) {
                return Optional.empty();
            }
            Optional<StructureShape> outputShape = opIndex.getOutput(operation);
            if (!outputShape.isPresent()) {
                return Optional.empty();
            }

            // Grab the first member from the output shape.
            Optional<MemberShape> memberShape = outputShape.get().getMember(memberNames.get(0));

            // For each member name in the path except the first, try to find that member in the previous structure
            for (String memberName: memberNames.subList(1, memberNames.size())) {
                if (!memberShape.isPresent()) {
                    return Optional.empty();
                }
                memberShape = model.getShape(memberShape.get().getTarget())
                        .flatMap(Shape::asStructureShape)
                        .flatMap(target -> target.getMember(memberName));
            }
            return memberShape;
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
            return STRING_SET;
        }

        Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
            return trait.getInputToken();
        }

        Optional<MemberShape> getMember(
                Model model, OperationIndex opIndex, OperationShape operation, PaginatedTrait trait
        ) {
            return getMemberPath(opIndex, operation, trait)
                    .flatMap(memberName -> opIndex.getInput(operation).flatMap(input -> input.getMember(memberName)));
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
            return STRING_SET;
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

        Optional<String> getMemberPath(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
            return trait.getPageSize();
        }

        Optional<MemberShape> getMember(
                Model model, OperationIndex opIndex, OperationShape operation, PaginatedTrait trait
        ) {
            return getMemberPath(opIndex, operation, trait)
                    .flatMap(memberName -> opIndex.getInput(operation).flatMap(input -> input.getMember(memberName)));
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
