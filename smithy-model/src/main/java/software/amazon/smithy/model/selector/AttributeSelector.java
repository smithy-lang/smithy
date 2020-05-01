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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Matches shapes with a specific attribute or that matches an attribute comparator.
 */
final class AttributeSelector implements InternalSelector {

    private final BiFunction<Shape, Map<String, Set<Shape>>, AttributeValue> key;
    private final List<AttributeValue> expected;
    private final AttributeComparator comparator;
    private final boolean caseInsensitive;

    AttributeSelector(
            BiFunction<Shape, Map<String, Set<Shape>>, AttributeValue> key,
            List<String> expected,
            AttributeComparator comparator,
            boolean caseInsensitive
    ) {
        this.key = key;
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
    }

    static AttributeSelector existence(BiFunction<Shape, Map<String, Set<Shape>>, AttributeValue> key) {
        return new AttributeSelector(key, null, null, false);
    }

    @Override
    public boolean push(Context context, Shape shape, Receiver next) {
        if (matchesAttribute(shape, context)) {
            return next.apply(context, shape);
        } else {
            return true;
        }
    }

    private boolean matchesAttribute(Shape shape, Context stack) {
        AttributeValue lhs = key.apply(shape, stack.getVars());

        if (expected.isEmpty()) {
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
