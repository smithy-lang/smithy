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

import java.util.Set;
import software.amazon.smithy.model.Model;

/**
 * Removes specific trait definitions from a model, and removes any
 * instances of a removed trait definition.
 *
 * @see ModelTransformer#removeTraitDefinitions
 */
final class RemoveTraitDefinitions {
    private final Set<String> fullyQualifiedTraitNames;

    RemoveTraitDefinitions(Set<String> fullyQualifiedTraitNames) {
        this.fullyQualifiedTraitNames = fullyQualifiedTraitNames;
    }

    Model transform(ModelTransformer transformer, Model model) {
        if (fullyQualifiedTraitNames.isEmpty()) {
            return model;
        }

        Model.Builder builder = model.toBuilder().clearTraitDefinitions();
        model.getTraitDefinitions().stream()
                .filter(def -> !fullyQualifiedTraitNames.contains(def.getFullyQualifiedName()))
                .forEach(builder::addTraitDefinition);
        // Remove any traits that are an instance of the removed trait definitions.
        return transformer.removeTraitsIf(
                builder.build(),
                (shape, trait) -> fullyQualifiedTraitNames.contains(trait.getTraitName()));
    }
}
