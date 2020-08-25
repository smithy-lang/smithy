/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates the streaming trait.
 *
 * <p>Ensures that the targeted shapes are only referenced by top level input/output structures.
 *
 * <p>If the targeted shape is a union, ensures that all members are structures.
 */
public final class StreamingTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(StreamingTrait.class)) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = validateStreamingTargets(model);
        events.addAll(validateAllEventStreamMembers(model));
        return events;
    }

    private List<ValidationEvent> validateStreamingTargets(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider provider = NeighborProviderIndex.of(model).getReverseProvider();

        // Find any containers that reference a streaming trait.
        Set<Shape> streamingStructures = model.shapes(MemberShape.class)
                .filter(member -> member.getMemberTrait(model, StreamingTrait.class).isPresent())
                .map(member -> model.expectShape(member.getContainer()))
                .collect(Collectors.toSet());

        for (Shape shape : streamingStructures) {
            for (Relationship rel : provider.getNeighbors(shape)) {
                if (rel.getRelationshipType() != RelationshipType.INPUT
                        && rel.getRelationshipType() != RelationshipType.OUTPUT
                        && rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                    events.add(error(rel.getShape(), String.format(
                            "This shape has an invalid `%s` relationship to a structure, `%s`, that contains "
                            + "a stream", rel.getRelationshipType(), shape.getId())));
                }
            }
        }

        return events;
    }

    private List<ValidationEvent> validateAllEventStreamMembers(Model model) {
        return model.shapes(UnionShape.class)
                .filter(shape -> shape.hasTrait(StreamingTrait.class))
                .flatMap(shape -> validateUnionMembers(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateUnionMembers(Model model, UnionShape shape) {
        // Find members that don't reference a structure and combine
        // these member names into a comma separated list.
        String invalidMembers = shape.getAllMembers().values().stream()
                .map(em -> Pair.of(em.getMemberName(), model.getShape(em.getTarget()).orElse(null)))
                .filter(pair -> pair.getRight() != null && !(pair.getRight() instanceof StructureShape))
                .map(Pair::getLeft)
                .sorted()
                .collect(Collectors.joining(", "));
        if (!invalidMembers.isEmpty()) {
            return ListUtils.of(error(shape, String.format(
                    "Each member of an event stream union must target a structure shape, but the following union "
                    + "members do not: [%s]", invalidMembers)));
        }
        return Collections.emptyList();
    }
}
