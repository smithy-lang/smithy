/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Objects;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;

/**
 * Provides a cache of precomputed neighbors for models.
 */
public final class NeighborProviderIndex implements KnowledgeIndex {

    private final NeighborProvider provider;
    private final WeakReference<Model> model;

    // These providers are lazily computed on first access.
    private volatile NeighborProvider reversed;
    private volatile NeighborProvider providerWithTraits;
    private volatile NeighborProvider reversedWithTraits;

    public NeighborProviderIndex(Model model) {
        provider = NeighborProvider.precomputed(model);

        // Store a WeakReference to the model since the reversed provider that includes
        // traits is lazily computed.
        this.model = new WeakReference<>(model);
    }

    public static NeighborProviderIndex of(Model model) {
        return model.getKnowledge(NeighborProviderIndex.class, NeighborProviderIndex::new);
    }

    /**
     * Gets the precomputed neighbor provider.
     *
     * @return Returns the provider.
     */
    public NeighborProvider getProvider() {
        return provider;
    }

    /**
     * Gets the neighbor provider that includes trait relationships.
     *
     * @return Returns the provider.
     */
    public NeighborProvider getProviderWithTraitRelationships() {
        NeighborProvider result = providerWithTraits;

        if (result == null) {
            Model model = getOrThrowModel();
            synchronized (this) {
                result = providerWithTraits;
                if (result == null) {
                    providerWithTraits = result = NeighborProvider.cached(
                            NeighborProvider.withTraitRelationships(model, provider));
                }
            }
        }

        return result;
    }

    /**
     * Gets a reversed, bottom up neighbor provider.
     *
     * @return Returns the reversed neighbor provider.
     */
    public NeighborProvider getReverseProvider() {
        NeighborProvider result = reversed;

        if (result == null) {
            Model model = getOrThrowModel();
            synchronized (this) {
                result = reversed;
                if (result == null) {
                    reversed = result = NeighborProvider.reverse(model, provider);
                }
            }
        }

        return result;
    }

    /**
     * Gets a reversed, bottom up neighbor provider that includes reverse traits.
     *
     * @return Returns the reversed neighbor provider with reverse traits.
     */
    public NeighborProvider getReverseProviderWithTraitRelationships() {
        NeighborProvider result = reversedWithTraits;

        if (result == null) {
            synchronized (this) {
                result = reversedWithTraits;
                if (result == null) {
                    NeighborProvider withTraits = getProviderWithTraitRelationships();
                    reversedWithTraits = result = NeighborProvider.reverse(getOrThrowModel(), withTraits);
                }
            }
        }

        return result;
    }

    private Model getOrThrowModel() {
        return Objects.requireNonNull(model.get(), "Model was destroyed before using this knowledge index");
    }
}
