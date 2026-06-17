/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final Map<String, Integer> variableIndices;

    WrappedSelector(
            String expression,
            List<InternalSelector> selectors,
            List<InternalSelector> roots,
            Map<String, Integer> variableIndices
    ) {
        this.expression = expression;
        this.roots = roots;
        this.variableIndices = Collections.unmodifiableMap(variableIndices);
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
        return select(model, StartingContext.DEFAULT);
    }

    @Override
    public Set<Shape> select(Model model, StartingContext startingContext) {
        Collection<? extends Shape> startingShapes = getStartingShapes(model, startingContext);

        if (isParallel(startingShapes)) {
            return shapes(model).collect(Collectors.toSet());
        } else {
            // This is more optimized than using shapes() for smaller models that aren't parallelized.
            Set<Shape> result = new HashSet<>();
            pushShapes(model, startingShapes, (ctx, s) -> {
                result.add(s);
                return InternalSelector.Response.CONTINUE;
            });
            return result;
        }
    }

    private Collection<? extends Shape> getStartingShapes(Model model, StartingContext startingContext) {
        Collection<? extends Shape> startingShapes = startingContext.getStartingShapes();
        return startingShapes == null ? delegate.getStartingShapes(model) : startingShapes;
    }

    private boolean isParallel(Collection<? extends Shape> startingShapes) {
        return startingShapes.size() >= PARALLEL_THRESHOLD;
    }

    @Override
    public void consumeMatches(Model model, Consumer<ShapeMatch> shapeMatchConsumer) {
        consumeMatches(model, StartingContext.DEFAULT, shapeMatchConsumer);
    }

    @Override
    public void consumeMatches(Model model, StartingContext context, Consumer<ShapeMatch> shapeMatchConsumer) {
        // This is more optimized than using matches() and collecting to a Set
        // because it avoids creating streams and buffering the result of
        // pushing each shape into internal selectors.
        Collection<? extends Shape> startingShapes = getStartingShapes(model, context);
        pushShapes(model, startingShapes, (ctx, s) -> {
            shapeMatchConsumer.accept(new ShapeMatch(s, ctx.getVars()));
            return InternalSelector.Response.CONTINUE;
        });
    }

    @Override
    public Stream<Shape> shapes(Model model, StartingContext startingContext) {
        Collection<? extends Shape> startingShapes = getStartingShapes(model, startingContext);
        NeighborProviderIndex index = NeighborProviderIndex.of(model);
        List<Set<Shape>> computedRoots = computeRoots(model);
        return streamStartingShapes(startingShapes).flatMap(shape -> {
            Context context = new Context(model, index, computedRoots, variableIndices);
            return delegate.pushResultsToCollection(context, shape, new ArrayList<>()).stream();
        });
    }

    @Override
    public Stream<ShapeMatch> matches(Model model, StartingContext startingContext) {
        Collection<? extends Shape> startingShapes = getStartingShapes(model, startingContext);
        NeighborProviderIndex index = NeighborProviderIndex.of(model);
        List<Set<Shape>> computedRoots = computeRoots(model);
        return streamStartingShapes(startingShapes).flatMap(shape -> {
            List<ShapeMatch> result = new ArrayList<>();
            delegate.push(new Context(model, index, computedRoots, variableIndices), shape, (ctx, s) -> {
                result.add(new ShapeMatch(s, ctx.getVars()));
                return InternalSelector.Response.CONTINUE;
            });
            return result.stream();
        });
    }

    private Stream<? extends Shape> streamStartingShapes(Collection<? extends Shape> startingShapes) {
        return isParallel(startingShapes) ? startingShapes.parallelStream() : startingShapes.stream();
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
        Context isolatedContext = new Context(model, index, results, variableIndices);
        Set<Shape> captures = new HashSet<>();
        for (Shape rootShape : shapesToEmit) {
            isolatedContext.clearVariables();
            selector.push(isolatedContext, rootShape, (c, s) -> {
                captures.add(s);
                return InternalSelector.Response.CONTINUE;
            });
        }

        return captures;
    }

    private void pushShapes(
            Model model,
            Collection<? extends Shape> startingShapes,
            InternalSelector.Receiver acceptor
    ) {
        Objects.requireNonNull(startingShapes);
        Context context = new Context(model, NeighborProviderIndex.of(model), computeRoots(model), variableIndices);

        // When the selector's output is independent of the input shape (e.g., it begins with a
        // ":root(...)" rooted subexpression), every starting shape produces the identical result.
        // Pushing a single shape avoids re-deriving the same result once per starting shape, which
        // is O(n^2) work for selectors like ":root(*)".
        if (delegate.isInputShapeIndependent() && !startingShapes.isEmpty()) {
            delegate.push(context, startingShapes.iterator().next(), acceptor);
            return;
        }

        for (Shape shape : startingShapes) {
            context.clearVariables();
            delegate.push(context, shape, acceptor);
        }
    }
}
