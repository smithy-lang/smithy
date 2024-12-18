/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.BuilderRef;

public final class IntEnumShape extends IntegerShape {

    private final Map<String, MemberShape> members;
    private volatile Map<String, Integer> enumValues;

    private IntEnumShape(Builder builder) {
        super(builder);
        members = NamedMemberUtils.computeMixinMembers(
                builder.getMixins(),
                builder.members,
                getId(),
                getSourceLocation());
        validateMemberShapeIds();
        if (members.size() < 1) {
            throw new SourceException("intEnum shapes must have at least one member", getSourceLocation());
        }
    }

    /**
     * Gets a map of enum member names to their corresponding values.
     *
     * @return A map of member names to enum values.
     */
    public Map<String, Integer> getEnumValues() {
        if (enumValues == null) {
            Map<String, Integer> values = new LinkedHashMap<>(members.size());
            for (MemberShape member : members()) {
                values.put(member.getMemberName(), member.expectTrait(EnumValueTrait.class).expectIntValue());
            }
            enumValues = Collections.unmodifiableMap(values);
        }
        return enumValues;
    }

    /**
     * Gets the members of the shape, including mixin members.
     *
     * @return Returns the immutable member map.
     */
    @Override
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return (Builder) updateBuilder(builder());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.intEnumShape(this);
    }

    @Override
    public Optional<IntEnumShape> asIntEnumShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.INT_ENUM;
    }

    /**
     * Builder used to create a {@link IntegerShape}.
     */
    public static class Builder extends IntegerShape.Builder {
        private final BuilderRef<Map<String, MemberShape>> members = BuilderRef.forOrderedMap();

        @Override
        public IntEnumShape build() {
            return new IntEnumShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.INT_ENUM;
        }

        @Override
        public Builder id(ShapeId shapeId) {
            super.id(shapeId);
            for (MemberShape member : members.peek().values()) {
                addMember(member.toBuilder().id(shapeId.withMember(member.getMemberName())).build());
            }
            return this;
        }

        @Override
        public Builder id(String shapeId) {
            return id(ShapeId.from(shapeId));
        }

        /**
         * Replaces the members of the builder.
         *
         * @param members Members to add to the builder.
         * @return Returns the builder.
         */
        public Builder members(Collection<MemberShape> members) {
            clearMembers();
            for (MemberShape member : members) {
                addMember(member);
            }
            return this;
        }

        @Override
        public Builder clearMembers() {
            members.clear();
            return this;
        }

        @Override
        public Builder addMember(MemberShape member) {
            if (!member.getTarget().equals(UnitTypeTrait.UNIT)) {
                throw new SourceException(String.format(
                        "intEnum members may only target `smithy.api#Unit`, but found `%s`",
                        member.getTarget()), getSourceLocation());
            }
            members.get().put(member.getMemberName(), member);

            return this;
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param enumValue The value of the enum.
         * @return Returns the builder.
         */
        public Builder addMember(String memberName, int enumValue) {
            return addMember(memberName, enumValue, null);
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param enumValue The value of the enum.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public Builder addMember(String memberName, int enumValue, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(UnitTypeTrait.UNIT)
                    .id(getId().withMember(memberName))
                    .addTrait(EnumValueTrait.builder().intValue(enumValue).build());

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return addMember(builder.build());
        }

        /**
         * Removes a member by name.
         *
         * <p>Note that removing a member that was added by a mixin results in
         * an inconsistent model. It's best to use ModelTransform to ensure
         * that the model remains consistent when removing members.
         *
         * @param member Member name to remove.
         * @return Returns the builder.
         */
        public Builder removeMember(String member) {
            if (members.hasValue()) {
                members.get().remove(member);
            }
            return this;
        }

        @Override
        public Builder addMixin(Shape shape) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before adding a mixin");
            }
            super.addMixin(shape);
            NamedMemberUtils.cleanMixins(shape, members.get());
            return this;
        }

        @Override
        public Builder removeMixin(ToShapeId shape) {
            super.removeMixin(shape);
            NamedMemberUtils.removeMixin(shape, members.get());
            return this;
        }

        @Override
        public Builder flattenMixins() {
            if (getMixins().isEmpty()) {
                return this;
            }
            members(NamedMemberUtils.flattenMixins(members.get(), getMixins(), getId(), getSourceLocation()));
            return (Builder) super.flattenMixins();
        }
    }
}
