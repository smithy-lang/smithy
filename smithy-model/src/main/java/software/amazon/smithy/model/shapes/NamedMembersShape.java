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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;

/**
 * Abstract classes shared by structure and union shapes.
 *
 * <p>The order of members in structures and unions are the same as the
 * order that they are defined in the model.
 */
public abstract class NamedMembersShape extends Shape implements NamedMembers {

    private final Map<String, MemberShape> members;
    private volatile List<String> memberNames;

    NamedMembersShape(NamedMembersShape.Builder<?, ?> builder) {
        super(builder, false);
        members = NamedMemberUtils.computeMixinMembers(
                builder.getMixins(), builder.members, getId(), getSourceLocation());
        validateMemberShapeIds();
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

    @Override
    public final Optional<MemberShape> getMember(String name) {
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

        private final BuilderRef<Map<String, MemberShape>> members = BuilderRef.forOrderedMap();

        @Override
        public final B id(ShapeId shapeId) {
            // If there are already any members set, update their ids to point to the new parent id.
            for (MemberShape member : members.peek().values()) {
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
            members.get().put(member.getMemberName(), member);
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
            if (members.hasValue()) {
                members.get().remove(member);
            }
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B addMixin(Shape shape) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before adding a mixin");
            }
            super.addMixin(shape);
            NamedMemberUtils.cleanMixins(shape, members.get());
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B removeMixin(ToShapeId shape) {
            super.removeMixin(shape);
            NamedMemberUtils.removeMixin(shape, members.get());
            return (B) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B flattenMixins() {
            if (getMixins().isEmpty()) {
                return (B) this;
            }
            members(NamedMemberUtils.flattenMixins(members.get(), getMixins(), getId(), getSourceLocation()));
            return super.flattenMixins();
        }
    }
}
