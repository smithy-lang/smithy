/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains abstract functionality shared by the resource lifecycle traits
 * ({@code @createsResources}, {@code @deletesResources}, {@code @putsResources},
 * {@code @readsResources}, and {@code @updatesResources}), each of which is a
 * list of {@link ResourceBinding}.
 *
 * @param <R> The concrete binding type held by the trait. Create, put, read, and
 *  update use {@link ResourceLifecycleBinding}; delete uses
 *  {@link ResourceDeletionBinding}, which has no properties.
 */
@SmithyUnstableApi
public abstract class AbstractResourceLifecycleTrait<R extends ResourceBinding> extends AbstractTrait {

    private final List<R> bindings;

    protected AbstractResourceLifecycleTrait(ShapeId id, Builder<?, ?, R> builder) {
        super(id, builder.getSourceLocation());
        this.bindings = builder.bindings.copy();
    }

    /**
     * @return Gets the resource lifecycle bindings of the trait.
     */
    public final List<R> getBindings() {
        return bindings;
    }

    @Override
    protected final Node createNode() {
        List<Node> nodes = new ArrayList<>(bindings.size());
        for (R binding : bindings) {
            nodes.add(binding.toNode());
        }
        return new ArrayNode(nodes, getSourceLocation());
    }

    /**
     * Trait provider that parses a list of binding objects.
     *
     * @param <T> The concrete lifecycle trait type to create.
     * @param <R> The concrete binding type parsed for each list element.
     */
    public static class Provider<T extends AbstractResourceLifecycleTrait<R>, R extends ResourceBinding>
            extends AbstractTrait.Provider {
        private final Supplier<Builder<T, ?, R>> builderFactory;
        private final Function<Node, R> bindingReader;

        /**
         * @param id The ID of the trait being created.
         * @param builderFactory Creates an empty builder for the concrete trait.
         * @param bindingReader Parses a single binding from its node representation.
         */
        public Provider(ShapeId id, Supplier<Builder<T, ?, R>> builderFactory, Function<Node, R> bindingReader) {
            super(id);
            this.builderFactory = builderFactory;
            this.bindingReader = bindingReader;
        }

        @Override
        public T createTrait(ShapeId id, Node value) {
            Builder<T, ?, R> builder = builderFactory.get();
            builder.sourceLocation(value.getSourceLocation());
            for (ObjectNode member : value.expectArrayNode().getElementsAs(ObjectNode.class)) {
                builder.addBinding(bindingReader.apply(member));
            }
            T result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Abstract builder for resource lifecycle traits.
     *
     * @param <T> The concrete trait type built by this builder.
     * @param <B> The concrete builder type.
     * @param <R> The concrete binding type held by the builder.
     */
    public abstract static class Builder<
            T extends AbstractResourceLifecycleTrait<R>,
            B extends Builder<T, B, R>,
            R extends ResourceBinding> extends AbstractTraitBuilder<T, B> {

        private final BuilderRef<List<R>> bindings = BuilderRef.forList();

        protected Builder() {}

        protected Builder(AbstractResourceLifecycleTrait<R> trait) {
            sourceLocation(trait.getSourceLocation());
            this.bindings.setBorrowed(trait.getBindings());
        }

        /**
         * Replaces all bindings in the builder with the given bindings.
         *
         * @param bindings Bindings to set.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B bindings(List<R> bindings) {
            clearBindings();
            this.bindings.get().addAll(bindings);
            return (B) this;
        }

        /**
         * Adds a single binding to the builder.
         *
         * @param binding Binding to add.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B addBinding(R binding) {
            this.bindings.get().add(binding);
            return (B) this;
        }

        /**
         * Clears all bindings from the builder.
         *
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B clearBindings() {
            bindings.clear();
            return (B) this;
        }
    }
}
