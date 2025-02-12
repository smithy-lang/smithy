/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;

/**
 * {@code flattenAndRemoveMixins} flattens mixins out of the model.
 */
public final class FlattenAndRemoveMixins implements ProjectionTransformer {

    @Override
    public String getName() {
        return "flattenAndRemoveMixins";
    }

    @Override
    public Model transform(TransformContext context) {
        Model model = context.getModel();
        return context.getTransformer().flattenAndRemoveMixins(model);
    }
}
