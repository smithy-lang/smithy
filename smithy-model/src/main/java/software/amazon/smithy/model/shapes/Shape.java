/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;

/**
 * A {@code Shape} defines a model component.
 *
 * <p>A {@code Shape} may have an arbitrary number of typed traits
 * attached to it, allowing additional information to be associated
 * with the shape.
 *
 * <p>Shape does implement {@link Comparable}, but comparisons are based
 * solely on the ShapeId of the shape. This assumes that shapes are being
 * compared in the context of a Model that forbids shape ID conflicts.
 */
public abstract class Shape implements FromSourceLocation, Tagged, ToShapeId, Comparable<Shape> {

    private final ShapeId id;
    private final Map<ShapeId, Trait> traits;
    private final transient SourceLocation source;

    /**
     * This class is package-private, which means that all subclasses of this
     * class must reside within the same package. Because of this, Shape is a
     * closed set of known concrete shape types.
     *
     * @param builder Builder to extract values from.
     * @param expectMemberSegments True/false if the ID must have a member.
     */
    @SuppressWarnings("unchecked")
    Shape(AbstractShapeBuilder builder, boolean expectMemberSegments) {
        source = builder.getSourceLocation();
        id = SmithyBuilder.requiredState("id", builder.getId());
        traits = builder.copyTraits();
        validateShapeId(expectMemberSegments);
    }

    /**
     * Validates that a shape ID has or does not have a member.
     *
     * @param expectMember Whether or not a member is expected.
     */
    private void validateShapeId(boolean expectMember) {
        if (expectMember) {
            if (!getId().getMember().isPresent()) {
                throw new SourceException(String.format(
                        "Shapes of type `%s` must contain a member in their shape ID. Found `%s`",
                        getType(), getId()), getSourceLocation());
            }
        } else if (getId().getMember().isPresent()) {
            throw new SourceException(String.format(
                    "Shapes of type `%s` cannot contain a member in their shape ID. Found `%s`",
                    getType(), getId()), getSourceLocation());
        }
    }

    /**
     * Converts a shape, potentially of an unknown concrete type, into a
     * Shape builder.
     *
     * @param shape Shape to create a builder from.
     * @param <B> Shape builder to create.
     * @param <S> Shape that is being converted to a builder.
     * @return Returns a shape fro the given shape.
     */
    @SuppressWarnings("unchecked")
    public static <B extends AbstractShapeBuilder<B, S>, S extends Shape> B shapeToBuilder(S shape) {
        return (B) shape.accept(new ShapeToBuilder());
    }

    /**
     * Gets the type of the shape.
     *
     * @return Returns the type;
     */
    public abstract ShapeType getType();

    /**
     * Dispatches the shape to the appropriate {@link ShapeVisitor} method.
     *
     * @param <R> Return type of the accept.
     * @param visitor ShapeVisitor to use.
     * @return Returns the result.
     */
    public abstract <R> R accept(ShapeVisitor<R> visitor);

    /**
     * Get the {@link ShapeId} of the shape.
     *
     * @return Returns the shape ID.
     */
    public final ShapeId getId() {
        return id;
    }

    /**
     * Checks if the shape has a specific trait by name.
     *
     * <p>Relative shape IDs are assumed to refer to the "smithy.api"
     * namespace.
     *
     * @param id The possibly relative trait ID.
     * @return Returns true if the shape has the given trait.
     */
    public boolean hasTrait(String id) {
        return findTrait(id).isPresent();
    }

    /**
     * Checks if the shape has a specific trait by name.
     *
     * @param id The fully-qualified trait ID.
     * @return Returns true if the shape has the given trait.
     */
    public boolean hasTrait(ShapeId id) {
        return findTrait(id).isPresent();
    }

    /**
     * Checks if the shape has a specific trait by class.
     *
     * @param traitClass Trait class to check.
     * @return Returns true if the shape has the given trait.
     */
    public boolean hasTrait(Class<? extends Trait> traitClass) {
        return getTrait(traitClass).isPresent();
    }

    /**
     * Attempts to find a trait applied to the shape by name.
     *
     * @param id The trait shape ID.
     * @return Returns the optionally found trait.
     */
    public Optional<Trait> findTrait(ShapeId id) {
        return Optional.ofNullable(traits.get(id));
    }

    /**
     * Attempts to find a trait applied to the shape by ID.
     *
     * <p>Relative shape IDs are assumed to refer to the "smithy.api"
     * namespace.
     *
     * @param id The trait ID.
     * @return Returns the optionally found trait.
     */
    public Optional<Trait> findTrait(String id) {
        return findTrait(ShapeId.from(Trait.makeAbsoluteName(id)));
    }

    /**
     * Attempt to retrieve a specific {@link Trait} by class from the shape.
     *
     * <p>The first trait instance found matching the given type is returned.
     *
     * @param traitClass Trait class to retrieve.
     * @param <T> The instance of the trait to retrieve.
     * @return Returns the matching trait.
     */
    @SuppressWarnings("unchecked")
    public final <T extends Trait> Optional<T> getTrait(Class<T> traitClass) {
        for (Trait trait : traits.values()) {
            if (traitClass.isInstance(trait)) {
                return Optional.of((T) trait);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets specific {@link Trait} by class from the shape or throws if not found.
     *
     * @param traitClass Trait class to retrieve.
     * @param <T> The instance of the trait to retrieve.
     * @return Returns the matching trait.
     * @throws ExpectationNotMetException if the trait cannot be found.
     */
    public final <T extends Trait> T expectTrait(Class<T> traitClass) {
        return getTrait(traitClass).orElseThrow(() -> new ExpectationNotMetException(String.format(
                "Expected shape `%s` to have a trait `%s`", getId(), traitClass.getCanonicalName()), this));
    }

    /**
     * Gets all of the traits attached to the shape.
     *
     * @return Returns the attached traits.
     */
    public final Map<ShapeId, Trait> getAllTraits() {
        return traits;
    }

    /**
     * Gets a trait from the member shape or from the shape targeted by the
     * member.
     *
     * <p>If the shape is not a member, then the method functions the same as
     * {@link #getTrait(Class)}.
     *
     * @param model Model used to find member targets.
     * @param trait Trait type to get.
     * @param <T> Trait type to get.
     * @return Returns the optionally found trait on the shape or member.
     * @see MemberShape#getTrait(Class)
     */
    public <T extends Trait> Optional<T> getMemberTrait(Model model, Class<T> trait) {
        return getTrait(trait);
    }

    /**
     * Gets a trait from the member shape or from the shape targeted by the
     * member.
     *
     * <p>If the shape is not a member, then the method functions the same as
     * {@link #findTrait(String)}.
     *
     * @param model Model used to find member targets.
     * @param traitName Trait name to get.
     * @return Returns the optionally found trait on the shape or member.
     * @see MemberShape#findTrait(String)
     */
    public Optional<Trait> findMemberTrait(Model model, String traitName) {
        return findTrait(traitName);
    }

    /**
     * @return Optionally returns the shape as a {@link BigDecimalShape}.
     */
    public Optional<BigDecimalShape> asBigDecimalShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link BigIntegerShape}.
     */
    public Optional<BigIntegerShape> asBigIntegerShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link BlobShape}.
     */
    public Optional<BlobShape> asBlobShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link BooleanShape}.
     */
    public Optional<BooleanShape> asBooleanShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link ByteShape}.
     */
    public Optional<ByteShape> asByteShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link ShortShape}.
     */
    public Optional<ShortShape> asShortShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link FloatShape}.
     */
    public Optional<FloatShape> asFloatShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link DocumentShape}.
     */
    public Optional<DocumentShape> asDocumentShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link DoubleShape}.
     */
    public Optional<DoubleShape> asDoubleShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link IntegerShape}.
     */
    public Optional<IntegerShape> asIntegerShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link ListShape}.
     */
    public Optional<ListShape> asListShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link SetShape}.
     */
    @Deprecated
    public Optional<SetShape> asSetShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link LongShape}.
     */
    public Optional<LongShape> asLongShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link MapShape}.
     */
    public Optional<MapShape> asMapShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link MemberShape}.
     */
    public Optional<MemberShape> asMemberShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as an {@link OperationShape}.
     */
    public Optional<OperationShape> asOperationShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link ResourceShape}.
     */
    public Optional<ResourceShape> asResourceShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link ServiceShape}.
     */
    public Optional<ServiceShape> asServiceShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link StringShape}.
     */
    public Optional<StringShape> asStringShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link StructureShape}.
     */
    public Optional<StructureShape> asStructureShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link UnionShape}.
     */
    public Optional<UnionShape> asUnionShape() {
        return Optional.empty();
    }

    /**
     * @return Optionally returns the shape as a {@link TimestampShape}.
     */
    public Optional<TimestampShape> asTimestampShape() {
        return Optional.empty();
    }

    /**
     * @return Returns true if the shape is a {@link BigDecimalShape} shape.
     */
    public final boolean isBigDecimalShape() {
        return getType() == ShapeType.BIG_DECIMAL;
    }

    /**
     * @return Returns true if the shape is a {@link BigIntegerShape} shape.
     */
    public final boolean isBigIntegerShape() {
        return getType() == ShapeType.BIG_INTEGER;
    }

    /**
     * @return Returns true if the shape is a {@link BlobShape} shape.
     */
    public final boolean isBlobShape() {
        return getType() == ShapeType.BLOB;
    }

    /**
     * @return Returns true if the shape is a {@link BooleanShape} shape.
     */
    public final boolean isBooleanShape() {
        return getType() == ShapeType.BOOLEAN;
    }

    /**
     * @return Returns true if the shape is a {@link ByteShape} shape.
     */
    public final boolean isByteShape() {
        return getType() == ShapeType.BYTE;
    }

    /**
     * @return Returns true if the shape is a {@link ShortShape} shape.
     */
    public final boolean isShortShape() {
        return getType() == ShapeType.SHORT;
    }

    /**
     * @return Returns true if the shape is a {@link FloatShape} shape.
     */
    public final boolean isFloatShape() {
        return getType() == ShapeType.FLOAT;
    }

    /**
     * @return Returns true if the shape is an {@link DocumentShape} shape.
     */
    public final boolean isDocumentShape() {
        return getType() == ShapeType.DOCUMENT;
    }

    /**
     * @return Returns true if the shape is an {@link DoubleShape} shape.
     */
    public final boolean isDoubleShape() {
        return getType() == ShapeType.DOUBLE;
    }

    /**
     * @return Returns true if the shape is a {@link ListShape} shape.
     */
    public final boolean isListShape() {
        return getType() == ShapeType.LIST;
    }

    /**
     * @return Returns true if the shape is a {@link SetShape} shape.
     */
    @Deprecated
    public final boolean isSetShape() {
        return getType() == ShapeType.SET;
    }

    /**
     * @return Returns true if the shape is a {@link IntegerShape} shape.
     */
    public final boolean isIntegerShape() {
        return getType() == ShapeType.INTEGER;
    }

    /**
     * @return Returns true if the shape is a {@link LongShape} shape.
     */
    public final boolean isLongShape() {
        return getType() == ShapeType.LONG;
    }

    /**
     * @return Returns true if the shape is a {@link MapShape} shape.
     */
    public final boolean isMapShape() {
        return getType() == ShapeType.MAP;
    }

    /**
     * @return Returns true if the shape is a {@link MemberShape} shape.
     */
    public final boolean isMemberShape() {
        return getType() == ShapeType.MEMBER;
    }

    /**
     * @return Returns true if the shape is an {@link OperationShape} shape.
     */
    public final boolean isOperationShape() {
        return getType() == ShapeType.OPERATION;
    }

    /**
     * @return Returns true if the shape is a {@link ResourceShape} shape.
     */
    public final boolean isResourceShape() {
        return getType() == ShapeType.RESOURCE;
    }

    /**
     * @return Returns true if the shape is a {@link ServiceShape} shape.
     */
    public final boolean isServiceShape() {
        return getType() == ShapeType.SERVICE;
    }

    /**
     * @return Returns true if the shape is a {@link StringShape} shape.
     */
    public final boolean isStringShape() {
        return getType() == ShapeType.STRING;
    }

    /**
     * @return Returns true if the shape is a {@link StructureShape} shape.
     */
    public final boolean isStructureShape() {
        return getType() == ShapeType.STRUCTURE;
    }

    /**
     * @return Returns true if the shape is a {@link UnionShape} shape.
     */
    public final boolean isUnionShape() {
        return getType() == ShapeType.UNION;
    }

    /**
     * @return Returns true if the shape is a {@link TimestampShape} shape.
     */
    public final boolean isTimestampShape() {
        return getType() == ShapeType.TIMESTAMP;
    }

    /**
     * Gets all of the members contained in the shape.
     *
     * @return Returns the members contained in the shape (if any).
     */
    public Collection<MemberShape> members() {
        return Collections.emptyList();
    }

    /**
     * Get a specific member by name.
     *
     * <p>Shapes with no members return an empty Optional.
     *
     * @param name Name of the member to retrieve.
     * @return Returns the optional member.
     */
    public Optional<MemberShape> getMember(String name) {
        return Optional.empty();
    }

    @Override
    public ShapeId toShapeId() {
        return id;
    }

    @Override
    public final List<String> getTags() {
        return getTrait(TagsTrait.class).map(TagsTrait::getValues).orElseGet(Collections::emptyList);
    }

    @Override
    public final SourceLocation getSourceLocation() {
        return source;
    }

    @Override
    public int compareTo(Shape other) {
        return getId().compareTo(other.getId());
    }

    @Override
    public final String toString() {
        return "(" + getType() + ": `" + getId() + "`)";
    }

    @Override
    public int hashCode() {
        return getId().hashCode() + 3 * getType().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Shape)) {
            return false;
        }

        Shape other = (Shape) o;
        return getId().equals(other.getId())
               && getType() == other.getType()
               && traits.equals(other.traits);
    }
}
