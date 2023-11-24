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

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the length trait on map shapes or members that target them.
 */
@SmithyInternalApi
final class MapLengthPlugin extends MemberAndShapeTraitPlugin<MapShape, ObjectNode, LengthTrait> {

    MapLengthPlugin() {
        super(MapShape.class, ObjectNode.class, LengthTrait.class);
    }

    @Override
    protected void check(Shape shape, LengthTrait trait, ObjectNode node, Context context, Emitter emitter) {
        trait.getMin().ifPresent(min -> {
            if (node.size() < min) {
                emitter.accept(node, getSeverity(context), String.format(
                        "Value provided for `%s` must have at least %d entries, but the provided value only "
                        + "has %d entries", shape.getId(), min, node.size()));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.size() > max) {
                emitter.accept(node, getSeverity(context), String.format(
                        "Value provided for `%s` must have no more than %d entries, but the provided value "
                        + "has %d entries", shape.getId(), max, node.size()));
            }
        });
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING : Severity.ERROR;
    }
}
