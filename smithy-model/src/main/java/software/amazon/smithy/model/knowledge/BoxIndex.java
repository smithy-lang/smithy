/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.knowledge;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.BoxTrait;

/**
 * An index that checks if a shape is boxed or not.
 *
 * <p>A service, resource, and operation are never considered boxed. A member
 * is considered boxed if the member is targeted by the {@code box} trait or
 * if the shape the member targets is considered boxed. A shape is considered
 * boxed if it is targeted by the {@code box} trait or if the shape is a
 * string, blob, timestamp, bigInteger, bigDecimal, list, set, map, structure,
 * or union.
 */
public final class BoxIndex implements KnowledgeIndex {

    private final Map<ShapeId, Boolean> boxMap;

    public BoxIndex(Model model) {
        // Create a HashMap with the same initial capacity as the number of shapes in the model.
        boxMap = model.shapes().collect(Collectors.toMap(
                Shape::getId,
                s -> isBoxed(model, s),
                (a, b) -> b,
                () -> new HashMap<>(model.toSet().size())));
    }

    public static BoxIndex of(Model model) {
        return model.getKnowledge(BoxIndex.class, BoxIndex::new);
    }

    private static boolean isBoxed(Model model, Shape shape) {
        if (shape.hasTrait(BoxTrait.class)) {
            return true;
        }

        if (shape.isStringShape()
                || shape.isBlobShape()
                || shape.isTimestampShape()
                || shape.isBigDecimalShape()
                || shape.isBigIntegerShape()
                || shape instanceof CollectionShape
                || shape.isMapShape()
                || shape.isStructureShape()
                || shape.isUnionShape()) {
            return true;
        }

        // Check if the member targets a boxed shape.
        if (shape.isMemberShape()) {
            return shape.asMemberShape()
                    .map(MemberShape::getTarget)
                    .flatMap(model::getShape)
                    .filter(target -> isBoxed(model, target))
                    .isPresent();
        }

        return false;
    }

    /**
     * Checks if the given shape should be considered boxed, meaning it
     * accepts a null value.
     *
     * <p>Note that "accepts a null value" means that the type that
     * represents the shape, <em>in code</em>, accepts a null value or can
     * be optionally set, but does not necessarily mean that sending a null
     * value over the wire in a given protocol has any special meaning
     * that's materially different than if a value is completely omitted.
     *
     * @param shape Shape to check.
     * @return Returns true if the shape is effectively boxed.
     */
    public boolean isBoxed(ToShapeId shape) {
        return boxMap.get(shape.toShapeId());
    }
}
