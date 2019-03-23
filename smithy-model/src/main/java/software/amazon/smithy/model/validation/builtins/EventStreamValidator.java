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
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.InputEventStreamTrait;
import software.amazon.smithy.model.traits.OutputEventStreamTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates inputEventStream and outputEventStream traits.
 *
 * <ul>
 *     <li>Ensures that event stream members are present.</li>
 *     <li>Ensures that event stream members are not required.</li>
 *     <li>Ensures that event stream members target structures or unions.</li>
 *     <li>Ensures that event stream union members are all structures.</li>
 * </ul>
 */
public class EventStreamValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        List<ValidationEvent> events = new ArrayList<>();

        model.getShapeIndex().shapes(OperationShape.class)
                .flatMap(operation -> Trait.flatMapStream(operation, InputEventStreamTrait.class))
                .forEach(pair -> {
                    OperationShape operation = pair.getLeft();
                    ShapeId input = operation.getInput().orElse(null);
                    InputEventStreamTrait trait = pair.getRight();
                    events.addAll(check(index, operation, input, trait, trait.getValue()));
                });

        model.getShapeIndex().shapes(OperationShape.class)
                .flatMap(operation -> Trait.flatMapStream(operation, OutputEventStreamTrait.class))
                .forEach(pair -> {
                    OperationShape operation = pair.getLeft();
                    ShapeId input = operation.getOutput().orElse(null);
                    OutputEventStreamTrait trait = pair.getRight();
                    events.addAll(check(index, operation, input, trait, trait.getValue()));
                });

        return events;
    }

    private List<ValidationEvent> check(
            ShapeIndex index,
            OperationShape operation,
            ShapeId inputOutput,
            Trait trait,
            String member
    ) {
        String inputOrOutputName = trait instanceof InputEventStreamTrait ? "input" : "output";

        if (inputOutput == null) {
            // The operation has no input/output, so this is the only check.
            return List.of(error(operation, trait, String.format(
                    "Operation has the `%s` but does not define an %s structure.",
                    trait.getRelativeName(), inputOrOutputName)));
        }

        StructureShape struct = index.getShape(inputOutput).flatMap(Shape::asStructureShape).orElse(null);
        if (struct == null) {
            // Broken targets are validated elsewhere.
            return List.of();
        }

        MemberShape actualMember = struct.getMember(member).orElse(null);
        if (actualMember == null) {
            // Stop because member-specific validation can't be performed.
            return List.of(error(operation, trait, String.format(
                    "Operation %s member `%s` was not found for the `%s` trait.",
                    inputOrOutputName, member, trait.getRelativeName())));
        }

        List<ValidationEvent> events = new ArrayList<>();
        if (actualMember.isRequired()) {
            events.add(error(operation, trait, String.format(
                    "Operation %s member `%s` is referenced by an `%s` trait, so it cannot be marked as required.",
                    inputOrOutputName, actualMember.getId(), trait.getRelativeName())));
        }

        // Additional event stream specific validation can't be performed,
        // and broken member target validation is covered elsewhere.
        index.getShape(actualMember.getTarget()).ifPresent(referencedMember -> {
            events.addAll(checkReferencedMember(
                    index, operation, trait, actualMember, referencedMember, inputOrOutputName));
        });

        return events;
    }

    private List<ValidationEvent> checkReferencedMember(
            ShapeIndex index,
            OperationShape operation,
            Trait trait,
            MemberShape member,
            Shape referencedMember,
            String inputOrOutputName
    ) {
        if (referencedMember.asUnionShape().isPresent()) {
            // Find members that don't reference a structure and combine
            // these member names into a comma separated list.
            String invalidMembers = referencedMember.asUnionShape().get().getAllMembers().values().stream()
                    .map(em -> new Pair<>(em.getMemberName(), index.getShape(em.getTarget()).orElse(null)))
                    .filter(pair -> pair.getRight() != null && !(pair.getRight() instanceof StructureShape))
                    .map(Pair::getLeft)
                    .sorted()
                    .collect(Collectors.joining(", "));
            if (!invalidMembers.isEmpty()) {
                return List.of(error(operation, trait, String.format(
                        "Operation %s member `%s` targets an invalid union `%s`; each member of an event "
                        + "stream union must target a structure shape, but the following union members do not: [%s]",
                        inputOrOutputName, member.getId(), referencedMember.getId(), invalidMembers)));
            }
        } else if (!referencedMember.isStructureShape()) {
            return List.of(error(operation, trait, String.format(
                    "Operation %s member `%s` is referenced by the `%s` trait, so it must target a structure "
                    + "or union, but found %s, a %s.",
                    inputOrOutputName, member.getId(), trait.getRelativeName(),
                    referencedMember.getId(), referencedMember.getType())));
        }

        return List.of();
    }
}
