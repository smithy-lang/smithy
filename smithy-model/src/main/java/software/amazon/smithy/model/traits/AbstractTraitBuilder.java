/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
