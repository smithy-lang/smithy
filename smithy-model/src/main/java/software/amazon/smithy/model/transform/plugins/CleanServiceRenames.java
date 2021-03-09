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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes "rename" entries from service shapes when a shape is removed.
 */
public final class CleanServiceRenames implements ModelTransformerPlugin {

    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Map<ShapeId, ServiceShape.Builder> updatedServices = new HashMap<>();

        model.shapes(ServiceShape.class).forEach(service -> {
            for (Shape shape : shapes) {
                if (service.getRename().containsKey(shape.getId())) {
                    updatedServices
                            .computeIfAbsent(service.getId(), id -> service.toBuilder())
                            .removeRename(shape);
                }
            }
        });

        if (updatedServices.isEmpty()) {
            return model;
        }

        Set<Shape> shapesToReplace = updatedServices.values().stream()
                .map(ServiceShape.Builder::build)
                .collect(Collectors.toSet());
        return transformer.replaceShapes(model, shapesToReplace);
    }
}
