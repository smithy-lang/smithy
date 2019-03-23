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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Tagged;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Removes traits and trait definitions from a model if the trait definition
 * contains any of the provided tags.
 */
public final class ExcludeTraitsByTag implements ProjectionTransformer {
    @Override
    public String getName() {
        return "excludeTraitsByTag";
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        return (transformer, model) -> {
            Set<String> definitions = model.getTraitDefinitions().stream()
                    .filter(definition -> hasAnyTag(definition, arguments))
                    .map(TraitDefinition::getFullyQualifiedName)
                    .collect(Collectors.toSet());

            return transformer.removeTraitDefinitions(model, definitions);
        };
    }

    private boolean hasAnyTag(Tagged tagged, Collection<String> tags) {
        return tagged.getTags().stream().anyMatch(tags::contains);
    }
}
