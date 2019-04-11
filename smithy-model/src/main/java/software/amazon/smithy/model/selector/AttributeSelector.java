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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.ListUtils;

/**
 * Matches shapes with a specific attribute.
 */
final class AttributeSelector implements Selector {
    static final Comparator EQUALS = String::equals;
    static final Comparator STARTS_WITH = String::startsWith;
    static final Comparator ENDS_WITH = String::endsWith;
    static final Comparator CONTAINS = String::contains;
    static final KeyGetter KEY_ID = (shape) -> ListUtils.of(shape.getId().toString());
    static final KeyGetter KEY_ID_NAMESPACE = (shape) -> ListUtils.of(shape.getId().getNamespace());
    static final KeyGetter KEY_ID_NAME = (shape) -> ListUtils.of(shape.getId().getName());
    static final KeyGetter KEY_ID_MEMBER = (shape) -> shape.getId().getMember()
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
    static final KeyGetter KEY_SERVICE_VERSION = (shape) -> shape.asServiceShape()
            .map(ServiceShape::getVersion)
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);

    private final KeyGetter key;
    private final String expected;
    private final Comparator comparator;
    private final boolean caseInsensitive;

    interface KeyGetter extends Function<Shape, List<String>> {}

    interface Comparator extends BiFunction<String, String, Boolean> {}

    AttributeSelector(KeyGetter key) {
        this.key = key;
        expected = null;
        comparator = null;
        caseInsensitive = false;
    }

    AttributeSelector(
            KeyGetter key,
            Comparator comparator,
            String expected,
            boolean caseInsensitive
    ) {
        this.expected = expected;
        this.key = key;
        this.caseInsensitive = caseInsensitive;
        this.comparator = comparator;
    }

    @Override
    public Set<Shape> select(NeighborProvider neighborProvider, Set<Shape> shapes) {
        return shapes.stream()
                .filter(shape -> matchesAttribute(key.apply(shape)))
                .collect(Collectors.toSet());
    }

    private boolean matchesAttribute(List<String> result) {
        if (comparator == null || expected == null) {
            return !result.isEmpty();
        }

        String rhs = caseInsensitive ? expected.toLowerCase(Locale.US) : expected;
        return result.stream()
                .map(value -> caseInsensitive ? value.toLowerCase(Locale.US) : value)
                .anyMatch(lhs -> comparator.apply(lhs, rhs));
    }
}
