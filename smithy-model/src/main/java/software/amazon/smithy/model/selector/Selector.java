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
     * Selects matching shapes from a model using a specific set of starting shapes.
     *
     * <p>This method makes chaining the results of one selector into another easier.
     *
     * @param model Model to query.
     * @param startingShapes Shapes to push through the selector.
     * @return Returns the matching shapes.
     */
    default Set<Shape> select(Model model, Collection<? extends Shape> startingShapes) {
        throw new UnsupportedOperationException("Selecting using starting shapes is not supported by this Selector");
    }

    /**
     * Matches a selector to a model.
     *
     * @param model Model used to resolve shapes with.
     * @return Returns the matching shapes.
     */
    default Set<Shape> select(Model model) {
        return shapes(model).collect(Collectors.toSet());
    }

    /**
     * Matches a selector to a set of shapes and receives each matched shape
     * with the variables that were set when the shape was matched.
     *
     * @param model Model to select shapes from.
     * @param shapeMatchConsumer Receives each matched shape and the vars available when the shape was matched.
     */
    default void consumeMatches(Model model, Consumer<ShapeMatch> shapeMatchConsumer) {
        matches(model).forEach(shapeMatchConsumer);
    }

    /**
     * Returns a stream of shapes in a model that match the selector.
     *
     * @param model Model to match the selector against.
     * @return Returns a stream of matching shapes.
     */
    Stream<Shape> shapes(Model model);

    /**
     * Returns a stream of {@link ShapeMatch} objects for each match found in
     * a model.
     *
     * @param model Model to match the selector against.
     * @return Returns a stream of {@code ShapeMatch} objects.
     */
    Stream<ShapeMatch> matches(Model model);

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
