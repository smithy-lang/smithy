/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.BiPredicate;
import software.amazon.smithy.model.Model;

/**
 * A simple shape filter based on shape types.
 */
public final class ShapeTypeFilter implements BiPredicate<Model, Shape> {

    private final EnumSet<ShapeType> directShapeTypes;
    private final EnumSet<ShapeType> targetShapeTypes;

    /**
     * Constructs a filter that matches all shapes of one of the given shape types,
     * or any MemberShape with a target shape of those types.
     */
    public ShapeTypeFilter(ShapeType first, ShapeType... others) {
        this(EnumSet.of(first, others));
    }

    /**
     * Constructs a filter that matches all shapes of one of the given shape types,
     * or any MemberShape with a target shape of those types.
     */
    public ShapeTypeFilter(EnumSet<ShapeType> shapeTypes) {
        this(shapeTypes, shapeTypes);
    }

    /**
     * Constructs a filter that matches all shapes of one of the given direct shape types,
     * or any MemberShape with a target shape that matches one of the given target shape types.
     */
    public ShapeTypeFilter(EnumSet<ShapeType> directShapeTypes, EnumSet<ShapeType> targetShapeTypes) {
        this.directShapeTypes = directShapeTypes;
        this.targetShapeTypes = targetShapeTypes;
    }

    @Override
    public boolean test(Model model, Shape shape) {
        ShapeType shapeType = shape.getType();

        if (shapeType.equals(ShapeType.MEMBER)) {
            MemberShape memberShape = (MemberShape) shape;
            Optional<Shape> target = model.getShape(memberShape.getTarget());
            return target.filter(value -> targetShapeTypes.contains(value.getType()))
                    .isPresent();
        } else {
            return directShapeTypes.contains(shapeType);
        }
    }

    /**
     * @return The set of {@link ShapeType}s that are matched directly.
     */
    public EnumSet<ShapeType> directShapeTypes() {
        return directShapeTypes;
    }

    /**
     * @return The set of {@link ShapeType}s that are matched against {@link MemberShape} targets.
     */
    public EnumSet<ShapeType> targetShapeTypes() {
        return targetShapeTypes;
    }
}
