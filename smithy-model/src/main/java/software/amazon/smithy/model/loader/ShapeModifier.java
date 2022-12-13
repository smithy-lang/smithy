/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Represents a modification that needs to be done to resolve a pending shape.
 */
interface ShapeModifier {
    /**
     * Modifies locally defined members on the shape.
     *
     * <p>This method is called once for each locally defined member on the shape.
     *
     * @param shapeBuilder The builder for the shape being modified.
     * @param memberBuilder The builder for the locally defined member.
     * @param unclaimedTraits Function that provides unclaimed traits for a shape.
     * @param shapeMap A function that returns a shape for a given ID, or null.
     */
    default void modifyMember(
            AbstractShapeBuilder<?, ?> shapeBuilder,
            MemberShape.Builder memberBuilder,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits,
            Function<ShapeId, Shape> shapeMap
    ) {
    }

    /**
     * Modify the pending shape after its local members have been built.
     *
     * <p>This method is called once after the shape's locally defined members have been built.
     *
     * @param builder The builder for the shape being modified.
     * @param memberBuilders The builders for the shape's locally defined members.
     * @param unclaimedTraits Function that provides unclaimed traits for a shape.
     * @param shapeMap A function that returns a shape for a given ID, or null.
     */
    default void modifyShape(
            AbstractShapeBuilder<?, ?> builder,
            Map<String, MemberShape.Builder> memberBuilders,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits,
            Function<ShapeId, Shape> shapeMap
    ) {
    }

    /**
     * @return Returns any events emitted by the modifier.
     */
    List<ValidationEvent> getEvents();
}
