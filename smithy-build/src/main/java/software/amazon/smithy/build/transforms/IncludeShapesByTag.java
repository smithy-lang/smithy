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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Removes shapes that are not tagged with at least one of the given
 * trait arguments.
 */
public final class IncludeShapesByTag implements ProjectionTransformer {
    @Override
    public String getName() {
        return "includeShapesByTag";
    }

    @Override
    public Collection<String> getAliases() {
        return Collections.singleton("includeByTag");
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Set<String> includeTags = new HashSet<>(arguments);
        return (transformer, model) -> transformer.filterShapes(
                model, shape -> shape.getTags().stream().anyMatch(includeTags::contains));
    }
}
