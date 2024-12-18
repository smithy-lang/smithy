/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

        Set<Shape> shapesToReplace = updatedServices.values()
                .stream()
                .map(ServiceShape.Builder::build)
                .collect(Collectors.toSet());
        return transformer.replaceShapes(model, shapesToReplace);
    }
}
