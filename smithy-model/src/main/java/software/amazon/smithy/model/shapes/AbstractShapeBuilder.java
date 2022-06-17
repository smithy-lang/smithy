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

package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.Map;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Abstract builder used to create {@link Shape}s.
 *
 * @param <B> Concrete builder type.
 * @param <S> Shape being created.
 */
public abstract class AbstractShapeBuilder<B extends AbstractShapeBuilder<?, ?>, S extends Shape>
        implements SmithyBuilder<S>, FromSourceLocation {

    private ShapeId id;
    private final BuilderRef<Map<ShapeId, Trait>> traits = BuilderRef.forUnorderedMap();
    private SourceLocation source = SourceLocation.none();

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
    @SuppressWarnings("unchecked")
    public B id(String shapeId) {
        return id(ShapeId.from(shapeId));
    }

    /**
     * Sets the source location of the shape.
     *
     * @param sourceLocation Source location to set.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B source(SourceLocation sourceLocation) {
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
    public B source(String filename, int line, int column) {
        return source(new SourceLocation(filename, line, column));
    }

    /**
     * Replace all traits in the builder.
     *
     * @param traitsToSet Sequence of traits to set on the builder.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B traits(Collection<Trait> traitsToSet) {
        clearTraits();
        traitsToSet.forEach(this::addTrait);
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
    public B addTraits(Collection<Trait> traitsToAdd) {
        traitsToAdd.forEach(this::addTrait);
        return (B) this;
    }

    /**
     * Adds a trait to the shape builder, replacing any conflicting traits.
     *
     * @param trait Trait instance to add.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B addTrait(Trait trait) {
        if (trait == null) {
            throw new IllegalArgumentException("trait must not be null");
        }

        traits.get().put(trait.toShapeId(), trait);
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
    public B removeTrait(String traitId) {
        return removeTrait(ShapeId.from(Trait.makeAbsoluteName(traitId)));
    }

    /**
     * Removes a trait from the shape builder.
     *
     * @param traitId ID of the trait to remove.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B removeTrait(ShapeId traitId) {
        if (traits.hasValue()) {
            traits.get().remove(traitId);
        }
        return (B) this;
    }

    /**
     * Removes all traits.
     *
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    public B clearTraits() {
        traits.clear();
        return (B) this;
    }

    /**
     * Configures the builder with the properties of the given shape.
     *
     * <p>This should be overridden in subclasses when the shape being
     * build has more properties than those captured in the method below.
     *
     * @param shape Shape to extract values and populate the builder with.
     * @return Returns the builder.
     */
    @SuppressWarnings("unchecked")
    final B from(S shape) {
        return (B) id(shape.getId())
                .source(shape.getSourceLocation())
                .addTraits(shape.getAllTraits().values());
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

    Map<ShapeId, Trait> copyTraits() {
        return traits.copy();
    }
}
