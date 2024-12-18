/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Matches a set of shapes using a selector expression.
 */
public interface Selector {

    /** A selector that always returns all provided values. */
    Selector IDENTITY = new IdentitySelector();

    /**
     * Starting environment context object used when evaluating a selector.
     */
    final class StartingContext {

        public static final StartingContext DEFAULT = new StartingContext();

        // If necessary, it's possible that we could also support predefined selector variables too.
        private final Collection<? extends Shape> startingShapes;

        /**
         * Create a StartingContext that sends all shapes in a Model through the Selector.
         */
        public StartingContext() {
            this(null);
        }

        /**
         * @param startingShapes A specific set of shapes to send through the Selector rather than all Model shapes.
         */
        public StartingContext(Collection<? extends Shape> startingShapes) {
            this.startingShapes = startingShapes;
        }

        /**
         * Get the potentially null set of starting shapes to provide to the selector.
         *
         * @return Returns the custom set starting shapes to provide to the selector.
         */
        public Collection<? extends Shape> getStartingShapes() {
            return startingShapes;
        }
    }

    /**
     * Parses a selector expression.
     *
     * @param expression Expression to parse.
     * @return Returns the parsed {@link Selector}.
     */
    static Selector parse(String expression) {
        if (expression.equals("*")) {
            return IDENTITY;
        } else {
            return SelectorParser.parse(expression);
        }
    }

    /**
     * Creates a Selector from a {@link Node}.
     *
     * @param node Node to parse.
     * @return Returns the created selector.
     * @throws SourceException on error.
     */
    static Selector fromNode(Node node) {
        try {
            return parse(node.expectStringNode().getValue());
        } catch (SelectorSyntaxException e) {
            throw new SourceException(e.getMessage(), node, e);
        }
    }

    /**
     * Matches a selector to a model.
     *
     * @param model Model used to resolve shapes with.
     * @return Returns the matching shapes.
     */
    default Set<Shape> select(Model model) {
        // Methods for providing a starting environment were introduced after initially releasing the Selector
        // interface. Typically, you'd call select(model, Env.DEFAULT) here, but to maintain backward compatibility,
        // this calls shapes(model). Implementations of this interface should override both this method and
        // select(Model, Env);
        return shapes(model).collect(Collectors.toSet());
    }

    /**
     * Matches a selector to a model.
     *
     * @param model   Model used to resolve shapes with.
     * @param context Selector starting environment context.
     * @return Returns the matching shapes.
     */
    default Set<Shape> select(Model model, StartingContext context) {
        return shapes(model, context).collect(Collectors.toSet());
    }

    /**
     * Matches a selector to a set of shapes and receives each matched shape
     * with the variables that were set when the shape was matched.
     *
     * @param model Model to select shapes from.
     * @param shapeMatchConsumer Receives each matched shape and the vars available when the shape was matched.
     */
    default void consumeMatches(Model model, Consumer<ShapeMatch> shapeMatchConsumer) {
        // Methods for providing a starting environment were introduced after initially releasing the Selector
        // interface. Typically, you'd call matches(Model, Env, Consumer) here, but to maintain backward compatibility,
        // this calls matches(model). Implementations of this interface should override both this method and
        // consumeMatches(Model, Env, Consumer);
        matches(model).forEach(shapeMatchConsumer);
    }

    /**
     * Matches a selector to a set of shapes and receives each matched shape
     * with the variables that were set when the shape was matched.
     *
     * @param model   Model to select shapes from.
     * @param context Selector starting environment context.
     * @param shapeMatchConsumer Receives each matched shape and the vars available when the shape was matched.
     */
    default void consumeMatches(Model model, StartingContext context, Consumer<ShapeMatch> shapeMatchConsumer) {
        matches(model, context).forEach(shapeMatchConsumer);
    }

    /**
     * Returns a stream of shapes in a model that match the selector.
     *
     * @param model Model to match the selector against.
     * @return Returns a stream of matching shapes.
     */
    default Stream<Shape> shapes(Model model) {
        return shapes(model, StartingContext.DEFAULT);
    }

    /**
     * Returns a stream of shapes in a model that match the selector.
     *
     * @param model   Model to match the selector against.
     * @param context Selector starting environment context.
     * @return Returns a stream of matching shapes.
     */
    default Stream<Shape> shapes(Model model, StartingContext context) {
        return matches(model, context).map(ShapeMatch::getShape);
    }

    /**
     * Returns a stream of {@link ShapeMatch} objects for each match found in
     * a model.
     *
     * @param model Model to match the selector against.
     * @return Returns a stream of {@code ShapeMatch} objects.
     */
    default Stream<ShapeMatch> matches(Model model) {
        return matches(model, StartingContext.DEFAULT);
    }

    /**
     * Returns a stream of {@link ShapeMatch} objects for each match found in
     * a model.
     *
     * @param model           Model to match the selector against.
     * @param startingContext Selector starting environment context.
     * @return Returns a stream of {@code ShapeMatch} objects.
     */
    default Stream<ShapeMatch> matches(Model model, StartingContext startingContext) {
        // Needed for backward compatibility with potentially already existing Selectors.
        throw new UnsupportedOperationException("matches(model, context) is not implemented");
    }

    /**
     * Represents a selector match found in the model.
     *
     * <p>The {@code getShape} method is used to get the shape that matched,
     * and all of the contextual variables that were set when the match
     * occurred can be accessed using typical {@link Map} methods like
     * {@code get}, {@code contains}, etc.
     */
    final class ShapeMatch extends HashMap<String, Set<Shape>> {
        private final Shape shape;

        /**
         * @param shape Shape that matched.
         * @param variables Variables that matched. This map is copied into ShapeMatch.
         */
        public ShapeMatch(Shape shape, Map<String, Set<Shape>> variables) {
            super(variables);
            this.shape = shape;
        }

        /**
         * Gets the matching shape.
         *
         * @return Returns the matching shape.
         */
        public Shape getShape() {
            return shape;
        }
    }

    /**
     * Creates a Selector {@code Runner}, used to customize how a selector is
     * executed.
     *
     * @return Returns the created runner.
     */
    @Deprecated
    default Runner runner() {
        return new Runner(this);
    }

    /**
     * Builds the execution environment for a selector and executes selectors.
     *
     * @deprecated This class is no longer necessary. It was originally intended
     *  to allow more customization to how selectors are executed against a model,
     *  but this has proven unnecessary.
     */
    @Deprecated
    final class Runner {

        private final Selector selector;
        private Model model;

        Runner(Selector selector) {
            this.selector = selector;
        }

        @Deprecated
        public Runner model(Model model) {
            this.model = model;
            return this;
        }

        @Deprecated
        public Set<Shape> selectShapes() {
            return selector.select(Objects.requireNonNull(model, "model not set"));
        }

        @Deprecated
        public void selectMatches(BiConsumer<Shape, Map<String, Set<Shape>>> matchConsumer) {
            selector.consumeMatches(Objects.requireNonNull(model, "model not set"),
                    m -> matchConsumer.accept(m.getShape(), m));
        }
    }
}
