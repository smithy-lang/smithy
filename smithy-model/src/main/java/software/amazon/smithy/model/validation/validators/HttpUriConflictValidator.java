/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.pattern.SmithyPattern.Segment;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates that no two URIs in a service conflict with each other.
 */
public final class HttpUriConflictValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(HttpTrait.class)) {
            return Collections.emptyList();
        }

        return model.shapes(ServiceShape.class)
                .flatMap(shape -> validateService(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        List<OperationShape> operations = new ArrayList<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            if (operation.hasTrait(HttpTrait.class)) {
                operations.add(operation);
            }
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : operations) {
            events.addAll(checkConflicts(model, operation, operation.expectTrait(HttpTrait.class), operations));
        }

        return events;
    }

    private List<ValidationEvent> checkConflicts(
            Model model,
            OperationShape operation,
            HttpTrait httpTrait,
            List<OperationShape> operations
    ) {
        String method = httpTrait.getMethod();
        UriPattern pattern = httpTrait.getUri();

        // Some conflicts are potentially allowable, so we split them up into to lists.
        List<Pair<ShapeId, UriPattern>> conflicts = new ArrayList<>();
        List<Pair<ShapeId, UriPattern>> allowableConflicts = new ArrayList<>();

        for (OperationShape other : operations) {
            if (other != operation && other.hasTrait(HttpTrait.class)) {
                HttpTrait otherHttpTrait = other.expectTrait(HttpTrait.class);
                if (otherHttpTrait.getMethod().equals(method)
                        && otherHttpTrait.getUri().conflictsWith(pattern)
                        && endpointConflicts(model, operation, other)) {
                    // Now that we know we have a conflict, determine whether it is allowable or not.
                    if (isAllowableConflict(model, operation, other)) {
                        allowableConflicts.add(Pair.of(other.getId(), otherHttpTrait.getUri()));
                    } else {
                        conflicts.add(Pair.of(other.getId(), otherHttpTrait.getUri()));
                    }
                }
            }
        }

        // Non-allowable conflicts get turned into ERRORs, they must be resolved to pass validation.
        List<ValidationEvent> events = new ArrayList<>();
        if (!conflicts.isEmpty()) {
            events.add(error(operation, formatConflicts(pattern, conflicts)));
        }

        // Allowable conflicts get turned into DANGERs, which must at least be acknowledged with a suppression.
        if (!allowableConflicts.isEmpty()) {
            String message = formatConflicts(pattern, allowableConflicts)
                    + ". Pattern traits applied to the label members prevent the label value from evaluating to a "
                    + "conflict, but this is still a poor design. If this is acceptable, this can be suppressed.";
            events.add(danger(operation, message));
        }

        return events;
    }

    private boolean endpointConflicts(Model model, OperationShape operation, OperationShape otherOperation) {
        // If neither operation has the endpoint trait, then their endpoints do conflict.
        if (!operation.hasTrait(EndpointTrait.ID) && !otherOperation.hasTrait(EndpointTrait.ID)) {
            return true;
        }

        // If one, but not both, operations have the endpoint trait, then their endpoints can't conflict.
        if (operation.hasTrait(EndpointTrait.ID) ^ otherOperation.hasTrait(EndpointTrait.ID)) {
            return false;
        }

        // At this point we know both operations have the endpoint trait, so we need to check if
        // they conflict.
        SmithyPattern prefix = operation.getTrait(EndpointTrait.class).get().getHostPrefix();
        SmithyPattern otherPrefix = otherOperation.getTrait(EndpointTrait.class).get().getHostPrefix();
        boolean allowable = !isAllowableConflict(
                model,
                prefix,
                operation,
                otherPrefix,
                otherOperation,
                this::getHostLabelPatterns);
        return allowable;
    }

    private boolean isAllowableConflict(Model model, OperationShape operation, OperationShape otherOperation) {
        UriPattern uriPattern = operation.getTrait(HttpTrait.class).get().getUri();
        UriPattern otherUriPattern = otherOperation.getTrait(HttpTrait.class).get().getUri();
        return isAllowableConflict(
                model,
                uriPattern,
                operation,
                otherUriPattern,
                otherOperation,
                this::getHttpLabelPatterns);
    }

    private boolean isAllowableConflict(
            Model model,
            SmithyPattern pattern,
            OperationShape operation,
            SmithyPattern otherPattern,
            OperationShape otherOperation,
            BiFunction<Model, OperationShape, Map<String, Pattern>> getLabelPatterns
    ) {

        Map<Segment, Segment> conflictingLabelSegments = pattern.getConflictingLabelSegmentsMap(otherPattern);

        // If there aren't any conflicting label segments that means the uris are identical, which is not allowable.
        if (conflictingLabelSegments.isEmpty()) {
            return false;
        }

        Map<String, Pattern> labelPatterns = getLabelPatterns.apply(model, operation);
        Map<String, Pattern> otherLabelPatterns = getLabelPatterns.apply(model, otherOperation);

        return conflictingLabelSegments.entrySet()
                .stream()
                // Only allow conflicts in cases where one of the segments is static and the other is a label.
                .filter(conflict -> conflict.getKey().isLabel() != conflict.getValue().isLabel())
                // Only allow the uris to conflict if every conflicting segment is allowable.
                .allMatch(conflict -> {
                    Pattern p;
                    String staticSegment;

                    if (conflict.getKey().isLabel()) {
                        p = labelPatterns.get(conflict.getKey().getContent());
                        staticSegment = conflict.getValue().getContent();
                    } else {
                        p = otherLabelPatterns.get(conflict.getValue().getContent());
                        staticSegment = conflict.getKey().getContent();
                    }

                    if (p == null) {
                        return false;
                    }

                    // If the pattern on the label segment does not match the static segment, then this segment's
                    // conflict is allowable.
                    return !p.matcher(staticSegment).find();
                });
    }

    private Map<String, Pattern> getHttpLabelPatterns(Model model, OperationShape operation) {
        return HttpBindingIndex.of(model)
                .getRequestBindings(operation)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().getLocation().equals(HttpBinding.Location.LABEL))
                .flatMap(entry -> OptionalUtils.stream(entry.getValue()
                        .getMember()
                        .getMemberTrait(model, PatternTrait.class)
                        .map(pattern -> Pair.of(entry.getKey(), pattern.getPattern()))))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Map<String, Pattern> getHostLabelPatterns(Model model, OperationShape operation) {
        Map<String, Pattern> result = new HashMap<>();
        for (MemberShape member : OperationIndex.of(model).expectInputShape(operation).getAllMembers().values()) {
            if (member.hasTrait(HostLabelTrait.class) && member.hasTrait(PatternTrait.class)) {
                result.put(member.getMemberName(), member.expectTrait(PatternTrait.class).getPattern());
            }
        }
        return result;
    }

    private String formatConflicts(UriPattern pattern, List<Pair<ShapeId, UriPattern>> conflicts) {
        String conflictString = conflicts.stream()
                .map(conflict -> String.format("`%s` (%s)", conflict.getLeft(), conflict.getRight()))
                .sorted()
                .collect(Collectors.joining(", "));

        return String.format(
                "Operation URI, `%s`, conflicts with other operation URIs in the same service: [%s]",
                pattern,
                conflictString);
    }
}
