/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Abstract builder used to create {@link Shape}s.
 *
 * @param <B> Concrete builder type.
 * @param <S> Shape being created.
 */
public abstract class AbstractShapeBuilder<B extends AbstractShapeBuilder<B, S>, S extends Shape>
        implements SmithyBuilder<S>, FromSourceLocation {

    private ShapeId id;
    private Map<ShapeId, Trait> traits;
    private SourceLocation source = SourceLocation.none();
    private Map<ShapeId, Shape> mixins;

    AbstractShapeBuilder() {}

    @Override
    public SourceLocation getSourceLocation() {
        return source;
    }

    /**
     * Gets the type of shape being built.
     *
     * @return Returns the shape type.
     */
    public abstract ShapeType getShapeType();

    /**
     * Gets the shape ID of the builder.
     *
     * @return Returns null if no shape ID is set.
     */
    public ShapeId getId() {
        return id;
    }

    /**
     * Sets the shape ID of the shape.
     *
     * @param shapeId Shape ID to set.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B id(ShapeId shapeId) {
        id = shapeId;
        return (B) this;
    }

    /**
     * Sets the shape ID of the shape.
     *
     * @param shapeId Absolute shape ID string to set.
     * @return Returns the builder.
     * @throws ShapeIdSyntaxException if the shape ID is invalid.
     */
    public final B id(String shapeId) {
        return id(ShapeId.from(shapeId));
    }

    /**
     * Sets the source location of the shape.
     *
     * @param sourceLocation Source location to set.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public final B source(SourceLocation sourceLocation) {
        if (sourceLocation == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        source = sourceLocation;
        return (B) this;
    }

    /**
     * Sets the source location of the shape.
     *
     * @param filename Name of the file in which the shape was defined.
     * @param line Line number in the file where the shape was defined.
     * @param column Column number of the line where the shape was defined.
     * @return Returns the builder.
     */
    public final B source(String filename, int line, int column) {
        return source(new SourceLocation(filename, line, column));
    }

    /**
     * Replace all traits in the builder.
     *
     * @param traitsToSet Sequence of traits to set on the builder.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public final B traits(Collection<? extends Trait> traitsToSet) {
        clearTraits();
        for (Trait trait : traitsToSet) {
            addTrait(trait);
        }
        return (B) this;
    }

    /**
     * Adds traits from an iterator to the shape builder, replacing any
     * conflicting traits.
     *
     * @param traitsToAdd Sequence of traits to add to the builder.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public final B addTraits(Collection<? extends Trait> traitsToAdd) {
        for (Trait trait : traitsToAdd) {
            addTrait(trait);
        }
        return (B) this;
    }

    /**
     * Adds a trait to the shape builder, replacing any conflicting traits.
     *
     * @param trait Trait instance to add.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public final B addTrait(Trait trait) {
        Objects.requireNonNull(trait, "trait must not be null");

        if (traits == null) {
            traits = new HashMap<>();
        }

        traits.put(trait.toShapeId(), trait);
        return (B) this;
    }

    /**
     * Removes a trait from the shape builder.
     *
     * <p>A relative trait name will attempt to remove a prelude trait
     * with the given name.
     *
     * @param traitId Absolute or relative ID of the trait to remove.
     * @return Returns the builder.
     */
    public final B removeTrait(String traitId) {
        return removeTrait(ShapeId.from(Trait.makeAbsoluteName(traitId)));
    }

    /**
     * Removes a trait from the shape builder.
     *
     * @param traitId ID of the trait to remove.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public final B removeTrait(ShapeId traitId) {
        if (traits != null) {
            traits.remove(traitId);
        }
        return (B) this;
    }

    /**
     * Removes all traits.
     *
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public final B clearTraits() {
        traits = null;
        return (B) this;
    }

    /**
     * Adds a member to the shape IFF the shape supports members.
     *
     * @param member Member to add to the shape.
     * @return Returns the model assembler.
     * @throws UnsupportedOperationException if the shape does not support members.
     */
    public B addMember(MemberShape member) {
        throw new UnsupportedOperationException(String.format(
                "Member `%s` cannot be added to %s", member.getId(), getClass().getName()));
    }

    /**
     * Adds a mixin to the shape.
     *
     * @param shape Mixin to add.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B addMixin(Shape shape) {
        if (mixins == null) {
            mixins = new LinkedHashMap<>();
        }

        mixins.put(shape.getId(), shape);
        return (B) this;
    }

    /**
     * Replaces the mixins of the shape.
     *
     * @param mixins Mixins to add.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B mixins(Collection<? extends Shape> mixins) {
        for (Shape shape : mixins) {
            addMixin(shape);
        }
        return (B) this;
    }

    /**
     * Removes a mixin from the shape by shape or ID.
     *
     * @param shape Shape or shape ID to remove.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B removeMixin(ToShapeId shape) {
        if (mixins != null) {
            mixins.remove(shape.toShapeId());
        }

        return (B) this;
    }

    /**
     * Removes all mixins.
     *
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B clearMixins() {
        if (mixins != null) {
            // Avoid concurrent modification.
            List<ShapeId> mixinIds = new ArrayList<>(mixins.keySet());
            for (ShapeId id : mixinIds) {
                removeMixin(id);
            }
        }
        return (B) this;
    }

    /**
     * Removes mixins from a shape and flattens them into the shape.
     *
     * <p>Flattening a mixin into a shape copies the traits and members of a
     * mixin onto the shape, effectively resulting in the same shape but with
     * no trace of the mixin relationship.
     *
     * @return Returns the updated builder.
     */
    @SuppressWarnings("unchecked")
    public B flattenMixins() {
        if (mixins != null) {
            for (Shape mixin : mixins.values()) {
                // Only inherit non-local traits that aren't already on the shape.
                Map<ShapeId, Trait> nonLocalTraits = MixinTrait.getNonLocalTraitsFromMap(mixin.getAllTraits());
                for (Map.Entry<ShapeId, Trait> entry : nonLocalTraits.entrySet()) {
                    if (traits == null || !traits.containsKey(entry.getKey())) {
                        addTrait(entry.getValue());
                    }
                }
            }
            // Don't call clearMixins here because its side effects are unwanted.
            mixins.clear();
        }

        return (B) this;
    }

    Map<ShapeId, Shape> getMixins() {
        return mixins == null ? Collections.emptyMap() : mixins;
    }

    Map<ShapeId, Trait> getTraits() {
        return traits == null ? Collections.emptyMap() : traits;
    }
}
