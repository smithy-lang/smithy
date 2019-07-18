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

package software.amazon.smithy.build.transforms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Filters out shapes and trait definitions that are not part of one of the
 * given namespaces.
 *
 * <p>Note that this does not filter out prelude shapes or namespaces.
 */
public final class IncludeNamespaces implements ProjectionTransformer {
    @Override
    public String getName() {
        return "includeNamespaces";
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Set<String> includeNamespaces = new HashSet<>(arguments);
        return (transformer, model) -> transformer.filterShapes(
                model, shape -> Prelude.isPreludeShape(shape)
                                || includeNamespaces.contains(shape.getId().getNamespace()));
    }
}
