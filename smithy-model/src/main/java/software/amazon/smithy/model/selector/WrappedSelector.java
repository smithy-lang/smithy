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

import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Provides a toString method that prints the expression.
 *
 * <p>This is the only concrete implementation of Selector that is
 * exported from the package.
 */
final class WrappedSelector implements Selector {
    private final String expression;
    private final Selector delegate;

    WrappedSelector(String expression, Selector delegate) {
        this.expression = expression;
        this.delegate = delegate;
    }

    @Override
    public Set<Shape> select(Model model, NeighborProvider neighborProvider, Set<Shape> shapes) {
        return delegate.select(model, neighborProvider, shapes);
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WrappedSelector && expression.equals(((WrappedSelector) other).expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }
}
