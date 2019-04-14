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

package software.amazon.smithy.model.validation.node;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.LengthTrait;

/**
 * Validates the length trait on map shapes or members that target them.
 */
public final class MapLengthPlugin extends MemberAndShapeTraitPlugin<MapShape, ObjectNode, LengthTrait> {
    public MapLengthPlugin() {
        super(MapShape.class, ObjectNode.class, LengthTrait.class);
    }

    @Override
    protected List<String> check(Shape shape, LengthTrait trait, ObjectNode node, ShapeIndex index) {
        List<String> messages = new ArrayList<>();
        trait.getMin().ifPresent(min -> {
            if (node.size() < min) {
                messages.add(String.format(
                        "Value provided for `%s` must have at least %d entries, but the provided value only "
                        + "has %d entries", shape.getId(), min, node.size()));
            }
        });
        trait.getMax().ifPresent(max -> {
            if (node.size() > max) {
                messages.add(String.format(
                        "Value provided for `%s` must have no more than %d entries, but the provided value "
                        + "has %d entries", shape.getId(), max, node.size()));
            }
        });
        return messages;
    }
}
