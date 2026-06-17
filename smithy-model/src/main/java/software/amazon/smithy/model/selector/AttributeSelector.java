/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Matches shapes with a specific attribute or that matches an attribute comparator.
 */
final class AttributeSelector implements InternalSelector {

    private final List<String> path;
    private final List<AttributeValue> expected;
    private final AttributeComparator comparator;
    private final boolean caseInsensitive;
    private final Function<Model, Collection<? extends Shape>> optimizer;

    // When this selector is a plain "[trait|<name>]" existence check, the trait's ShapeId is resolved once here at
    // parse time. The hot path can then test the shape directly instead of rebuilding the absolute name and looking
    // the ShapeId up in a cache on every shape. Null when the fast path does not apply.
    private final ShapeId traitExistenceId;

    AttributeSelector(
            List<String> path,
            List<String> expected,
            AttributeComparator comparator,
            boolean caseInsensitive
    ) {
        this.path = path;
        this.caseInsensitive = caseInsensitive;
        this.comparator = comparator;

        // Create the valid values of the expected selector.
        if (expected == null) {
            this.expected = Collections.emptyList();
        } else {
            this.expected = new ArrayList<>(expected.size());
            for (String validValue : expected) {
                this.expected.add(AttributeValue.literal(validValue));
            }
        }

        // Optimization for loading shapes with a specific trait.
        // This optimization can only be applied when there's no comparator,
        // and it doesn't matter how deep into the trait the selector descends.
        if (comparator == null
                && path.size() >= 2
                && path.get(0).equals("trait") // only match on traits
                && !path.get(1).startsWith("(")) { // don't match projections
            // The trait name might be relative to the prelude, so ensure it's absolute.
            ShapeId trait = ShapeId.from(Trait.makeAbsoluteName(path.get(1)));
            optimizer = model -> model.getShapesWithTrait(trait);
            // The per-shape existence fast path only applies when the path stops at the trait itself
            // (e.g., "[trait|http]"). Deeper paths (e.g., "[trait|range|min]") still need to walk into
            // the trait's node value.
            this.traitExistenceId = path.size() == 2 ? trait : null;
        } else {
            optimizer = Model::toSet;
            this.traitExistenceId = null;
        }
    }

    static AttributeSelector existence(List<String> path) {
        return new AttributeSelector(path, null, null, false);
    }

    @Override
    public Collection<? extends Shape> getStartingShapes(Model model) {
        return optimizer.apply(model);
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        if (matchesAttribute(shape, context)) {
            return next.apply(context, shape);
        } else {
            return Response.CONTINUE;
        }
    }

    private boolean matchesAttribute(Shape shape, Context stack) {
        // Fast path for plain "[trait|<name>]" existence checks: avoids rebuilding the trait ShapeId and
        // allocating the AttributeValue path chain on every shape. This mirrors the semantics of resolving the
        // path to a trait NodeValue, which is considered present only when the trait's node is not a null node.
        if (traitExistenceId != null) {
            Trait trait = shape.findTrait(traitExistenceId).orElse(null);
            return trait != null && !trait.toNode().isNullNode();
        }

        AttributeValue lhs = AttributeValue.shape(shape, stack.getVars()).getPath(path);

        if (comparator == null) {
            return lhs.isPresent();
        }

        for (AttributeValue rhs : expected) {
            if (comparator.compare(lhs, rhs, caseInsensitive)) {
                return true;
            }
        }

        return false;
    }
}
