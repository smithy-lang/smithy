/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.diff.evaluators.configurable;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.diff.DiffEvaluator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Loads configurable diff evaluators defined in the model's metadata.
 */
public final class ConfigurableEvaluatorLoader {
    private static final String DIFF_EVALUATORS_KEY = "diffEvaluators";

    private ConfigurableEvaluatorLoader() {}

    /**
     * Loads configurable diff evaluators defined in a model's metadata.
     *
     * <p>This returns a ValidatedResult containing validation events that
     * occurred when deserializing found diff evaluators.
     *
     * @param model Model to load diff evaluators from.
     * @return The result of loading the diff evaluators, including any validation errors that occurred.
     */
    public static ValidatedResult<List<DiffEvaluator>> loadMetadataDiffEvaluators(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        List<DiffEvaluator> evaluatorDefinitions = new ArrayList<>();

        model.getMetadataProperty(DIFF_EVALUATORS_KEY).ifPresent(node -> {
            try {
                ArrayNode arrayNode = node.expectArrayNode(
                        String.format("metadata property `%s` must be an array of objects.", DIFF_EVALUATORS_KEY));
                for (Node element : arrayNode.getElements()) {
                    try {
                        ConfigurableEvaluatorDefinition definition = ConfigurableEvaluatorDefinition.fromNode(element);
                        evaluatorDefinitions.add(new ConfigurableEvaluator(definition));
                    } catch (SourceException e) {
                        events.add(ValidationEvent.fromSourceException(e));
                    }
                }
            } catch (SourceException e) {
                events.add(ValidationEvent.fromSourceException(e));
            }
        });

        return new ValidatedResult<>(evaluatorDefinitions, events);
    }
}
