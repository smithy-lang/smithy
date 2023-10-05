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
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.BuilderRef;

public final class EnumShape extends StringShape {

    private static final Pattern CONVERTABLE_VALUE = Pattern.compile("^[a-zA-Z-_.:/\\s]+[a-zA-Z_0-9-.:/\\s]*$");
    private static final Logger LOGGER = Logger.getLogger(EnumShape.class.getName());

    private final Map<String, MemberShape> members;
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

    private EnumShape(Builder builder, Map<String, MemberShape> members) {
        super(builder);
        this.members = members;
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

    @Override
    @SuppressWarnings("deprecation")
    public Optional<Trait> findTrait(ShapeId id) {
        if (id.equals(EnumTrait.ID)) {
            return super.findTrait(SyntheticEnumTrait.ID);
        }
        return super.findTrait(id);
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
        if (shape.isEnumShape()) {
            return Optional.of((EnumShape) shape);
        }

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

    /**
     * Determines whether a given string shape can be converted to an enum shape.
     *
     * @param shape The string shape to be converted.
     * @param synthesizeEnumNames Whether synthesizing enum names should be accounted for.
     * @return Returns true if the string shape can be converted to an enum shape.
     */
    public static boolean canConvertToEnum(StringShape shape, boolean synthesizeEnumNames) {
        if (shape.isEnumShape()) {
            return true;
        }

        if (!shape.hasTrait(EnumTrait.class)) {
            LOGGER.info(String.format(
                    "Unable to convert string shape `%s` to enum shape because it doesn't have an enum trait.",
                    shape.getId()
            ));
            return false;
        }

        EnumTrait trait = shape.expectTrait(EnumTrait.class);
        if (!trait.hasNames() && !synthesizeEnumNames) {
            LOGGER.info(String.format(
                    "Unable to convert string shape `%s` to enum shape because it doesn't define names. The "
                            + "`synthesizeNames` option may be able to synthesize the names for you.",
                    shape.getId()
            ));
            return false;
        }

        for (EnumDefinition definition : trait.getValues()) {
            if (!canConvertEnumDefinitionToMember(definition, synthesizeEnumNames)) {
                LOGGER.info(String.format(
                        "Unable to convert string shape `%s` to enum shape because it has at least one value which "
                                + "cannot be safely synthesized into a name: %s",
                        shape.getId(), definition.getValue()
                ));
                return false;
            }
        }

        return true;
    }

    /**
     * Converts an enum definition to the equivalent enum member shape.
     *
     * <p>If an enum definition is marked as deprecated, the DeprecatedTrait
     * is applied to the converted enum member shape.
     *
     * <p>If an enum definition has an "internal" tag, the InternalTrait is
     * applied to the converted enum member shape.
     *
     * @param parentId The {@link ShapeId} of the enum shape.
     * @param synthesizeName Whether to synthesize a name if possible.
     * @return An optional member shape representing the enum definition,
     *         or empty if conversion is impossible.
     */
    static Optional<MemberShape> memberFromEnumDefinition(
            EnumDefinition definition,
            ShapeId parentId,
            boolean synthesizeName
    ) {
        String name;
        if (!definition.getName().isPresent()) {
            if (canConvertEnumDefinitionToMember(definition, synthesizeName)) {
                name = definition.getValue().replaceAll("[-.:/\\s]+", "_");
            } else {
                return Optional.empty();
            }
        } else {
            name = definition.getName().get();
        }

        try {
            MemberShape.Builder builder = MemberShape.builder()
                    .id(parentId.withMember(name))
                    .target(UnitTypeTrait.UNIT)
                    .addTrait(EnumValueTrait.builder().stringValue(definition.getValue()).build());

            definition.getDocumentation().ifPresent(docs -> builder.addTrait(new DocumentationTrait(docs)));
            if (!definition.getTags().isEmpty()) {
                builder.addTrait(TagsTrait.builder().values(definition.getTags()).build());
            }
            if (definition.isDeprecated()) {
                builder.addTrait(DeprecatedTrait.builder().build());
            }
            if (definition.hasTag("internal")) {
                builder.addTrait(new InternalTrait());
            }

            return Optional.of(builder.build());
        } catch (ShapeIdSyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Determines whether the definition can be converted to a member.
     *
     * @param withSynthesizedNames Whether to account for name synthesization.
     * @return Returns true if the definition can be converted.
     */
    static boolean canConvertEnumDefinitionToMember(EnumDefinition definition, boolean withSynthesizedNames) {
        return definition.getName().isPresent()
                || (withSynthesizedNames && CONVERTABLE_VALUE.matcher(definition.getValue()).find());
    }

    /**
     * Converts an enum member into an equivalent enum definition object.
     *
     * @param member The enum member to convert.
     * @return An {@link EnumDefinition} representing the given member.
     */
    static EnumDefinition enumDefinitionFromMember(MemberShape member) {
        EnumDefinition.Builder builder = EnumDefinition.builder().name(member.getMemberName());

        String traitValue = member
                .getTrait(EnumValueTrait.class)
                .flatMap(EnumValueTrait::getStringValue)
                .orElseThrow(() -> new IllegalStateException("Enum definitions can only be made for string enums."));
        builder.value(traitValue);
        member.getTrait(DocumentationTrait.class).ifPresent(docTrait -> builder.documentation(docTrait.getValue()));
        member.getTrait(TagsTrait.class).ifPresent(tagsTrait -> builder.tags(tagsTrait.getValues()));
        member.getTrait(DeprecatedTrait.class).ifPresent(deprecatedTrait -> builder.deprecated(true));
        return builder.build();
    }

    @Override
    public ShapeType getType() {
        return ShapeType.ENUM;
    }

    public static final class Builder extends StringShape.Builder {
        private final BuilderRef<Map<String, MemberShape>> members = BuilderRef.forOrderedMap();

        @Override
        public EnumShape build() {
            // Collect members from enum and mixins
            Map<String, MemberShape> aggregatedMembers =
                NamedMemberUtils.computeMixinMembers(getMixins(), members, getId(), getSourceLocation());
            addSyntheticEnumTrait(aggregatedMembers.values());
            return new EnumShape(this, aggregatedMembers);
        }

        /**
         * Adds a synthetic version of the enum trait.
         *
         * <p>This allows the enum shape to be used as if it were a string shape with
         * the enum trait, without having to manually add the trait or risk that it
         * gets serialized.
         */
        private void addSyntheticEnumTrait(Collection<MemberShape> memberShapes) {
            SyntheticEnumTrait.Builder builder = SyntheticEnumTrait.builder();
            builder.sourceLocation(getSourceLocation());
            for (MemberShape member : memberShapes) {
                try {
                    builder.addEnum(EnumShape.enumDefinitionFromMember(member));
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

        @Override
        public Builder id(String id) {
            return (Builder) super.id(id);
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
                Optional<MemberShape> member = EnumShape.memberFromEnumDefinition(definition, getId(), synthesizeNames);
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
            if (!member.hasTrait(EnumValueTrait.ID)) {
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

        @Override
        public Builder source(SourceLocation source) {
            return (Builder) super.source(source);
        }
    }
}
