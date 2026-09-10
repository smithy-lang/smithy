/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Creates traits from {@link Node} values.
 *
 * <p>This is the interface used to create traits when loading a model.
 * If a trait implementation does not have a corresponding
 * {@link TraitService}, the concrete class for the trait will not be
 * used in code, and instead a {@link DynamicTrait} trait will be used.
 */
public interface TraitService {
    /**
     * @return Gets the shape ID of the trait that this provider created.
     */
    ShapeId getShapeId();

    /**
     * Creates the trait from a node value.
     *
     * @param target The shape targeted by the trait.
     * @param value The value of the trait.
     * @return Returns the created trait.
     */
    Trait createTrait(ShapeId target, Node value);

    /**
     * Creates the trait from a node value, providing the classloader that was used to discover
     * trait providers.
     *
     * <p>Most providers ignore the classloader; the default implementation delegates to
     * {@link #createTrait(ShapeId, Node)}. Providers whose trait value references components
     * discovered through their own SPIs (for example endpoint rule-set functions contributed by an
     * {@code EndpointRuleSetExtension}) can override this to resolve those components from
     * {@code classLoader} rather than the classloader that loaded the provider. This matters when
     * the model is assembled with a caller-supplied classloader (see
     * {@code Model.assembler(ClassLoader)}) whose closure contains extensions that are not visible
     * to the provider's own classloader.
     *
     * @param target The shape targeted by the trait.
     * @param value The value of the trait.
     * @param classLoader The classloader used to discover trait providers, or null if unknown.
     * @return Returns the created trait.
     */
    default Trait createTrait(ShapeId target, Node value, ClassLoader classLoader) {
        return createTrait(target, value);
    }
}
