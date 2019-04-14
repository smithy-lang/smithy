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

import static java.lang.String.format;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates that neighbors target resolvable shapes of the correct type.
 */
public class TargetValidator extends AbstractValidator {

    /** Valid member shape targets. */
    private static final Set<ShapeType> INVALID_MEMBER_TARGETS = SetUtils.of(
            ShapeType.SERVICE, ShapeType.RESOURCE, ShapeType.OPERATION, ShapeType.MEMBER);

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        NeighborProvider neighborProvider = model.getKnowledge(NeighborProviderIndex.class).getProvider();
        return index.shapes()
                .flatMap(shape -> validateShape(index, shape, neighborProvider.getNeighbors(shape)))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateShape(ShapeIndex index, Shape shape, List<Relationship> relationships) {
        return relationships.stream().flatMap(relationship -> {
            if (relationship.getNeighborShape().isPresent()) {
                return OptionalUtils.stream(
                        validateTarget(index, shape, relationship.getNeighborShape().get(), relationship));
            } else {
                return Stream.of(unresolvedTarget(shape, relationship));
            }
        });
    }

    private Optional<ValidationEvent> validateTarget(ShapeIndex index, Shape shape, Shape target, Relationship rel) {
        RelationshipType relType = rel.getRelationshipType();

        switch (relType) {
            case MEMBER_TARGET:
                // Members cannot target other members, service, operation, or resource shapes.
                if (INVALID_MEMBER_TARGETS.contains(target.getType())) {
                    return Optional.of(error(shape, format(
                            "Members cannot target %s shapes, but found %s", target.getType(), target)));
                }
            case MAP_KEY:
                return target.asMemberShape().flatMap(m -> validateMapKey(shape, m.getTarget(), index));
            case RESOURCE:
                if (target.getType() != ShapeType.RESOURCE) {
                    return Optional.of(badType(shape, target, relType, ShapeType.RESOURCE));
                }
                return Optional.empty();
            case OPERATION:
                if (target.getType() != ShapeType.OPERATION) {
                    return Optional.of(badType(shape, target, relType, ShapeType.OPERATION));
                }
                return Optional.empty();
            case INPUT:
            case OUTPUT:
                // Input/output must target structures and cannot have the error trait.
                if (target.getType() != ShapeType.STRUCTURE) {
                    return Optional.of(badType(shape, target, relType, ShapeType.STRUCTURE));
                } else if (target.findTrait("error").isPresent()) {
                    return Optional.of(inputOutputWithErrorTrait(shape, target, rel.getRelationshipType()));
                } else {
                    return Optional.empty();
                }
            case ERROR:
                // Errors must target a structure with the error trait.
                if (target.getType() != ShapeType.STRUCTURE) {
                    return Optional.of(badType(shape, target, relType, ShapeType.STRUCTURE));
                } else if (!target.findTrait("error").isPresent()) {
                    return Optional.of(errorNoTrait(shape, target.getId()));
                } else {
                    return Optional.empty();
                }
            case IDENTIFIER:
                return validateIdentifier(shape, target);
            case CREATE:
            case READ:
            case UPDATE:
            case DELETE:
            case LIST:
                if (target.getType() != ShapeType.OPERATION) {
                    return Optional.of(error(shape, format(
                            "Resource %s lifecycle operation must target an operation, but found %s",
                            relType.toString().toLowerCase(Locale.US), target)));
                } else {
                    return Optional.empty();
                }
            default:
                return Optional.empty();
        }
    }

    private Optional<ValidationEvent> validateMapKey(Shape shape, ShapeId target, ShapeIndex index) {
        return index.getShape(target)
                .filter(FunctionalUtils.not(Shape::isStringShape))
                .map(resolved -> error(shape, format(
                        "Map key member targets %s, but is expected to target a string", resolved)));
    }

    private Optional<ValidationEvent> validateIdentifier(Shape shape, Shape target) {
        if (target.getType() != ShapeType.STRING) {
            return Optional.of(badType(shape, target, RelationshipType.IDENTIFIER, ShapeType.STRING));
        } else {
            return Optional.empty();
        }
    }

    private ValidationEvent unresolvedTarget(Shape shape, Relationship rel) {
        if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
            return error(shape, String.format(
                    "member shape targets an unresolved shape `%s`", rel.getNeighborShapeId()));
        } else {
            return error(shape, String.format(
                    "%s shape has a `%s` relationship to an unresolved shape `%s`",
                    shape.getType(),
                    rel.getRelationshipType().toString().toLowerCase(Locale.US),
                    rel.getNeighborShapeId()));
        }
    }

    private ValidationEvent badType(Shape shape, Shape target, RelationshipType rel, ShapeType valid) {
        return error(shape, format(
                "%s shape `%s` relationships must target a %s shape, but found %s",
                shape.getType(), rel.toString().toLowerCase(Locale.US), valid, target));
    }

    private ValidationEvent inputOutputWithErrorTrait(Shape shape, Shape target, RelationshipType rel) {
        String descriptor = rel == RelationshipType.INPUT ? "input" : "output";
        return error(shape, format(
                "Operation %s targets an invalid structure `%s` that is marked with the `error` trait.",
                descriptor, target.getId()));
    }

    private ValidationEvent errorNoTrait(Shape shape, ShapeId target) {
        return error(shape, format(
                "Operation error targets an invalid structure, `%s`, that is not marked with the `error` trait.",
                target));
    }
}
