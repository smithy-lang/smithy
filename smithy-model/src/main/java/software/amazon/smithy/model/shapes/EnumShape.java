/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.EnumDefaultTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;

public final class EnumShape extends StringShape implements NamedMembers {

    private static final Logger LOGGER = Logger.getLogger(EnumShape.class.getName());

    private final Map<String, MemberShape> members;
    private volatile List<String> memberNames;
    private volatile Map<String, String> enumValues;

    private EnumShape(Builder builder) {
        super(builder);
        members = NamedMemberUtils.computeMixinMembers(
                builder.getMixins(), builder.members, getId(), getSourceLocation());
        validateMemberShapeIds();
        if (members.size() < 1) {
            throw new SourceException("enum shapes must have at least one member", getSourceLocation());
        }
    }

    /**
     * Gets a map of enum member names to their corresponding values.
     *
     * @return A map of member names to enum values.
     */
    public Map<String, String> getEnumValues() {
        if (enumValues == null) {
            Map<String, String> values = new LinkedHashMap<>(members.size());
            for (MemberShape member : members()) {
                if (member.hasTrait(EnumDefaultTrait.ID)) {
                    values.put(member.getMemberName(), "");
                    continue;
                }
                values.put(member.getMemberName(), member.expectTrait(EnumValueTrait.class).expectStringValue());
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

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model, including mixin members.
     *
     * @return Returns an immutable list of member names.
     */
    @Override
    public List<String> getMemberNames() {
        List<String> names = memberNames;
        if (names == null) {
            names = ListUtils.copyOf(members.keySet());
            memberNames = names;
        }

        return names;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        // Members are ordered, so do a test on the ordering and their values.
        EnumShape b = (EnumShape) other;
        return getMemberNames().equals(b.getMemberNames()) && members.equals(b.members);
    }

    @Override
    public Optional<Trait> findTrait(ShapeId id) {
        if (id.equals(EnumTrait.ID)) {
            return super.findTrait(SyntheticEnumTrait.ID);
        }
        return super.findTrait(id);
    }

    /**
     * Get a specific member by name.
     *
     * @param name Name of the member to retrieve.
     * @return Returns the optional member.
     */
    @Override
    public Optional<MemberShape> getMember(String name) {
        return Optional.ofNullable(members.get(name));
    }

    @Override
    public Collection<MemberShape> members() {
        return members.values();
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
        return cases.enumShape(this);
    }

    @Override
    public Optional<EnumShape> asEnumShape() {
        return Optional.of(this);
    }

    /**
     * Converts a base {@link StringShape} to an {@link EnumShape} if possible.
     *
     * The result will be empty if the given shape doesn't have the {@link EnumTrait},
     * if the enum doesn't have names and name synthesization is disabled, or if a name
     * cannot be synthesized.
     *
     * @param shape A base {@link StringShape} to convert.
     * @param synthesizeNames Whether names should be synthesized if possible.
     * @return Optionally returns an {@link EnumShape} equivalent of the given shape.
     */
    public static Optional<EnumShape> fromStringShape(StringShape shape, boolean synthesizeNames) {
        if (!shape.hasTrait(EnumTrait.ID)) {
            return Optional.empty();
        }
        StringShape stringWithoutEnumTrait = shape.toBuilder().removeTrait(EnumTrait.ID).build();
        Builder enumBuilder = EnumShape.builder();
        stringWithoutEnumTrait.updateBuilder(enumBuilder);
        try {
            return Optional.of(enumBuilder
                    .setMembersFromEnumTrait(shape.expectTrait(EnumTrait.class), synthesizeNames)
                    .build());
        } catch (IllegalStateException e) {
            LOGGER.info(String.format("Unable to convert `%s` to an enum: %s", shape.getId(), e));
            return Optional.empty();
        }
    }

    /**
     * Converts a base {@link StringShape} to an {@link EnumShape} if possible.
     *
     * The result will be empty if the given shape doesn't have the {@link EnumTrait}
     * or if the enum definitions don't have names.
     *
     * @param shape A base {@link StringShape} to convert.
     * @return Optionally returns an {@link EnumShape} equivalent of the given shape.
     */
    public static Optional<EnumShape> fromStringShape(StringShape shape) {
        return fromStringShape(shape, false);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.ENUM;
    }

    public static final class Builder extends StringShape.Builder {
        private final BuilderRef<Map<String, MemberShape>> members = BuilderRef.forOrderedMap();

        @Override
        public EnumShape build() {
            addSyntheticEnumTrait();
            return new EnumShape(this);
        }

        /**
         * Adds a synthetic version of the enum trait.
         *
         * <p>This allows the enum shape to be used as if it were a string shape with
         * the enum trait, without having to manually add the trait or risk that it
         * gets serialized.
         */
        private void addSyntheticEnumTrait() {
            SyntheticEnumTrait.Builder builder = SyntheticEnumTrait.builder();
            for (MemberShape member : members.get().values()) {
                try {
                    builder.addEnum(EnumDefinition.fromMember(member));
                } catch (IllegalStateException e) {
                    // This can happen if the enum value trait is using something other
                    // than a string value. Rather than letting the exception propagate
                    // here, we let the shape validator handle it because it will give
                    // a much better error.
                    return;
                }
            }
            addTrait(builder.build());
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.ENUM;
        }

        @Override
        public Builder id(ShapeId shapeId) {
            super.id(shapeId);
            for (MemberShape member : members.peek().values()) {
                addMember(member.toBuilder().id(shapeId.withMember(member.getMemberName())).build());
            }
            return this;
        }

        /**
         * Sets enum members from an {@link EnumTrait}.
         *
         * <p>This is primarily useful when converting from string shapes to enums.
         *
         * @param trait The {@link EnumTrait} whose values should be converted to members.
         * @param synthesizeNames Whether to synthesize names if they aren't present in the enum trait.
         * @return Returns the builder.
         */
        public Builder setMembersFromEnumTrait(EnumTrait trait, boolean synthesizeNames) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before adding a named enum trait to a string.");
            }
            clearMembers();

            for (EnumDefinition definition : trait.getValues()) {
                Optional<MemberShape> member = definition.asMember(getId(), synthesizeNames);
                if (member.isPresent()) {
                    addMember(member.get());
                } else {
                    throw new IllegalStateException(String.format(
                            "Unable to convert enum trait entry with name: `%s` and value `%s` to an enum member.",
                            definition.getName().orElse(""), definition.getValue()
                    ));
                }
            }

            return this;
        }

        /**
         * Sets enum members from an {@link EnumTrait}.
         *
         * <p>This is primarily useful when converting from string shapes to enums.
         *
         * @param trait The {@link EnumTrait} whose values should be converted to members.
         * @return Returns the builder.
         */
        public Builder setMembersFromEnumTrait(EnumTrait trait) {
            return setMembersFromEnumTrait(trait, false);
        }

        /**
         * Replaces the members of the builder.
         *
         * @param members Members to add to the builder.
         * @return Returns the builder.
         */
        public Builder setMembersFromEnumTrait(Collection<MemberShape> members) {
            clearMembers();
            for (MemberShape member : members) {
                addMember(member);
            }
            return this;
        }

        /**
         * Removes all members from the shape.
         *
         * @return Returns the builder.
         */
        public Builder clearMembers() {
            members.clear();
            return this;
        }

        /**
         * Adds a member to the shape.
         *
         * <p>If the member does not already have an {@link EnumValueTrait}, one will
         * be generated with the value being equal to the member name.
         *
         * @param member Member to add to the shape.
         * @return Returns the model assembler.
         * @throws UnsupportedOperationException if the shape does not support members.
         */
        @Override
        public Builder addMember(MemberShape member) {
            if (!member.getTarget().equals(UnitTypeTrait.UNIT)) {
                throw new SourceException(String.format(
                        "Enum members may only target `smithy.api#Unit`, but found `%s`", member.getTarget()
                ), getSourceLocation());
            }
            if (!member.hasTrait(EnumValueTrait.ID) && !member.hasTrait(EnumDefaultTrait.ID)) {
                member = member.toBuilder()
                        .addTrait(EnumValueTrait.builder().stringValue(member.getMemberName()).build())
                        .build();
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
        public Builder addMember(String memberName, String enumValue) {
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
        public Builder addMember(String memberName, String enumValue, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(UnitTypeTrait.UNIT)
                    .id(getId().withMember(memberName))
                    .addTrait(EnumValueTrait.builder().stringValue(enumValue).build());

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return addMember(builder.build());
        }

        /**
         * Adds the default member to the builder.
         *
         * @param memberName Member name to add.
         * @return Returns the builder.
         */
        public Builder addDefaultMember(String memberName) {
            return addDefaultMember(memberName, null);
        }

        /**
         * Adds the default member to the builder.
         *
         * @param memberName Member name to add.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public Builder addDefaultMember(String memberName, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(UnitTypeTrait.UNIT)
                    .id(getId().withMember(memberName))
                    .addTrait(new EnumDefaultTrait());

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
            setMembersFromEnumTrait(NamedMemberUtils.flattenMixins(
                    members.get(), getMixins(), getId(), getSourceLocation()));
            return (Builder) super.flattenMixins();
        }
    }
}
