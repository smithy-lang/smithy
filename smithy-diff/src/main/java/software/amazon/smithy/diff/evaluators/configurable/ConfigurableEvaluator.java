/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.diff.evaluators.configurable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.diff.evaluators.AbstractDiffEvaluator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Diff evaluator that is configurable via the `diffEvaluators` metadata property in
 * Smithy models.
 */
public final class ConfigurableEvaluator extends AbstractDiffEvaluator {
    private final ConfigurableEvaluatorDefinition definition;

    ConfigurableEvaluator(ConfigurableEvaluatorDefinition definition) {
        this.definition = definition;
    }

    @Override
    public String getEventId() {
        return definition.getId();
    }

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        Model model = getApplicableModel(differences);
        Set<Shape> shapes = getApplicableShapes(differences);
        definition.getFilter().ifPresent(filter -> shapes.retainAll(filter.select(model)));
        Set<Shape> matches = definition.getSelector().shapes(model)
                .filter(shapes::contains)
                .collect(Collectors.toSet());
        return mapToEvents(shapes, matches);
    }

    private Model getApplicableModel(Differences differences) {
        return definition.getAppliesTo() == AppliesTo.ADDED_SHAPES
                ? differences.getNewModel()
                : differences.getOldModel();
    }

    private Set<Shape> getApplicableShapes(Differences differences) {
        return definition.getAppliesTo() == AppliesTo.ADDED_SHAPES
                ? differences.addedShapes().collect(Collectors.toSet())
                : differences.removedShapes().collect(Collectors.toSet());
    }

    private List<ValidationEvent> mapToEvents(Set<Shape> applicableShapes, Set<Shape> matches) {
        switch (definition.getEmitCondition()) {
            case IF_ANY_MATCH:
                if (!matches.isEmpty()) {
                    return Collections.singletonList(getBaseEventBuilder().build());
                }
                break;
            case IF_ALL_MATCH:
                if (matches.equals(applicableShapes)) {
                    return Collections.singletonList(getBaseEventBuilder().build());
                }
                break;
            case IF_NONE_MATCH:
                if (matches.stream().anyMatch(applicableShapes::contains)) {
                    return Collections.singletonList(getBaseEventBuilder().build());
                }
                break;
            case FOR_EACH_MATCH:
            default:
                return matches.stream()
                        .map(shape -> getBaseEventBuilder().shape(shape).build())
                        .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private ValidationEvent.Builder getBaseEventBuilder() {
        return ValidationEvent.builder()
                .id(definition.getId())
                .message(definition.getMessage())
                .severity(definition.getSeverity());
    }
}
