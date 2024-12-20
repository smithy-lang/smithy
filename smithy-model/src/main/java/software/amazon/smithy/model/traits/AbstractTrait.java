/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
 *
 * <p>The Node value of a trait can be provided when the trait is created
 * using {@link #setNodeCache(Node)}. Note that when setting the node cache,
 * the equality and hashcode of the trait are impacted because they are by
 * default based on the{@link #toNode()} value of a trait. This typically
 * isn't an issue until model transformations are performed that modify a
 * trait. In these cases, the original node value of the trait might differ
 * from the updated trait even if they are semantically the same value (for
 * example, if the only change to the trait is modifying its source location,
 * or if a property of the trait was explicitly set to false, but false is
 * omitted when serializing the updated trait to a node value). If this use
 * case needs to be accounted for, you must override equals and hashCode of
 * the trait.
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

    /**
     * @param id ID of the trait.
     * @param nodeValue The node representation of the shape, if known and trusted.
     */
    public AbstractTrait(ShapeId id, Node nodeValue) {
        this(id, nodeValue.getSourceLocation());
        setNodeCache(nodeValue);
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
        if (other == this) {
            return true;
        } else if (!(other instanceof Trait)) {
            return false;
        } else if (hashCode() != other.hashCode()) { // take advantage of hashcode caching
            return false;
        } else {
            Trait b = (Trait) other;
            return toShapeId().equals(b.toShapeId()) && toNode().equals(b.toNode());
        }
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
            setNodeCache(createNode());
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
     * Sets the node cache of the trait up front, if known.
     *
     * <p>This is useful for maintaining a trait value exactly as provided in
     * a model file, allowing for validation to detect extraneous properties,
     * and removing the need to create the node again when calling createNode.
     *
     * @param value Value to set.
     */
    protected final void setNodeCache(Node value) {
        this.nodeCache = value;
    }

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
