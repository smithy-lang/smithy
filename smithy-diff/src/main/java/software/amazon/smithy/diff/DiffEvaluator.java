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
