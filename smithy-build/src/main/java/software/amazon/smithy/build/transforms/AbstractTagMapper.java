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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;

// TODO: remove tags from authentication and protocols.
abstract class AbstractTagMapper implements ProjectionTransformer {
    private final boolean exclude;

    AbstractTagMapper(boolean exclude) {
        this.exclude = exclude;
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Set<String> tags = new HashSet<>(arguments);
        return (transformer, model) -> removeTraitDefTags(removeShapeTags(transformer, model, tags), tags);
    }

    private Model removeShapeTags(ModelTransformer transformer, Model model, Set<String> tags) {
        return transformer.mapShapes(model, shape -> intersectIfChanged(shape.getTags(), tags)
                .map(intersection -> {
                    TagsTrait.Builder builder = TagsTrait.builder();
                    intersection.forEach(builder::addValue);
                    return Shape.shapeToBuilder(shape).addTrait(builder.build()).build();
                })
                .orElse(shape));
    }

    private Model removeTraitDefTags(Model model, Set<String> tags) {
        Set<TraitDefinition> definitions = model.getTraitDefinitions().stream()
                .map(definition -> intersectIfChanged(definition.getTags(), tags)
                        .map(intersection -> {
                            TraitDefinition.Builder builder = definition.toBuilder();
                            builder.clearTags();
                            intersection.forEach(builder::addTag);
                            return builder.build();
                        })
                        .orElse(definition))
                .collect(Collectors.toSet());

        if (definitions.equals(model.getTraitDefinitions())) {
            return model;
        }

        Model.Builder builder = model.toBuilder();
        builder.clearTraitDefinitions();
        definitions.forEach(builder::addTraitDefinition);
        return builder.build();
    }

    private Optional<Set<String>> intersectIfChanged(Collection<String> subject, Collection<String> other) {
        Set<String> temp = new HashSet<>(subject);
        if (exclude) {
            temp.removeAll(other);
        } else {
            temp.retainAll(other);
        }
        return temp.size() == subject.size() ? Optional.empty() : Optional.of(temp);
    }
}
