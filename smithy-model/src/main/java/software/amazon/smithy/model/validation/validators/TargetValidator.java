/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Validates that neighbors target resolvable shapes of the correct type.
 */
public final class TargetValidator extends AbstractValidator {

    private static final int MAX_EDIT_DISTANCE_FOR_SUGGESTIONS = 2;
    private static final Set<ShapeType> INVALID_MEMBER_TARGETS = SetUtils.of(
            ShapeType.SERVICE,
            ShapeType.RESOURCE,
            ShapeType.OPERATION,
            ShapeType.MEMBER);

    private static final String UNRESOLVED_SHAPE_PART = "UnresolvedShape";

    // Relationship types listed here are checked to see if a shape refers to a deprecated shape.
    private static final Map<RelationshipType, String> RELATIONSHIP_TYPE_DEPRECATION_MAPPINGS = MapUtils.of(
            RelationshipType.MEMBER_TARGET,
            "Member targets a deprecated shape",
            RelationshipType.RESOURCE,
            "Binds a deprecated resource",
            RelationshipType.OPERATION,
            "Binds a deprecated operation",
            RelationshipType.IDENTIFIER,
            "Resource identifier targets a deprecated shape",
            RelationshipType.PROPERTY,
            "Resource property targets a deprecated shape",
            RelationshipType.INPUT,
            "Operation input targets a deprecated shape",
            RelationshipType.OUTPUT,
            "Operation output targets a deprecated shape",
            RelationshipType.ERROR,
            "Operation error targets a deprecated shape",
            RelationshipType.MIXIN,
            "Applies a deprecated mixin");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider neighborProvider = NeighborProviderIndex.of(model).getProvider();
        for (Shape shape : model.toSet()) {
            validateShape(model, shape, neighborProvider.getNeighbors(shape), events);
        }
        return events;
    }

    private void validateShape(
            Model model,
            Shape shape,
            List<Relationship> relationships,
            List<ValidationEvent> mutableEvents
    ) {
        for (Relationship relationship : relationships) {
            if (relationship.getNeighborShape().isPresent()) {
                validateTarget(model, shape, relationship.getNeighborShape().get(), relationship, mutableEvents);
            } else {
                mutableEvents.add(unresolvedTarget(model, shape, relationship));
            }
        }
    }

    private void validateTarget(
            Model model,
            Shape shape,
            Shape target,
            Relationship rel,
            List<ValidationEvent> events
    ) {
        RelationshipType relType = rel.getRelationshipType();

        if (relType != RelationshipType.MIXIN && relType.getDirection() == RelationshipDirection.DIRECTED) {
            if (target.hasTrait(TraitDefinition.class)) {
                events.add(error(shape,
                        format(
                                "Found a %s reference to trait definition `%s`. Trait definitions cannot be targeted by "
                                        + "members or referenced by shapes in any other context other than applying them as "
                                        + "traits.",
                                relType,
                                rel.getNeighborShapeId())));
                return;
            }

            // Ignoring members with the mixin trait, forbid shapes to reference mixins except as mixins.
            if (!target.isMemberShape() && target.hasTrait(MixinTrait.class)) {
                events.add(error(shape,
                        format(
                                "Illegal %s reference to mixin `%s`; shapes marked with the mixin trait can only be "
                                        + "referenced to apply them as a mixin.",
                                relType,
                                rel.getNeighborShapeId())));
                return;
            }
        }

        validateDeprecatedTargets(shape, target, relType, events);

        switch (relType) {
            case PROPERTY:
            case MEMBER_TARGET:
                // Members and property cannot target other members, service, operation, or resource shapes.
                if (INVALID_MEMBER_TARGETS.contains(target.getType())) {
                    events.add(error(shape,
                            format(
                                    "Members cannot target %s shapes, but found %s",
                                    target.getType(),
                                    target)));
                }
                break;
            case MAP_KEY:
                target.asMemberShape().ifPresent(m -> validateMapKey(shape, m.getTarget(), model, events));
                break;
            case RESOURCE:
                if (target.getType() != ShapeType.RESOURCE) {
                    events.add(badType(shape, target, relType, ShapeType.RESOURCE));
                }
                break;
            case OPERATION:
                if (target.getType() != ShapeType.OPERATION) {
                    events.add(badType(shape, target, relType, ShapeType.OPERATION));
                }
                break;
            case INPUT:
            case OUTPUT:
                // Input/output must target structures and cannot have the error trait.
                if (target.getType() != ShapeType.STRUCTURE) {
                    events.add(badType(shape, target, relType, ShapeType.STRUCTURE));
                } else if (target.findTrait("error").isPresent()) {
                    events.add(inputOutputWithErrorTrait(shape, target, rel.getRelationshipType()));
                }
                break;
            case ERROR:
                // Errors must target a structure with the error trait.
                if (target.getType() != ShapeType.STRUCTURE) {
                    events.add(badType(shape, target, relType, ShapeType.STRUCTURE));
                } else if (!target.findTrait("error").isPresent()) {
                    events.add(errorNoTrait(shape, target.getId()));
                }
                break;
            case IDENTIFIER:
                validateIdentifier(shape, target, events);
                break;
            case CREATE:
            case READ:
            case UPDATE:
            case DELETE:
            case LIST:
                if (target.getType() != ShapeType.OPERATION) {
                    events.add(error(shape,
                            format(
                                    "Resource %s lifecycle operation must target an operation, but found %s",
                                    relType.toString().toLowerCase(Locale.US),
                                    target)));
                }
                break;
            case MIXIN:
                if (!target.hasTrait(MixinTrait.class)) {
                    events.add(error(shape,
                            format(
                                    "Attempted to use %s as a mixin, but it is not marked with the mixin trait",
                                    target.getId())));
                }
                break;
            default:
                break;
        }
    }

    private void validateDeprecatedTargets(
            Shape shape,
            Shape target,
            RelationshipType relType,
            List<ValidationEvent> events
    ) {
        if (!target.hasTrait(DeprecatedTrait.class)) {
            return;
        }

        String relLabel = RELATIONSHIP_TYPE_DEPRECATION_MAPPINGS.get(relType);
        if (relLabel == null) {
            return;
        }

        StringBuilder builder = new StringBuilder(relLabel).append(", ").append(target.getId());
        DeprecatedTrait deprecatedTrait = target.expectTrait(DeprecatedTrait.class);
        deprecatedTrait.getMessage().ifPresent(message -> builder.append(". ").append(message));
        deprecatedTrait.getSince().ifPresent(since -> builder.append(" (since ").append(since).append(')'));
        events.add(ValidationEvent.builder()
                .id("DeprecatedShape." + target.getId())
                .severity(Severity.WARNING)
                .shape(shape)
                .message(builder.toString())
                .build());
    }

    private void validateMapKey(Shape shape, ShapeId target, Model model, List<ValidationEvent> events) {
        model.getShape(target).filter(FunctionalUtils.not(Shape::isStringShape)).ifPresent(resolved -> {
            String message = format("Map key member targets %s, but is expected to target a string", resolved);
            events.add(error(shape, message));
        });
    }

    private void validateIdentifier(Shape shape, Shape target, List<ValidationEvent> events) {
        if (target.getType() != ShapeType.STRING && target.getType() != ShapeType.ENUM) {
            events.add(badType(shape, target, RelationshipType.IDENTIFIER, ShapeType.STRING));
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
            return error(shape,
                    String.format(
                            "member shape targets an unresolved shape `%s`%s",
                            rel.getNeighborShapeId(),
                            suggestionText),
                    UNRESOLVED_SHAPE_PART);
        } else {
            // Use "a" or "an" depending on if the relationship starts with a vowel.
            String indefiniteArticle = isUppercaseVowel(rel.getRelationshipType().toString().charAt(0))
                    ? "an"
                    : "a";
            return error(shape,
                    String.format(
                            "%s shape has %s `%s` relationship to an unresolved shape `%s`%s",
                            shape.getType(),
                            indefiniteArticle,
                            rel.getRelationshipType().toString().toLowerCase(Locale.US),
                            rel.getNeighborShapeId(),
                            suggestionText),
                    UNRESOLVED_SHAPE_PART);
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
        return error(shape,
                format(
                        "%s shape `%s` relationships must target a %s shape, but found %s",
                        shape.getType(),
                        rel.toString().toLowerCase(Locale.US),
                        valid,
                        target));
    }

    private ValidationEvent inputOutputWithErrorTrait(Shape shape, Shape target, RelationshipType rel) {
        String descriptor = rel == RelationshipType.INPUT ? "input" : "output";
        return error(shape,
                format(
                        "Operation %s targets an invalid structure `%s` that is marked with the `error` trait.",
                        descriptor,
                        target.getId()));
    }

    private ValidationEvent errorNoTrait(Shape shape, ShapeId target) {
        return error(shape,
                format(
                        "`%s` cannot be bound as an error because it is not marked with the `error` trait.",
                        target));
    }
}
