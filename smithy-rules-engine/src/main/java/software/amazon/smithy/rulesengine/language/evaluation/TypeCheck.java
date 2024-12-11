/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation;

import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * TypeCheck provides an interface for determining whether the given types within scope
 * satisfy the associated constraints.
 */
@SmithyUnstableApi
public interface TypeCheck {
    /**
     * Checks whether the given types within a scope satisfy the constraints.
     *
     * @param scope the scope to evaluate.
     * @return the type validated by the scope check.
     */
    Type typeCheck(Scope<Type> scope);
}
