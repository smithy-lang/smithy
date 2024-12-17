/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import java.util.List;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Interface used to evaluate two models and their normalized
 * differences and return {@link ValidationEvent}s that are relative to
 * the new model.
 *
 * <p>For example, a ValidationEvent is emitted when an operation is
 * removed from a service or resource.
 *
 * <p>Implementations of this interface are found using Java SPI and
 * automatically applied to the detected differences when creating a
 * {@link ModelDiff}.
 */
@FunctionalInterface
public interface DiffEvaluator {
    /**
     * Returns validation events given two models and the detected
     * differences between them.
     *
     * @param differences Detected differences.
     * @return Returns validation events that are relative to the new model.
     */
    List<ValidationEvent> evaluate(Differences differences);
}
