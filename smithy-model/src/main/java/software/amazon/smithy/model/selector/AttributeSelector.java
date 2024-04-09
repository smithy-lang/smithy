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
                && path.get(0).equals("trait")     // only match on traits
                && !path.get(1).startsWith("(")) { // don't match projections
            optimizer = model -> {
                // The trait name might be relative to the prelude, so ensure it's absolute.
                String absoluteShapeId = Trait.makeAbsoluteName(path.get(1));
                ShapeId trait = ShapeId.from(absoluteShapeId);
                return model.getShapesWithTrait(trait);
            };
        } else {
            optimizer = Model::toSet;
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
