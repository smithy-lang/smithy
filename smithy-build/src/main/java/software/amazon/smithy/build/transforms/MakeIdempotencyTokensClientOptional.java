/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;

/**
 * {@code makeIdempotencyTokensClientOptional} makes {@code @idempotencyToken} fields {@code @clientOptional}.
 */
public final class MakeIdempotencyTokensClientOptional implements ProjectionTransformer {

    @Override
    public String getName() {
        return "makeIdempotencyTokensClientOptional";
    }

    @Override
    public Model transform(TransformContext context) {
        Model model = context.getModel();
        return context.getTransformer().makeIdempotencyTokensClientOptional(model);
    }
}
