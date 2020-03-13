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
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EventStreamTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates inputEventStream and outputEventStream traits.
 *
 * <ul>
 *     <li>Ensures that event stream members target structures or unions.</li>
 *     <li>Ensures that event stream union members are all structures.</li>
 *     <li>Ensures that structures with event streams are only targeted by input/output.</li>
 * </ul>
 */
public class EventStreamValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = model.getKnowledge(OperationIndex.class);
        List<ValidationEvent> events = new ArrayList<>();
        List<Shape> eventStreamStructures = new ArrayList<>();

        model.shapes(OperationShape.class).forEach(operation -> {
            operationIndex.getInput(operation).ifPresent(input -> {
                for (MemberShape member : input.getAllMembers().values()) {
                    if (member.hasTrait(EventStreamTrait.class)) {
                        eventStreamStructures.add(input);
                        events.addAll(check(model, operation, member, "input"));
                    }
                }
            });
            operationIndex.getOutput(operation).ifPresent(output -> {
                for (MemberShape member : output.getAllMembers().values()) {
                    if (member.hasTrait(EventStreamTrait.class)) {
                        eventStreamStructures.add(output);
                        events.addAll(check(model, operation, member, "output"));
                    }
                }
            });
        });

        events.addAll(validateEventStreamTargets(model, eventStreamStructures));

        return events;
    }

    private List<ValidationEvent> check(
            Model model,
            OperationShape operation,
            MemberShape member,
            String inputOrOutputName
    ) {
        Shape target = model.getShape(member.getTarget()).orElse(null);
        if (target == null) {
            return Collections.emptyList();
        }

        EventStreamTrait trait = member.expectTrait(EventStreamTrait.class);
        if (target.isUnionShape()) {
            // Find members that don't reference a structure and combine
            // these member names into a comma separated list.
            String invalidMembers = target.expectUnionShape().getAllMembers().values().stream()
                    .map(em -> Pair.of(em.getMemberName(), model.getShape(em.getTarget()).orElse(null)))
                    .filter(pair -> pair.getRight() != null && !(pair.getRight() instanceof StructureShape))
                    .map(Pair::getLeft)
                    .sorted()
                    .collect(Collectors.joining(", "));
            if (!invalidMembers.isEmpty()) {
                return ListUtils.of(error(operation, trait, String.format(
                        "Operation %s member `%s` targets an invalid union `%s`; each member of an event "
                        + "stream union must target a structure shape, but the following union members do not: [%s]",
                        inputOrOutputName, member.getId(), target.getId(), invalidMembers)));
            }
        } else if (!target.isStructureShape()) {
            return ListUtils.of(error(operation, trait, String.format(
                    "Operation %s member `%s` is referenced by the `%s` trait, so it must target a structure "
                    + "or union, but found %s, a %s.",
                    inputOrOutputName, member.getId(), trait.toShapeId().getName(),
                    target.getId(), target.getType())));
        }

        return ListUtils.of();
    }

    private List<ValidationEvent> validateEventStreamTargets(Model model, List<Shape> shapes) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider provider = model.getKnowledge(NeighborProviderIndex.class).getReverseProvider();

        for (Shape shape : shapes) {
            for (Relationship rel : provider.getNeighbors(shape)) {
                if (rel.getRelationshipType() != RelationshipType.INPUT
                        && rel.getRelationshipType() != RelationshipType.OUTPUT
                        && rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                    events.add(error(rel.getShape(), String.format(
                            "This shape has an invalid `%s` relationship to a structure, `%s`, that contains "
                            + "an event stream", rel.getRelationshipType(), shape.getId())));
                }
            }
        }

        return events;
    }
}
