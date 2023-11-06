/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.validation.node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

/**
 * Validates that items in lists with the `@uniqueItems` trait are unique.
 */
final class UniqueItemsPlugin extends MemberAndShapeTraitPlugin<CollectionShape, ArrayNode, UniqueItemsTrait> {
    UniqueItemsPlugin() {
        super(CollectionShape.class, ArrayNode.class, UniqueItemsTrait.class);
    }

    @Override
    protected void check(Shape shape, UniqueItemsTrait trait, ArrayNode value, Context context, Emitter emitter) {
        Set<Node> uniqueNodes = new HashSet<>(value.getElements());
        if (uniqueNodes.size() != value.size()) {
            List<Node> duplicateNodes = new ArrayList<>(value.getElements());
            for (Node uniqueNode : uniqueNodes) {
                // Collections remove the first encountered entry, so our unique
                // nodes through a set will leave behind any duplicates.
                duplicateNodes.remove(uniqueNode);
            }

            Set<String> duplicateValues = new LinkedHashSet<>();
            for (Node duplicateNode : duplicateNodes) {
                duplicateValues.add(duplicateNode.toString());
            }
            emitter.accept(value, String.format(
                    "Value provided for `%s` must have unique items, but the following items had multiple entries: "
                    + "[`%s`]", shape.getId(), String.join("`, `", duplicateValues)));
        }
    }
}
