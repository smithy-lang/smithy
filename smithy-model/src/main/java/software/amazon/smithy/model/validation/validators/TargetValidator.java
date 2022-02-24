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

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Validates that neighbors target resolvable shapes of the correct type.
 */
public final class TargetValidator extends AbstractValidator {

    private static final int MAX_EDIT_DISTANCE_FOR_SUGGESTIONS = 2;
    private static final Set<ShapeType> INVALID_MEMBER_TARGETS = SetUtils.of(
            ShapeType.SERVICE, ShapeType.RESOURCE, ShapeType.OPERATION, ShapeType.MEMBER);
    private static final Set<ShapeType> VALID_SET_TARGETS = SetUtils.of(
            ShapeType.STRING, ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER, ShapeType.LONG,
            ShapeType.BIG_INTEGER, ShapeType.BIG_DECIMAL, ShapeType.BLOB);

    @Override
    public List<ValidationEvent> validate(Model model) {
        NeighborProvider neighborProvider = NeighborProviderIndex.of(model).getProvider();
        return model.shapes()
                .flatMap(shape -> validateShape(model, shape, neighborProvider.getNeighbors(shape)))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateShape(Model model, Shape shape, List<Relationship> relationships) {
        return relationships.stream().flatMap(relationship -> {
            if (relationship.getNeighborShape().isPresent()) {
                return OptionalUtils.stream(
                        validateTarget(model, shape, relationship.getNeighborShape().get(), relationship));
            } else {
                return Stream.of(unresolvedTarget(model, shape, relationship));
            }
        });
    }

    private Optional<ValidationEvent> validateTarget(Model model, Shape shape, Shape target, Relationship rel) {
        RelationshipType relType = rel.getRelationshipType();

        if (relType.getDirection() == RelationshipDirection.DIRECTED && target.hasTrait(TraitDefinition.class)) {
            return Optional.of(error(shape, format(
                    "Found a %s reference to trait definition `%s`. Trait definitions cannot be targeted by "
                    + "members or referenced by shapes in any other context other than applying them as "
                    + "traits.", relType, rel.getNeighborShapeId())));
        }

        switch (relType) {
            case MEMBER_TARGET:
                // Members cannot target other members, service, operation, or resource shapes.
                if (INVALID_MEMBER_TARGETS.contains(target.getType())) {
                    return Optional.of(error(shape, format(
                            "Members cannot target %s shapes, but found %s", target.getType(), target)));
                }
            case MAP_KEY:
                return target.asMemberShape().flatMap(m -> validateMapKey(shape, m.getTarget(), model));
            case SET_MEMBER:
                return target.asMemberShape().flatMap(m -> validateSetMember(shape, m.getTarget(), model));
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

    private Optional<ValidationEvent> validateMapKey(Shape shape, ShapeId target, Model model) {
        return model.getShape(target)
                .filter(FunctionalUtils.not(Shape::isStringShape))
                .map(resolved -> error(shape, format(
                        "Map key member targets %s, but is expected to target a string", resolved)));
    }

    private Optional<ValidationEvent> validateSetMember(Shape shape, ShapeId target, Model model) {
        Shape targetShape = model.getShape(target).orElse(null);

        if (targetShape == null) {
            return Optional.empty();
        }

        if (!VALID_SET_TARGETS.contains(targetShape.getType())) {
            return Optional.of(error(shape, format(
                    "Set member targets %s, but sets can target only %s. You can model a collection of %s shapes "
                    + "by changing this shape to a list. Modeling a set of values of other types is problematic "
                    + "to support across a wide range of programming languages.",
                    targetShape,
                    VALID_SET_TARGETS.stream().map(ShapeType::toString).sorted().collect(Collectors.joining(", ")),
                    targetShape.getType())));
        } else if (targetShape.hasTrait(StreamingTrait.class)) {
            return Optional.of(error(shape, format("Set member targets %s, a shape marked with the @streaming trait. "
                                                   + "Sets do not support unbounded values.",
                                                   targetShape)));
        }

        return Optional.empty();
    }

    private Optional<ValidationEvent> validateIdentifier(Shape shape, Shape target) {
        if (target.getType() != ShapeType.STRING) {
            return Optional.of(badType(shape, target, RelationshipType.IDENTIFIER, ShapeType.STRING));
        } else {
            return Optional.empty();
        }
    }

    private ValidationEvent unresolvedTarget(Model model, Shape shape, Relationship rel) {
        // Detect if there are shape IDs within 2 characters edit distance from the
        // invalid shape ID target. An edit distance > 2 seems to give far more false
        // positives than are useful.
        Collection<String> suggestions = computeTargetSuggestions(model, rel.getNeighborShapeId());
        String suggestionText = !suggestions.isEmpty()
                ? ". Did you mean " + String.join(", ", suggestions) + "?"
                : "";

        if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
            // Don't show the relationship type for invalid member targets.
            return error(shape, String.format(
                    "member shape targets an unresolved shape `%s`%s", rel.getNeighborShapeId(), suggestionText));
        } else {
            // Use "a" or "an" depending on if the relationship starts with a vowel.
            String indefiniteArticle = isUppercaseVowel(rel.getRelationshipType().toString().charAt(0))
                    ? "an"
                    : "a";
            return error(shape, String.format(
                    "%s shape has %s `%s` relationship to an unresolved shape `%s`%s",
                    shape.getType(),
                    indefiniteArticle,
                    rel.getRelationshipType().toString().toLowerCase(Locale.US),
                    rel.getNeighborShapeId(),
                    suggestionText));
        }
    }

    private Collection<String> computeTargetSuggestions(Model model, ShapeId target) {
        String targetString = target.toString();
        int floor = Integer.MAX_VALUE;
        // Sort the result to create deterministic error messages.
        Set<String> candidates = new TreeSet<>();

        for (Shape shape : model.toSet()) {
            String idString = shape.getId().toString();
            int distance = StringUtils.levenshteinDistance(targetString, idString, MAX_EDIT_DISTANCE_FOR_SUGGESTIONS);
            if (distance == floor) {
                // Add to the list of candidates that have the same distance.
                candidates.add(idString);
            } else if (distance > -1 && distance < floor) {
                // Found a new ID that has a lower distance. Set the new floor,
                // clear out the previous candidates, and add this ID.
                floor = distance;
                candidates.clear();
                candidates.add(idString);
            }
        }

        return candidates;
    }

    private static boolean isUppercaseVowel(char c) {
        return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
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
                "`%s` cannot be bound as an error because it is not marked with the `error` trait.",
                target));
    }
}
