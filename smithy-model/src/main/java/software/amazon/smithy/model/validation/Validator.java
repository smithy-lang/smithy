/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;

/**
 * Validates a {@link Model} and returns a list of {@link ValidationEvent}.
 *
 * <p>A {@code Validator} is used to enforce constraints on a model. This
 * interface is used to implement both built-in validation that runs on all
 * Smithy loaded models and custom validators that can be registered when
 * loading a model. Registering a {@code Validator} class as a service
 * provider for the {@code Validator} interface will cause the validator
 * to be applied each time a Model is built using a {@link ModelAssembler}.
 *
 * <p>The {@link ValidatorService} class is used to provide a service provider
 * for validators that can be configured in the Smithy model via the
 * {@code validators[]} metadata.
 *
 * @see ValidationEvent
 */
@FunctionalInterface
public interface Validator {
    /** Event ID used for structural errors encountered when loading a model. */
    String MODEL_ERROR = "Model";

    /** Event ID used when something in the model is deprecated. */
    String MODEL_DEPRECATION = "ModelDeprecation";

    /**
     * Validates a model and returns a list of validation events.
     *
     * @param model Model to validate.
     * @return List of validation events.
     */
    List<ValidationEvent> validate(Model model);
}
