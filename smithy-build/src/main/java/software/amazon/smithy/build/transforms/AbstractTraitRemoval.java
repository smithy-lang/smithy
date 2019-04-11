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
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;

abstract class AbstractTraitRemoval implements ProjectionTransformer {
    Pair<Set<String>, Set<String>> parseTraits(List<String> arguments) {
        Set<String> traitNames = new HashSet<>();
        Set<String> traitNamespaces = new HashSet<>();

        for (String arg : arguments) {
            if (arg.endsWith("#")) {
                traitNamespaces.add(arg.substring(0, arg.length() - 1));
            } else if (arg.equals(Prelude.NAMESPACE)) {
                // For backwards compatibility, support "smithy.api" instead
                // of "smithy.api#".
                traitNamespaces.add(arg);
            } else {
                traitNames.add(Trait.makeAbsoluteName(arg));
            }
        }

        return Pair.of(traitNames, traitNamespaces);
    }

    boolean matchesTraitDefinition(
            TraitDefinition definition,
            Set<String> traitNames,
            Set<String> traitNamespaces) {
        return traitNames.contains(definition.getFullyQualifiedName())
               || traitNamespaces.contains(definition.getNamespace());
    }
}
