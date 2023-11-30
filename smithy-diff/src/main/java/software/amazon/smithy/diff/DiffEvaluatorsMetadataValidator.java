/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.diff;

import java.util.List;
import software.amazon.smithy.diff.evaluators.configurable.ConfigurableEvaluatorLoader;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates diff evaluators configured in {@code diffEvaluators} metadata
 * property.
 */
public class DiffEvaluatorsMetadataValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return ConfigurableEvaluatorLoader.loadMetadataDiffEvaluators(model).getValidationEvents();
    }
}
