/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.rulesengine.language.EndpointComponentFactory;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/***
 * Defines an endpoint rule-set used to resolve the client's transport endpoint.
 */
@SmithyUnstableApi
public final class EndpointRuleSetTrait extends AbstractTrait implements ToSmithyBuilder<EndpointRuleSetTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#endpointRuleSet");

    private final Node ruleSet;
    private final ClassLoader classLoader;
    private volatile EndpointComponentFactory componentFactory;
    private EndpointRuleSet endpointRuleSet;

    private EndpointRuleSetTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        ruleSet = SmithyBuilder.requiredState("ruleSet", builder.ruleSet);
        classLoader = builder.classLoader;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Node getRuleSet() {
        return ruleSet;
    }

    /**
     * Gets the {@link EndpointComponentFactory} used to resolve this rule-set's functions,
     * built-ins, and auth-scheme validators.
     *
     * <p>Built lazily from the classloader captured when this trait was created (the classloader
     * used to assemble the model) and cached, so endpoint extensions are discovered once and reused
     * across deserialization and validation. Extensions living only in a caller-supplied closure
     * classloader (for example {@code smithy-aws-endpoints}, which registers {@code aws.partition})
     * are found because that closure classloader is used here.
     *
     * @return the factory used to resolve endpoint components.
     */
    @SmithyInternalApi
    public EndpointComponentFactory getComponentFactory() {
        EndpointComponentFactory result = componentFactory;
        if (result == null) {
            // Reuse the shared default factory when this rule-set does not need a distinct
            // classloader: either no loader was captured, or it is the same loader that would back
            // the default. This avoids a redundant ServiceLoader scan for the common case where many
            // models resolve against the same (app/default) classpath. A closure classloader
            // different from the default gets its own factory, cached on this trait instance.
            ClassLoader ownLoader = EndpointRuleSet.class.getClassLoader();
            if (classLoader == null || classLoader == ownLoader) {
                result = EndpointRuleSet.getDefaultComponentFactory();
            } else {
                result = EndpointComponentFactory.createServiceFactory(classLoader);
            }
            componentFactory = result;
        }
        return result;
    }

    public EndpointRuleSet getEndpointRuleSet() {
        // EndpointRuleSet creation loads an SPI of functions, builtins, and more.
        // That work is deferred until necessary, usually when a ruleset is being validated.
        // The component factory (built from the classloader captured at trait-creation time) is
        // passed so functions contributed by extensions in a caller-supplied closure are resolved.
        if (endpointRuleSet == null) {
            endpointRuleSet = EndpointRuleSet.fromNode(ruleSet, getComponentFactory());
        }
        return endpointRuleSet;
    }

    @Override
    protected Node createNode() {
        return ruleSet;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .classLoader(classLoader)
                .ruleSet(ruleSet);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return createTrait(target, value, null);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value, ClassLoader classLoader) {
            EndpointRuleSetTrait trait = builder().sourceLocation(value)
                    .ruleSet(value)
                    .classLoader(classLoader)
                    .build();
            trait.setNodeCache(value);
            return trait;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointRuleSetTrait, Builder> {
        private Node ruleSet;
        private ClassLoader classLoader;

        private Builder() {}

        public Builder ruleSet(Node ruleSet) {
            this.ruleSet = ruleSet;
            return this;
        }

        /**
         * Sets the classloader used to discover endpoint extensions when the rule-set is later
         * materialized. Typically the classloader used to assemble the model. May be null.
         *
         * @param classLoader the classloader, or null.
         * @return this builder.
         */
        @SmithyInternalApi
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        @Override
        public EndpointRuleSetTrait build() {
            return new EndpointRuleSetTrait(this);
        }
    }
}
