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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Abstract classes shared by structure and union shapes.
 *
 * <p>The order of members in structures and unions are the same as the
 * order that they are defined in the model.
 */
public abstract class NamedMembersShape extends Shape {

    private final Map<String, MemberShape> members;
    private volatile List<String> memberNames;

    NamedMembersShape(NamedMembersShape.Builder<?, ?> builder) {
        super(builder, false);

        if (getMixins().isEmpty()) {
            members = MapUtils.orderedCopyOf(builder.members);
        } else {
            // Compute mixin members of this shape that inherit traits from mixin members.
            Map<String, MemberShape> computedMembers = new LinkedHashMap<>();
            for (Shape shape : builder.getMixins().values()) {
                for (MemberShape member : shape.members()) {
                    String name = member.getMemberName();
                    if (builder.members.containsKey(name)) {
                        MemberShape localMember = builder.members.get(name);
                        // Rebuild the member with the proper inherited mixin if needed.
                        // This catches errant cases where a member is added to a structure/union
                        // but omits the mixin members of parent shapes. Arguably, that's way too
                        // nuanced and error-prone to _not_ try to smooth over.
                        if (localMember.getMixins().isEmpty() || !builder.getMixins().containsKey(member.getId())) {
                            localMember = localMember.toBuilder().clearMixins().addMixin(member).build();
                        }
                        computedMembers.put(name, localMember);
                    } else {
                        computedMembers.put(name, MemberShape.builder()
                                .id(getId().withMember(name))
                                .target(member.getTarget())
                                .source(getSourceLocation())
                                .addMixin(member)
                                .build());
                    }
                }
            }

            // Add members local to the structure after inherited members.
            for (MemberShape member : builder.members.values()) {
                if (!computedMembers.containsKey(member.getMemberName())) {
                    computedMembers.put(member.getMemberName(), member);
                }
            }

            members = Collections.unmodifiableMap(computedMembers);
        }

        validateMemberShapeIds();
    }

    @Override
    protected void validateMixins(Collection<ShapeId> mixins) {
        // do nothing. Mixins are allowed on structures and unions.
    }

    /**
     * Gets the members of the shape, including mixin members.
     *
     * @return Returns the immutable member map.
     */
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model, including mixin members.
     *
     * @return Returns an immutable list of member names.
     */
    public List<String> getMemberNames() {
        List<String> names = memberNames;
        if (names == null) {
            names = ListUtils.copyOf(members.keySet());
            memberNames = names;
        }

        return names;
    }

    /**
     * Get a specific member by name.
     *
     * @param name Name of the member to retrieve.
     * @return Returns the optional member.
     */
    public Optional<MemberShape> getMember(String name) {
        return Optional.ofNullable(members.get(name));
    }

    @Override
    public Collection<MemberShape> members() {
        return members.values();
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        // Members are ordered, so do a test on the ordering and their values.
        NamedMembersShape b = (NamedMembersShape) other;
        return getMemberNames().equals(b.getMemberNames()) && members.equals(b.members);
    }

    /**
     * Builder used to create a List or Set shape.
     * @param <B> Concrete builder type.
     * @param <S> Shape type being created.
     */
    public abstract static class Builder<B extends Builder<B, S>, S extends NamedMembersShape>
            extends AbstractShapeBuilder<B, S> {

        private final Map<String, MemberShape> members = new LinkedHashMap<>();

        @Override
        public final B id(ShapeId shapeId) {
            // If there are already any members set, update their ids to point to the new parent id.
            for (MemberShape member : members.values()) {
                addMember(member.toBuilder().id(shapeId.withMember(member.getMemberName())).build());
            }
            return super.id(shapeId);
        }

        /**
         * Replaces the members of the builder.
         *
         * @param members Members to add to the builder.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B members(Collection<MemberShape> members) {
            clearMembers();
            for (MemberShape member : members) {
                addMember(member);
            }
            return (B) this;
        }

        /**
         * Removes all members from the shape.
         *
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B clearMembers() {
            members.clear();
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addMember(MemberShape member) {
            members.put(member.getMemberName(), member);
            return (B) this;
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param target Target of the member.
         * @return Returns the builder.
         */
        public B addMember(String memberName, ShapeId target) {
            return addMember(memberName, target, null);
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param target Target of the member.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public B addMember(String memberName, ShapeId target, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder().target(target).id(getId().withMember(memberName));

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
        @SuppressWarnings("unchecked")
        public B removeMember(String member) {
            members.remove(member);
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addMixin(Shape shape) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before adding a mixin");
            }

            super.addMixin(shape);

            // Clean up members that were previously mixed in by the given shape but
            // are no longer present on the given shape.
            members.values().removeIf(member -> {
                if (!isMemberMixedInFromShape(shape.getId(), member)) {
                    return false;
                }
                for (MemberShape mixinMember : shape.members()) {
                    if (mixinMember.getMemberName().equals(member.getMemberName())) {
                        return false;
                    }
                }
                return true;
            });

            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B removeMixin(ToShapeId shape) {
            super.removeMixin(shape);
            ShapeId id = shape.toShapeId();
            // Remove members that have a mixin where the ID equals the given ID or
            // the mixin ID without a member equals the given ID.
            members.values().removeIf(member -> isMemberMixedInFromShape(id, member));
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B flattenMixins() {
            if (getMixins().isEmpty()) {
                return (B) this;
            }

            // Ensure that the members are ordered, mixin members first, followed by local members.
            Set<MemberShape> orderedMembers = new LinkedHashSet<>();

            // Copy members from mixins onto the shape.
            for (Shape mixin : getMixins().values()) {
                for (MemberShape member : mixin.members()) {
                    SourceLocation location = getSourceLocation();
                    Collection<Trait> localTraits = Collections.emptyList();
                    MemberShape existing = members.remove(member.getMemberName());
                    if (existing != null) {
                        localTraits = existing.getIntroducedTraits().values();
                        location = existing.getSourceLocation();
                    }
                    orderedMembers.add(MemberShape.builder()
                            .id(getId().withMember(member.getMemberName()))
                            .target(member.getTarget())
                            .addTraits(member.getAllTraits().values())
                            .addTraits(localTraits)
                            .source(location)
                            .build());
                }
            }

            // Add any local members _after_ mixin members. LinkedHashSet will keep insertion
            // order, so no need to check for non-mixin members first.
            orderedMembers.addAll(members.values());
            members(orderedMembers);

            return super.flattenMixins();
        }

        private boolean isMemberMixedInFromShape(ShapeId check, MemberShape member) {
            if (member.getMixins().contains(check)) {
                return true;
            }
            for (ShapeId memberMixin : member.getMixins()) {
                if (memberMixin.withoutMember().equals(check)) {
                    return true;
                }
            }
            return false;
        }
    }
}
