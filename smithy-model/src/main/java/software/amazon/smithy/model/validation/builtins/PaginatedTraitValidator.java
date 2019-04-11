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

package software.amazon.smithy.model.validation.builtins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates paginated traits.
 *
 * <ul>
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
 *
 * @see PaginatedIndex for more pagination validation.
 */
public final class PaginatedTraitValidator extends AbstractValidator {
    private static final Set<ShapeType> ITEM_SHAPES = SetUtils.of(ShapeType.LIST, ShapeType.MAP);
    private static final Set<ShapeType> PAGE_SHAPES = SetUtils.of(ShapeType.INTEGER);
    private static final Set<ShapeType> STRING_SET = SetUtils.of(ShapeType.STRING);

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex shapeIndex = model.getShapeIndex();
        PaginatedIndex paginatedIndex = model.getKnowledge(PaginatedIndex.class);
        List<ValidationEvent> events = new ArrayList<>(paginatedIndex.getValidationEvents());
        events.addAll(paginatedIndex.getAllPaginatedInfo().stream()
                .flatMap(info -> validateInfo(shapeIndex, info).stream())
                .collect(Collectors.toList()));
        return events;
    }

    private List<ValidationEvent> validateInfo(ShapeIndex index, PaginatedIndex.PaginationInfo info) {
        List<ValidationEvent> events = new ArrayList<>();

        checkType(info, index, "input", "inputToken", info.getInputTokenMember(), STRING_SET).ifPresent(events::add);
        if (!info.getInputTokenMember().isOptional()) {
            events.add(mustBeOptional(info, "input", "inputToken", info.getInputTokenMember()));
        }

        checkType(info, index, "output", "outputToken", info.getInputTokenMember(), STRING_SET).ifPresent(events::add);
        if (!info.getOutputTokenMember().isOptional()) {
            events.add(mustBeOptional(info, "output", "outputToken", info.getOutputTokenMember()));
        }

        info.getItemsMember().ifPresent(items -> {
            checkType(info, index, "output", "items", items, ITEM_SHAPES).ifPresent(events::add);
        });

        info.getPageSizeMember().ifPresent(member -> {
            checkType(info, index, "input", "pageSize", member, PAGE_SHAPES).ifPresent(events::add);
            if (member.isRequired()) {
                events.add(mustBeOptional(info, "input", "pageSize", member));
            }
        });

        return events;
    }

    private Optional<ValidationEvent> checkType(
            PaginatedIndex.PaginationInfo info,
            ShapeIndex index,
            String inputOrOutput,
            String propertyName,
            MemberShape member,
            Set<ShapeType> types
    ) {
        return index.getShape(member.getTarget())
                .filter(target -> !types.contains(target.getType()))
                .map(target -> {
                    String typePrefix = types.size() == 1 ? "a(n)" : "one of the following types:";
                    return error(info.getOperation(), info.getPaginatedTrait(), String.format(
                            "`paginated` trait `%s` references %s member `%s`, a member that targets a %s "
                            + "shape, but %s must reference a member that targets %s %s.",
                            propertyName, inputOrOutput, member.getId().getName(),
                            target.getType(), propertyName, typePrefix, ValidationUtils.tickedList(types)));
                });
    }

    private ValidationEvent mustBeOptional(
            PaginatedIndex.PaginationInfo info,
            String struct, String prop,
            MemberShape member
    ) {
        return error(info.getOperation(), info.getPaginatedTrait(), String.format(
                "`paginated` trait `%s` must reference an optional %s structure member, but found a "
                + "reference to `%s`, a required member.", prop, struct, member.getId()));
    }
}
