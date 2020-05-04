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

import java.util.function.BiConsumer;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that the value contained in a string shape is a valid shape ID
 * and that the shape ID targets a shape that is in the set of shapes
 * matching the selector.
 */
@SmithyInternalApi
final class IdRefPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, IdRefTrait> {

    IdRefPlugin() {
        super(StringShape.class, StringNode.class, IdRefTrait.class);
    }

    @Override
    protected void check(
            Shape shape,
            IdRefTrait trait,
            StringNode node,
            Model model,
            BiConsumer<FromSourceLocation, String> emitter
    ) {
        try {
            ShapeId target = node.expectShapeId();
            Shape resolved = model.getShape(target).orElse(null);

            if (resolved == null) {
                if (trait.failWhenMissing()) {
                    failWhenNoMatch(node, trait, emitter, String.format(
                            "Shape ID `%s` was not found in the model", target));
                }
            } else if (!matchesSelector(trait, resolved.getId(), model)) {
                failWhenNoMatch(node, trait, emitter, String.format(
                        "Shape ID `%s` does not match selector `%s`",
                        resolved.getId(), trait.getSelector()));
            }
        } catch (SourceException e) {
            emitter.accept(node, e.getMessageWithoutLocation());
        }
    }

    private boolean matchesSelector(IdRefTrait trait, ShapeId needle, Model haystack) {
        return trait.getSelector().select(haystack).stream()
                .map(Shape::getId)
                .anyMatch(shapeId -> shapeId.equals(needle));
    }

    private void failWhenNoMatch(
            FromSourceLocation location,
            IdRefTrait trait,
            BiConsumer<FromSourceLocation, String> emitter,
            String message
    ) {
        emitter.accept(location, trait.getErrorMessage().orElse(message));
    }
}
