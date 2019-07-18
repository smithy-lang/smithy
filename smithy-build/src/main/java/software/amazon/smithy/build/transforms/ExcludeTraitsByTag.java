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
import java.util.List;
import java.util.function.BiFunction;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.Tagged;

/**
 * Removes traits and trait definitions from a model if the trait definition
 * contains any of the provided tags.
 *
 * <p>This transformer will not remove prelude trait definitions.
 */
public final class ExcludeTraitsByTag implements ProjectionTransformer {
    @Override
    public String getName() {
        return "excludeTraitsByTag";
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        return (transformer, model) -> transformer.removeShapesIf(
                model,
                shape -> !Prelude.isPreludeShape(shape)
                         && shape.hasTrait(TraitDefinition.class)
                         && hasAnyTag(shape, arguments));
    }

    private boolean hasAnyTag(Tagged tagged, Collection<String> tags) {
        return tagged.getTags().stream().anyMatch(tags::contains);
    }
}
