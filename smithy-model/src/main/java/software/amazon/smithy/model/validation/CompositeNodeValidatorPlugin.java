/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;
import software.amazon.smithy.model.validation.node.NodeValidatorPlugin;

/**
 * A composite {@link NodeValidatorPlugin} that efficiently delegates to child plugins
 * based on the shape types they apply to.
 */
final class CompositeNodeValidatorPlugin implements NodeValidatorPlugin {

    private static final ShapeTypeFilter SHAPE_TYPE_FILTER = new ShapeTypeFilter(EnumSet.allOf(ShapeType.class));
    private final EnumMap<ShapeType, List<NodeValidatorPlugin>> pluginsForDirectShapeTypes =
            new EnumMap<>(ShapeType.class);
    private final EnumMap<ShapeType, List<NodeValidatorPlugin>> pluginsForTargetShapeTypes =
            new EnumMap<>(ShapeType.class);

    CompositeNodeValidatorPlugin() {
        for (ShapeType shapeType : ShapeType.values()) {
            pluginsForDirectShapeTypes.put(shapeType, new ArrayList<>());
            pluginsForTargetShapeTypes.put(shapeType, new ArrayList<>());
        }
    }

    @Override
    public ShapeTypeFilter shapeTypeFilter() {
        return SHAPE_TYPE_FILTER;
    }

    /**
     * Adds a child {@link NodeValidatorPlugin},
     * indexing it by the shape types it applies to according to its shape matcher predicate.
     */
    void addChild(NodeValidatorPlugin plugin) {
        ShapeTypeFilter filter = plugin.shapeTypeFilter();

        for (ShapeType shapeType : filter.directShapeTypes()) {
            pluginsForDirectShapeTypes.get(shapeType).add(plugin);
        }
        for (ShapeType shapeType : filter.targetShapeTypes()) {
            pluginsForTargetShapeTypes.get(shapeType).add(plugin);
        }
    }

    @Override
    public void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
        ShapeType shapeType = shape.getType();

        if (shapeType.equals(ShapeType.MEMBER)) {
            MemberShape memberShape = (MemberShape) shape;
            Optional<Shape> target = context.model().getShape(memberShape.getTarget());
            if (target.isPresent()) {
                for (NodeValidatorPlugin plugin : pluginsForTargetShapeTypes.get(target.get().getType())) {
                    plugin.applyMatching(shape, value, context, emitter);
                }
            }
        } else {
            for (NodeValidatorPlugin plugin : pluginsForDirectShapeTypes.get(shapeType)) {
                plugin.applyMatching(shape, value, context, emitter);
            }
        }
    }
}
