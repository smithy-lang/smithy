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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Provides a toString method that prints the expression.
 */
final class WrappedSelector implements Selector {

    /** Uses parallel streams when the model size exceeds this number. */
    private static final int PARALLEL_THRESHOLD = 10000;

    private final String expression;
    private final InternalSelector delegate;
    private final List<InternalSelector> roots;

    WrappedSelector(String expression, List<InternalSelector> selectors, List<InternalSelector> roots) {
        this.expression = expression;
        this.roots = roots;
        this.delegate = AndSelector.of(selectors);
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Selector && toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    @Override
    public Set<Shape> select(Model model) {
        if (isParallel(model)) {
            return shapes(model).collect(Collectors.toSet());
        } else {
            Set<Shape> result = new HashSet<>();
            // This is more optimized than using shapes() for smaller models
            // that aren't parallelized.
            pushShapes(model, (ctx, s) -> {
                result.add(s);
                return InternalSelector.Response.CONTINUE;
            });
            return result;
        }
    }

    @Override
    public void consumeMatches(Model model, Consumer<ShapeMatch> shapeMatchConsumer) {
        // This is more optimized than using matches() and collecting to a Set
        // because it avoids creating streams and buffering the result of
        // pushing each shape into internal selectors.
        pushShapes(model, (ctx, s) -> {
            shapeMatchConsumer.accept(new ShapeMatch(s, ctx.getVars()));
            return InternalSelector.Response.CONTINUE;
        });
    }

    @Override
    public Stream<Shape> shapes(Model model) {
        NeighborProviderIndex index = NeighborProviderIndex.of(model);
        List<Set<Shape>> computedRoots = computeRoots(model);
        return streamStartingShape(model).flatMap(shape -> {
            Context context = new Context(model, index, computedRoots);
            return delegate.pushResultsToCollection(context, shape, new ArrayList<>()).stream();
        });
    }

    @Override
    public Stream<ShapeMatch> matches(Model model) {
        NeighborProviderIndex index = NeighborProviderIndex.of(model);
        List<Set<Shape>> computedRoots = computeRoots(model);
        return streamStartingShape(model).flatMap(shape -> {
            List<ShapeMatch> result = new ArrayList<>();
            delegate.push(new Context(model, index, computedRoots), shape, (ctx, s) -> {
                result.add(new ShapeMatch(s, ctx.getVars()));
                return InternalSelector.Response.CONTINUE;
            });
            return result.stream();
        });
    }

    // Eagerly compute roots over all model shapes before evaluating shapes one at a time.
    private List<Set<Shape>> computeRoots(Model model) {
        NeighborProviderIndex index = NeighborProviderIndex.of(model);
        List<Set<Shape>> rootResults = new ArrayList<>(roots.size());
        for (InternalSelector selector : roots) {
            Set<Shape> result = evalRoot(model, index, selector, rootResults);
            rootResults.add(result);
        }
        return rootResults;
    }

    // Eagerly compute a root subexpression.
    private Set<Shape> evalRoot(
            Model model,
            NeighborProviderIndex index,
            InternalSelector selector,
            List<Set<Shape>> results
    ) {
        Collection<? extends Shape> shapesToEmit = selector.getStartingShapes(model);
        Context isolatedContext = new Context(model, index, results);
        Set<Shape> captures = new HashSet<>();
        for (Shape rootShape : shapesToEmit) {
            isolatedContext.getVars().clear();
            selector.push(isolatedContext, rootShape, (c, s) -> {
                captures.add(s);
                return InternalSelector.Response.CONTINUE;
            });
        }

        return captures;
    }

    private void pushShapes(Model model, InternalSelector.Receiver acceptor) {
        Context context = new Context(model, NeighborProviderIndex.of(model), computeRoots(model));
        Collection<? extends Shape> shapes = delegate.getStartingShapes(model);
        for (Shape shape : shapes) {
            context.getVars().clear();
            delegate.push(context, shape, acceptor);
        }
    }

    private Stream<? extends Shape> streamStartingShape(Model model) {
        Collection<? extends Shape> startingShapes = delegate.getStartingShapes(model);
        return startingShapes.size() > PARALLEL_THRESHOLD
               ? startingShapes.parallelStream()
               : startingShapes.stream();
    }

    private boolean isParallel(Model model) {
        return model.getShapeIds().size() >= PARALLEL_THRESHOLD;
    }
}
