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

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that the value contained in a string shape is a valid shape ID
 * and that the shape ID targets a shape that is in the set of shapes
 * matching the selector.
 *
 * <p>Relative shape IDs may resolve to public prelude shapes if they don't
 * resolve to shapes defined in the same namespace as the ID.
 */
public final class IdRefPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, IdRefTrait> {

    private static final Logger LOGGER = Logger.getLogger(IdRefPlugin.class.getName());

    public IdRefPlugin() {
        super(StringShape.class, StringNode.class, IdRefTrait.class);
    }

    @Override
    protected List<String> check(Shape shape, IdRefTrait trait, StringNode node, Model model) {
        try {
            String target = node.getValue();

            if (!target.contains("#")) {
                LOGGER.warning("idRef traits should use absolute shape IDs. A future version of Smithy will remove "
                               + "support for relative shape IDs.");
            }

            Shape resolved = Prelude.resolveShapeId(model, shape.getId().getNamespace(), target).orElse(null);
            if (resolved == null) {
                return trait.failWhenMissing()
                       ? failWhenNoMatch(trait, String.format("Shape ID `%s` was not found in the model", target))
                       : ListUtils.of();
            } else if (matchesSelector(trait, resolved.getId(), model)) {
                return ListUtils.of();
            }

            return failWhenNoMatch(trait, String.format(
                    "Shape ID `%s` does not match selector `%s`", resolved.getId(), trait.getSelector()));
        } catch (ShapeIdSyntaxException e) {
            return ListUtils.of(e.getMessage());
        }
    }

    private boolean matchesSelector(IdRefTrait trait, ShapeId needle, Model haystack) {
        return trait.getSelector().select(haystack).stream()
                .map(Shape::getId)
                .anyMatch(shapeId -> shapeId.equals(needle));
    }

    private List<String> failWhenNoMatch(IdRefTrait trait, String message) {
        return ListUtils.of(trait.getErrorMessage().orElse(message));
    }
}
