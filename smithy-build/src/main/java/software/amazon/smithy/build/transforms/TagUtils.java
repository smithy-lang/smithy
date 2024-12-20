/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Utilities for {@link ExcludeTags} and {@link IncludeTags}.
 */
final class TagUtils {

    private TagUtils() {}

    static Model excludeShapeTags(ModelTransformer transformer, Model model, Set<String> tags) {
        return includeExcludeShapeTags(transformer, model, tags, true);
    }

    static Model includeShapeTags(ModelTransformer transformer, Model model, Set<String> tags) {
        return includeExcludeShapeTags(transformer, model, tags, false);
    }

    private static Model includeExcludeShapeTags(
            ModelTransformer transformer,
            Model model,
            Set<String> tags,
            boolean exclude
    ) {
        return transformer.mapShapes(model,
                shape -> intersectIfChanged(shape.getTags(), tags, exclude)
                        .map(intersection -> {
                            TagsTrait.Builder builder = TagsTrait.builder();
                            intersection.forEach(builder::addValue);
                            return Shape.shapeToBuilder(shape).addTrait(builder.build()).build();
                        })
                        .orElse(shape));
    }

    private static Optional<Set<String>> intersectIfChanged(
            Collection<String> subject,
            Collection<String> other,
            boolean exclude
    ) {
        Set<String> temp = new HashSet<>(subject);
        if (exclude) {
            temp.removeAll(other);
        } else {
            temp.retainAll(other);
        }
        return temp.size() == subject.size() ? Optional.empty() : Optional.of(temp);
    }
}
