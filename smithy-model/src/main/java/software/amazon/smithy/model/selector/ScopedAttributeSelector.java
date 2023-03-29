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

import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Matches a scoped attribute or projection against a set of assertions that
 * can path into the scoped attribute.
 */
final class ScopedAttributeSelector implements InternalSelector {

    static final class Assertion {
        private final ScopedFactory lhs;
        private final AttributeComparator comparator;
        private final List<ScopedFactory> rhs;
        private final boolean caseInsensitive;

        Assertion(
                ScopedFactory lhs,
                AttributeComparator comparator,
                List<ScopedFactory> rhs,
                boolean caseInsensitive
        ) {
            this.lhs = lhs;
            this.comparator = comparator;
            this.rhs = rhs;
            this.caseInsensitive = caseInsensitive;
        }
    }

    /**
     * Creates an AttributeValue from the given scope value.
     *
     * <p>This is useful for pathing into scopes.
     */
    @FunctionalInterface
    interface ScopedFactory {
        AttributeValue create(AttributeValue value);
    }

    private final List<String> path;
    private final List<Assertion> assertions;

    ScopedAttributeSelector(List<String> path, List<Assertion> assertions) {
        this.path = path;
        this.assertions = assertions;
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        if (matchesAssertions(shape, context.getVars())) {
            return next.apply(context, shape);
        } else {
            return Response.CONTINUE;
        }
    }

    private boolean matchesAssertions(Shape shape, Map<String, Set<Shape>> vars) {
        // First resolve the scope of the assertions.
        AttributeValue scope = AttributeValue.shape(shape, vars).getPath(path);

        // If it's not present, then nothing could ever match.
        if (!scope.isPresent()) {
            return false;
        }

        // When dealing with a projection, each flattened projection value is
        // used as a scope and then passed to the assertions one at a time.
        for (AttributeValue value : scope.getFlattenedValues()) {
            if (compareWithScope(value)) {
                return true;
            }
        }

        return false;
    }

    private boolean compareWithScope(AttributeValue scope) {
        // Ensure that each assertion matches, and provide them the scope.
        for (Assertion assertion : assertions) {
            AttributeValue lhs = assertion.lhs.create(scope);
            boolean matchedOneRhs = false;
            for (ScopedFactory factory : assertion.rhs) {
                AttributeValue rhs = factory.create(scope);
                if (assertion.comparator.compare(lhs, rhs, assertion.caseInsensitive)) {
                    matchedOneRhs = true;
                    break;
                }
            }

            if (!matchedOneRhs) {
                return false;
            }
        }

        return true;
    }
}
