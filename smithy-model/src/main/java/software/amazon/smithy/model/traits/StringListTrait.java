/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;

/**
 * Contains abstract functionality to build traits that contain a list
 * of strings.
 */
public abstract class StringListTrait extends AbstractTrait {
    private final List<String> values;

    /**
     * @param id The id of the trait being created.
     * @param values The string values of the trait.
     * @param sourceLocation Where the trait was defined.
     */
    public StringListTrait(ShapeId id, List<String> values, FromSourceLocation sourceLocation) {
        super(id, sourceLocation);
        this.values = Objects.requireNonNull(values, "values must not be null");
    }

    @Override
    protected final Node createNode() {
        List<Node> nodes = new ArrayList<>(values.size());
        for (String value : values) {
            nodes.add(Node.from(value));
        }
        return new ArrayNode(nodes, getSourceLocation());
    }

    /**
     * Factory method to create a StringList provider.
     *
     * @param <T> Type of StringList trait to create.
     */
    @FunctionalInterface
    public interface StringListTraitConstructor<T extends StringListTrait> {
        /**
         * Wraps the constructor of a StringList trait.
         *
         * @param values Values to pass to the trait.
         * @param sourceLocation The location in which the trait was defined.
         * @return Returns the created StringList trait.
         */
        T create(List<String> values, FromSourceLocation sourceLocation);
    }

    /**
     * @return Gets the trait values.
     */
    public final List<String> getValues() {
        return values;
    }

    /**
     * Abstract builder to build a StringList trait.
     */
    public abstract static class Builder<TRAIT extends StringListTrait, BUILDER extends Builder>
            extends AbstractTraitBuilder<TRAIT, BUILDER> {

        private final BuilderRef<List<String>> values = BuilderRef.forList();

        /**
         * Gets the values set in the builder.
         *
         * @return Returns the set values.
         */
        public List<String> getValues() {
            return values.copy();
        }

        /**
         * Adds a value to the builder.
         *
         * @param value Value to add.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER addValue(String value) {
            values.get().add(Objects.requireNonNull(value));
            return (BUILDER) this;
        }

        /**
         * Replaces all of the values in the builder with the given values.
         *
         * @param values Value to replace into the builder.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER values(Collection<String> values) {
            clearValues();
            this.values.get().addAll(values);
            return (BUILDER) this;
        }

        /**
         * Removes a value from the builder.
         *
         * @param value Value to remove.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER removeValue(String value) {
            this.values.get().remove(value);
            return (BUILDER) this;
        }

        /**
         * Clears all values out of the builder.
         *
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public BUILDER clearValues() {
            values.clear();
            return (BUILDER) this;
        }
    }

    /**
     * Trait provider that expects a list of string values.
     */
    public static class Provider<T extends StringListTrait> extends AbstractTrait.Provider {
        private final BiFunction<List<String>, SourceLocation, T> traitFactory;

        /**
         * @param id The ID of the trait being created.
         * @param traitFactory The factory used to create the trait.
         */
        public Provider(ShapeId id, BiFunction<List<String>, SourceLocation, T> traitFactory) {
            super(id);
            this.traitFactory = traitFactory;
        }

        @Override
        public T createTrait(ShapeId id, Node value) {
            List<String> values = Node.loadArrayOfString(id.toString(), value);
            T result = traitFactory.apply(values, value.getSourceLocation());
            result.setNodeCache(value);
            return result;
        }
    }
}
