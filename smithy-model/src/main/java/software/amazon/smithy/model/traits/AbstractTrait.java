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

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Base implementation of traits.
 *
 * <p>This implementation provides an {@link #equals} and {@link #hashCode}
 * that should work for most traits that extend from this base class. Note
 * that equality for traits that extend from this type are not based on the
 * concrete class, but rather the trait name and the trait's {@link ToNode}
 * representation.
 */
public abstract class AbstractTrait implements Trait {

    private final String name;
    private final SourceLocation sourceLocation;
    private int cachedHashCode = 0;
    private Node nodeCache;

    /**
     * @param name Fully-qualified Name of the trait.
     * @param sourceLocation Where the trait was defined.
     */
    public AbstractTrait(String name, FromSourceLocation sourceLocation) {
        this.name = Objects.requireNonNull(name, "name was not set on trait");
        this.sourceLocation = Objects.requireNonNull(sourceLocation, "sourceLocation was not set on trait")
                .getSourceLocation();
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public String toString() {
        return String.format("Trait `%s`, defined at %s", getName(), getSourceLocation());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Trait)) {
            return false;
        }

        Trait b = (Trait) other;
        return this == other || (getName().equals(b.getName()) && toNode().equals(b.toNode()));
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            cachedHashCode = getName().hashCode() * 17 + toNode().hashCode();
        }
        return cachedHashCode;
    }

    @Override
    public final Node toNode() {
        if (nodeCache == null) {
            nodeCache = createNode();
        }
        return nodeCache;
    }

    /**
     * The result of toNode is used for hashCodes and equality. Subclasses
     * must implement createNode to turn the trait into a Node. This is then
     * cached for subsequent retrievals.
     *
     * @return Returns the trait as a node.
     */
    protected abstract Node createNode();

    /**
     * Basic provider implementation that returns the name of the
     * provided trait.
     */
    public abstract static class Provider implements TraitService {
        private final String name;

        /**
         * @param name Name of the trait that the provider creates.
         */
        public Provider(String name) {
            this.name = name;
        }

        @Override
        public String getTraitName() {
            return name;
        }
    }
}
