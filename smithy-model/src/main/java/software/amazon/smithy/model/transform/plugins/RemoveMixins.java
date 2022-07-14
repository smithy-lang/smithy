/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.transform.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Removes mixins from shapes when a mixin is removed from the model.
 */
@SmithyInternalApi
public final class RemoveMixins implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Map<Shape, Set<Shape>> mixinShapesToRemove = new HashMap<>();

        for (Shape removedShape : shapes) {
            if (removedShape.hasTrait(MixinTrait.class) && !removedShape.isMemberShape()) {
                // Remove the mixin from any shape that uses it.
                Stream.concat(model.shapes(StructureShape.class), model.shapes(UnionShape.class)).forEach(shape -> {
                    if (shape.getMixins().contains(removedShape.getId())) {
                        mixinShapesToRemove.computeIfAbsent(shape, s -> new HashSet<>()).add(removedShape);
                    }
                });
            }
        }

        if (mixinShapesToRemove.isEmpty()) {
            return model;
        }

        List<Shape> toReplace = new ArrayList<>(mixinShapesToRemove.size());
        for (Map.Entry<Shape, Set<Shape>> entry : mixinShapesToRemove.entrySet()) {
            AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(entry.getKey());
            for (Shape mixin : entry.getValue()) {
                builder.removeMixin(mixin);
            }
            toReplace.add(builder.build());
        }

        // The replace transform handles ensuring that any updated mixins as a result
        // of removing mixins are reflected in inherited shapes.
        return transformer.replaceShapes(model, toReplace);
    }
}
