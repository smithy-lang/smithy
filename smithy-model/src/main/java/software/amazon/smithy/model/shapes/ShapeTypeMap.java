/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BiPredicate;
import software.amazon.smithy.model.Model;

public class ShapeTypeMap<T> {

    private final EnumMap<ShapeType, List<T>> directValues = new EnumMap<>(ShapeType.class);
    private final EnumMap<ShapeType, List<T>> targetValues = new EnumMap<>(ShapeType.class);

    public ShapeTypeMap() {
        for (ShapeType shapeType : ShapeType.values()) {
            directValues.put(shapeType, new ArrayList<>());
            targetValues.put(shapeType, new ArrayList<>());
        }
    }

    public void add(BiPredicate<Model, Shape> matcher, T value) {
        if (matcher instanceof ShapeTypeFilter) {
            ShapeTypeFilter filter = (ShapeTypeFilter) matcher;
            for (ShapeType shapeType : filter.directShapeTypes()) {
                directValues.get(shapeType).add(value);
            }
            for (ShapeType shapeType : filter.targetShapeTypes()) {
                targetValues.get(shapeType).add(value);
            }
        } else {
            for (ShapeType shapeType : ShapeType.values()) {
                directValues.get(shapeType).add(value);
            }
            for (ShapeType shapeType : ShapeType.values()) {
                targetValues.get(shapeType).add(value);
            }
        }
    }

    public List<T> get(Model model, Shape shape) {
        ShapeType shapeType = shape.getType();
        if (shapeType.equals(ShapeType.MEMBER)) {
            return shape.asMemberShape()
                    .flatMap(member -> model.getShape(member.getTarget()))
                    .map(s -> targetValues.get(s.getType()))
                    .orElse(Collections.emptyList());
        } else {
            return directValues.get(shapeType);
        }
    }
}
