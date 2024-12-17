/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * TODO: Implement evaluator to find removed authentication schemes.
 */
public final class RemovedAuthenticationScheme extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return Collections.emptyList();
    }
}
