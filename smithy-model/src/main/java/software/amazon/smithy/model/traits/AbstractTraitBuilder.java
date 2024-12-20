/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Provides the shared logic that all trait builders need.
 *
 * @param <T> Trait being built.
 * @param <B> Builder that is building the trait.
 */
public abstract class AbstractTraitBuilder<T extends Trait, B extends AbstractTraitBuilder>
        implements SmithyBuilder<T> {

    SourceLocation sourceLocation = SourceLocation.NONE;

    /**
     * Sets the source location of where the trait was defined.
     *
     * @param sourceLocation Location of the trait.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B sourceLocation(FromSourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation.getSourceLocation();
        return (B) this;
    }

    /**
     * Gets the source location configured for the builder.
     *
     * @return Returns the source location or null if not set.
     */
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }
}
