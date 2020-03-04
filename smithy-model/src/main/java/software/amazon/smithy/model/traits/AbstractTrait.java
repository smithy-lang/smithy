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
import software.amazon.smithy.model.shapes.ShapeId;

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

    private final transient ShapeId traitId;
    private final transient SourceLocation traitSourceLocation;
    private transient int cachedHashCode = 0;
    private transient Node nodeCache;

    /**
     * @param id ID of the trait.
     * @param sourceLocation Where the trait was defined.
     */
    public AbstractTrait(ShapeId id, FromSourceLocation sourceLocation) {
        this.traitId = Objects.requireNonNull(id, "id was not set on trait");
        this.traitSourceLocation = Objects.requireNonNull(sourceLocation, "sourceLocation was not set on trait")
                .getSourceLocation();
    }

    @Override
    public final ShapeId toShapeId() {
        return traitId;
    }

    @Override
    public final SourceLocation getSourceLocation() {
        return traitSourceLocation;
    }

    @Override
    public String toString() {
        return String.format("Trait `%s`, defined at %s", toShapeId(), getSourceLocation());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Trait)) {
            return false;
        }

        Trait b = (Trait) other;
        return this == other || (toShapeId().equals(b.toShapeId()) && toNode().equals(b.toNode()));
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            cachedHashCode = toShapeId().hashCode() * 17 + toNode().hashCode();
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
        private final ShapeId id;

        /**
         * @param id ID of the trait that the provider creates.
         */
        public Provider(ShapeId id) {
            this.id = id;
        }

        @Override
        public ShapeId getShapeId() {
            return id;
        }
    }
}
