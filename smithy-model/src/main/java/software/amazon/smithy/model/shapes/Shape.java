/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
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
    private final Map<ShapeId, Trait> introducedTraits;
    private final Map<ShapeId, Shape> mixins;
    private final transient SourceLocation source;
    private transient List<String> memberNames;
    private int hash;

    /**
     * This class is package-private, which means that all subclasses of this
     * class must reside within the same package. Because of this, Shape is a
     * closed set of known concrete shape types.
     *
     * @param builder Builder to extract values from.
     * @param expectMemberSegments True/false if the ID must have a member.
     */
    Shape(AbstractShapeBuilder<?, ?> builder, boolean expectMemberSegments) {
        source = builder.getSourceLocation();
        id = SmithyBuilder.requiredState("id", builder.getId());
        validateShapeId(expectMemberSegments);

        introducedTraits = MapUtils.copyOf(builder.getTraits());
        mixins = MapUtils.orderedCopyOf(builder.getMixins());

        if (mixins.isEmpty()) {
            // Simple case when there are no mixins.
            traits = introducedTraits;
        } else {
            validateMixins(mixins, introducedTraits);
            // Compute mixin traits.
            Map<ShapeId, Trait> computedTraits = new HashMap<>();
            for (Shape shape : mixins.values()) {
                // Mixin traits override other mixin traits, in order.
                computedTraits.putAll(MixinTrait.getNonLocalTraitsFromMap(shape.getAllTraits()));
            }
            // Traits applied to the shape directly override inherited traits.
            computedTraits.putAll(introducedTraits);
            traits = Collections.unmodifiableMap(computedTraits);
        }
    }

    protected void validateMixins(Map<ShapeId, Shape> mixins, Map<ShapeId, Trait> introducedTraits) {
        Set<String> invalid = new TreeSet<>();
        for (Shape mixin : mixins.values()) {
            if (mixin.getType() != getType()) {
                invalid.add(mixin.getId().toString());
            }
        }
        if (!invalid.isEmpty()) {
            String invalidList = String.join("`, `", invalid);
            throw new SourceException(String.format(
                    "Mixins may only be mixed into shapes of the same type. The following mixins were applied to the "
                            + "%s shape `%s` which are not %1$s shapes: [`%s`]",
                    getType(),
                    getId(),
                    invalidList),
                    source);
        }
    }

    protected MemberShape[] getRequiredMembers(AbstractShapeBuilder<?, ?> builder, String... requiredMembersNames) {
        // Caller knows the order of provided member names, so we don't need a dynamic data structure.
        MemberShape[] members = new MemberShape[requiredMembersNames.length];
        int missingMemberCount = 0;

        for (int memberNameIndex = 0; memberNameIndex < requiredMembersNames.length; memberNameIndex++) {
            String requiredMemberName = requiredMembersNames[memberNameIndex];
            MemberShape member = getRequiredMixinMember(builder, requiredMemberName);
            if (member != null) {
                members[memberNameIndex] = member;
            } else {
                missingMemberCount++;
            }
        }

        if (missingMemberCount > 0) {
            List<String> missingMembers = new ArrayList<>();
            for (int memberIndex = 0; memberIndex < members.length; memberIndex++) {
                if (members[memberIndex] == null) {
                    missingMembers.add(requiredMembersNames[memberIndex]);
                }
            }
            throw missingRequiredMembersException(missingMembers);
        }

        return members;
    }

    private MemberShape getRequiredMixinMember(AbstractShapeBuilder<?, ?> builder, String requiredMemberName) {
        Optional<MemberShape> memberOnBuilder = builder.getMember(requiredMemberName);
        if (memberOnBuilder.isPresent()) {
            return memberOnBuilder.get();
        }

        // Get the most recently introduced mixin member with the given name.
        MemberShape mostRecentMember = null;

        for (Shape shape : builder.getMixins().values()) {
            for (MemberShape member : shape.members()) {
                if (member.getMemberName().equals(requiredMemberName)) {
                    mostRecentMember = member;
                    break; // break to the next mixin shape.
                }
            }
        }

        if (mostRecentMember == null) {
            return null;
        }

        return MemberShape.builder()
                .id(getId().withMember(requiredMemberName))
                .target(mostRecentMember.getTarget())
                .source(getSourceLocation())
                .addMixin(mostRecentMember)
                .build();
    }

    private SourceException missingRequiredMembersException(List<String> missingMembersNames) {
        String missingRequired = missingMembersNames.size() > 1 ? "members" : "member";
        String missingMembers = String.join(", ", missingMembersNames);
        String message = String.format("Missing required %s of shape `%s`: %s",
                missingRequired,
                getId(),
                missingMembers);
        return new SourceException(message, getSourceLocation());
    }

    /**
     * Validates that a shape ID has or does not have a member.
     *
     * @param expectMember Whether or not a member is expected.
     */
    private void validateShapeId(boolean expectMember) {
        if (expectMember) {
            if (!getId().hasMember()) {
                throw new SourceException(String.format(
                        "Shapes of type `%s` must contain a member in their shape ID. Found `%s`",
                        getType(),
                        getId()), getSourceLocation());
            }
        } else if (getId().hasMember()) {
            throw new SourceException(String.format(
                    "Shapes of type `%s` cannot contain a member in their shape ID. Found `%s`",
                    getType(),
                    getId()), getSourceLocation());
        }
    }

    protected final void validateMemberShapeIds() {
        for (MemberShape member : members()) {
            if (!member.getId().toString().startsWith(getId().toString())) {
                ShapeId expected = getId().withMember(member.getMemberName());
                throw new SourceException(String.format(
                        "Expected the `%s` member of `%s` to have an ID of `%s` but found `%s`",
                        member.getMemberName(),
                        getId(),
                        expected,
                        member.getId()), getSourceLocation());
            }
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
                "Expected shape `%s` to have a trait `%s`",
                getId(),
                traitClass.getCanonicalName()), this));
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
     * @return Optionally returns the shape as a {@link IntEnumShape}.
     */
    public Optional<IntEnumShape> asIntEnumShape() {
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
     * @return Optionally returns the shape as a {@link EnumShape}.
     */
    public Optional<EnumShape> asEnumShape() {
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
        return getType() == ShapeType.INTEGER || getType() == ShapeType.INT_ENUM;
    }

    /**
     * @return Returns true if the shape is a {@link IntEnumShape} shape.
     */
    public final boolean isIntEnumShape() {
        return getType() == ShapeType.INT_ENUM;
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
        return getType() == ShapeType.STRING || getType() == ShapeType.ENUM;
    }

    /**
     * @return Returns true if the shape is an {@link EnumShape} shape.
     */
    public final boolean isEnumShape() {
        return getType() == ShapeType.ENUM;
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
     * Gets all the members contained in the shape.
     *
     * @return Returns the members contained in the shape (if any).
     */
    public Collection<MemberShape> members() {
        return getAllMembers().values();
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
        return Optional.ofNullable(getAllMembers().get(name));
    }

    /**
     * Gets the members of the shape, including mixin members.
     *
     * @return Returns the immutable member map.
     */
    public Map<String, MemberShape> getAllMembers() {
        return Collections.emptyMap();
    }

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model, including mixin members.
     *
     * <p>The order in which map key and value members are returned might
     * not match the order in which they were defined in the model because
     * their ordering is insignificant.
     *
     * @return Returns an immutable list of member names.
     */
    public List<String> getMemberNames() {
        List<String> result = memberNames;
        if (result == null) {
            result = ListUtils.copyOf(getAllMembers().keySet());
            memberNames = result;
        }
        return result;
    }

    /**
     * Get an ordered set of mixins attached to the shape.
     *
     * @return Returns the ordered mixin shape IDs.
     */
    public Set<ShapeId> getMixins() {
        return mixins.keySet();
    }

    /**
     * Gets the traits introduced by the shape and not inherited
     * from mixins.
     *
     * @return Returns the introduced traits.
     */
    public Map<ShapeId, Trait> getIntroducedTraits() {
        return introducedTraits;
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
        int h = hash;

        if (h == 0) {
            h = Objects.hash(getType(), getId());
            hash = h;
        }

        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Shape)) {
            return false;
        } else if (hashCode() != o.hashCode()) {
            return false; // take advantage of hashcode caching
        }

        Shape other = (Shape) o;
        return getType() == other.getType()
                && getId().equals(other.getId())
                && getMemberNames().equals(other.getMemberNames())
                && getAllMembers().equals(other.getAllMembers())
                && getAllTraits().equals(other.getAllTraits())
                && mixins.equals(other.mixins);
    }

    /**
     * Copies the ID, source location, all traits, and mixins of the shape to the builder.
     *
     * @param builder Builder to update.
     * @param <S> Type of shape being built.
     * @param <B> Type of builder being built.
     * @return Returns the builder.
     */
    <S extends Shape, B extends AbstractShapeBuilder<B, S>> B updateBuilder(B builder) {
        builder.id(getId());
        builder.source(getSourceLocation());
        // Only add introduced traits to the builder to allow model load -> rebuild -> serialize roundtripping.
        builder.addTraits(getIntroducedTraits().values());
        builder.mixins(mixins.values());

        // Add members to the builder that are not just strictly inherited from mixins.
        for (MemberShape member : members()) {
            if (member.getMixins().isEmpty() || !member.getIntroducedTraits().isEmpty()) {
                builder.addMember(member);
            }
        }

        return builder;
    }
}
