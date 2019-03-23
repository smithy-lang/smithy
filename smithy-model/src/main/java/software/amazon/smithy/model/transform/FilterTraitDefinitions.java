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

package software.amazon.smithy.model.transform;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Filters trait definitions out of a model that do not match a predicate,
 * and removes any instances of a removed trait definition.
 *
 * @see ModelTransformer#filterTraitDefinitions
 */
final class FilterTraitDefinitions {
    private final Predicate<TraitDefinition> predicate;

    FilterTraitDefinitions(Predicate<TraitDefinition> predicate) {
        this.predicate = predicate
                // Don't ever filter out built-in trait definitions.
                .or(def -> Prelude.isPreludeTraitDefinition(def.getFullyQualifiedName()));
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.removeTraitDefinitions(model, model.getTraitDefinitions().stream()
                .filter(Predicate.not(predicate))
                .map(TraitDefinition::getFullyQualifiedName)
                .collect(Collectors.toSet()));
    }
}
