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

    private final List<AttributeValue> expected;
    private final AttributeComparator comparator;
    private final boolean caseInsensitive;
    private final Function<Model, Collection<? extends Shape>> optimizer;
    private final AttributePathExtractor extractor;

    private AttributeSelector(
            List<AttributeValue> expected,
            AttributeComparator comparator,
            boolean caseInsensitive,
            Function<Model, Collection<? extends Shape>> optimizer,
            AttributePathExtractor extractor
    ) {
        this.expected = expected;
        this.comparator = comparator;
        this.caseInsensitive = caseInsensitive;
        this.optimizer = optimizer;
        this.extractor = extractor;
    }

    /**
     * Single factory for creating the best selector for an attribute expression.
     *
     * <p>This consolidates all the "is this a trait path?" logic into one place. For the common
     * {@code [trait|X]} existence check (no comparator, path depth 2), it returns a
     * {@link TraitExistenceSelector} that uses a direct {@code hasTrait} call. For comparator cases
     * targeting a trait, it precompiles the ShapeId and uses a starting-shape optimizer. Everything
     * else falls back to the generic path-walking machinery.
     *
     * @param path The parsed attribute path segments.
     * @param values The expected comparison values (null for existence checks).
     * @param comparator The comparator (null for existence checks).
     * @param caseInsensitive Whether the comparison is case-insensitive.
     * @return The best InternalSelector for this attribute expression.
     */
    static InternalSelector create(
            List<String> path,
            List<String> values,
            AttributeComparator comparator,
            boolean caseInsensitive
    ) {
        boolean isTraitPath = path.size() >= 2 && path.get(0).equals("trait") && !path.get(1).startsWith("(");

        // Plain [trait|X] existence check has specialized node with direct hasTrait.
        if (comparator == null && isTraitPath && path.size() == 2) {
            return new TraitExistenceSelector(ShapeId.from(Trait.makeAbsoluteName(path.get(1))));
        }

        // Build the expected AttributeValues for comparator cases.
        List<AttributeValue> expectedValues;
        if (values == null) {
            expectedValues = Collections.emptyList();
        } else {
            expectedValues = new ArrayList<>(values.size());
            for (String v : values) {
                expectedValues.add(AttributeValue.literal(v));
            }
        }

        // Determine starting-shape optimizer.
        Function<Model, Collection<? extends Shape>> optimizer;
        if (comparator == null && isTraitPath) {
            ShapeId trait = ShapeId.from(Trait.makeAbsoluteName(path.get(1)));
            optimizer = model -> model.getShapesWithTrait(trait);
        } else {
            optimizer = Model::toSet;
        }

        // Precompile path extraction.
        AttributePathExtractor extractor = AttributePathExtractor.compile(path);
        return new AttributeSelector(expectedValues, comparator, caseInsensitive, optimizer, extractor);
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

    @Override
    public boolean isOutputSubsetOfInput() {
        return true;
    }

    private boolean matchesAttribute(Shape shape, Context stack) {
        AttributeValue lhs = extractor.extract(shape, stack.getVars());

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
