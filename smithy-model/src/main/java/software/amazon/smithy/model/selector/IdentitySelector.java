/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * An optimized Selector implementation that uses the provided Model directly
 * rather than needing to send each shape through the Selector machinery.
 *
 * @see Selector#IDENTITY
 */
final class IdentitySelector implements Selector {
    @Override
    public Set<Shape> select(Model model) {
        return select(model, StartingContext.DEFAULT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Shape> select(Model model, StartingContext context) {
        Collection<? extends Shape> startingShapes = context.getStartingShapes();
        if (startingShapes == null) {
            return model.toSet();
        } else if (startingShapes instanceof Set) {
            return (Set<Shape>) startingShapes;
        } else {
            return new HashSet<>(startingShapes);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Shape> shapes(Model model, StartingContext context) {
        Collection<? extends Shape> startingShapes = context.getStartingShapes();
        if (startingShapes == null) {
            return model.shapes();
        } else {
            return (Stream<Shape>) startingShapes.stream();
        }
    }

    @Override
    public Stream<ShapeMatch> matches(Model model, StartingContext context) {
        Collection<? extends Shape> startingShapes = context.getStartingShapes();
        Stream<? extends Shape> shapeStream = startingShapes == null ? model.shapes() : startingShapes.stream();
        return shapeStream.map(shape -> new ShapeMatch(shape, Collections.emptyMap()));
    }

    @Override
    public String toString() {
        return "*";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Selector && toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
