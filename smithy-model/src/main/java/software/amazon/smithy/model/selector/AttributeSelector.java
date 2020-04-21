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

package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Matches shapes with a specific attribute or that matches an attribute comparator.
 */
final class AttributeSelector implements Selector {

    private final AttributeValue.Factory key;
    private final List<AttributeValue> expected;
    private final AttributeComparator comparator;
    private final boolean caseInsensitive;

    AttributeSelector(
            AttributeValue.Factory key,
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
                this.expected.add(new AttributeValue.Literal(validValue));
            }
        }
    }

    static AttributeSelector existence(AttributeValue.Factory key) {
        return new AttributeSelector(key, null, null, false);
    }

    @Override
    public Set<Shape> select(Model model, NeighborProvider neighborProvider, Set<Shape> shapes) {
        return shapes.stream()
                .filter(this::matchesAttribute)
                .collect(Collectors.toSet());
    }

    private boolean matchesAttribute(Shape shape) {
        AttributeValue lhs = key.create(shape);

        if (expected.isEmpty()) {
            return lhs.isPresent();
        }

        for (AttributeValue rhs : expected) {
            if (lhs.compare(comparator, rhs, caseInsensitive)) {
                return true;
            }
        }

        return false;
    }
}
