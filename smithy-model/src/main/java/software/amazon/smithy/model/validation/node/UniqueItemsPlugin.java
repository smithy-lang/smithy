/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

/**
 * Validates that items in lists with the `@uniqueItems` trait are unique.
 */
public final class UniqueItemsPlugin extends MemberAndShapeTraitPlugin<ArrayNode, UniqueItemsTrait> {

    private static final ShapeTypeFilter SHAPE_TYPE_FILTER = new ShapeTypeFilter(EnumSet.of(ShapeType.LIST));

    public UniqueItemsPlugin() {
        super(ArrayNode.class, UniqueItemsTrait.class);
    }

    @Override
    public ShapeTypeFilter shapeTypeFilter() {
        return SHAPE_TYPE_FILTER;
    }

    @Override
    protected void check(Shape shape, UniqueItemsTrait trait, ArrayNode value, Context context, Emitter emitter) {
        // Find duplicates in a single pass so the check stays linear in the list size.
        // The duplicates are recorded in the order of their second occurrence.
        Set<Node> seen = new HashSet<>(value.size());
        Set<String> duplicateValues = new LinkedHashSet<>();
        for (Node element : value.getElements()) {
            if (!seen.add(element)) {
                duplicateValues.add(element.toString());
            }
        }

        if (!duplicateValues.isEmpty()) {
            emitter.accept(value,
                    String.format(
                            "Value provided for `%s` must have unique items, but the following items had multiple entries: "
                                    + "[`%s`]",
                            shape.getId(),
                            String.join("`, `", duplicateValues)));
        }
    }
}
