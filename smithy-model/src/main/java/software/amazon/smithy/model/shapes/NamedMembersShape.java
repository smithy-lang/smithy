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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Abstract classes shared by structure and union shapes.
 *
 * <p>The order of members in structures and unions are the same as the
 * order that they are defined in the model.
 */
abstract class NamedMembersShape extends Shape {

    private final Map<String, MemberShape> members;
    private volatile List<String> memberNames;

    NamedMembersShape(NamedMembersShape.Builder<?, ?> builder) {
        super(builder, false);
        assert builder.members != null;

        // Copy the members to make them immutable and ensure that each
        // member has a valid ID that is prefixed with the shape ID.
        members = MapUtils.orderedCopyOf(builder.members);

        members.forEach((key, value) -> {
            ShapeId expected = getId().withMember(key);
            if (!value.getId().equals(expected)) {
                throw new IllegalArgumentException(String.format(
                        "Expected the `%s` member of `%s` to have an ID of `%s` but found `%s`",
                        key, getId(), expected, value.getId()));
            }
        });
    }

    /**
     * Gets the members of the shape.
     *
     * @return Returns the immutable member map.
     */
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model.
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
    abstract static class Builder<B extends Builder, S extends NamedMembersShape> extends AbstractShapeBuilder<B, S> {

        Map<String, MemberShape> members = new LinkedHashMap<>();

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
            this.members.clear();
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

        /**
         * Adds a member to the builder.
         *
         * @param member Shape targeted by the member.
         * @return Returns the builder.
         */
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
         * @param member Member name to remove.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B removeMember(String member) {
            members.remove(member);
            return (B) this;
        }
    }
}
