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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Structure shape that maps shape names to members.
 */
public final class StructureShape extends Shape implements ToSmithyBuilder<StructureShape> {

    private final Map<String, MemberShape> members;

    private StructureShape(Builder builder) {
        super(builder, false);
        assert builder.members != null;

        // Copy the members to make them immutable and ensure that each
        // member has a valid ID that is prefixed with the structure ID.
        members = Collections.unmodifiableMap(new LinkedHashMap<>(builder.members));
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
     * @return Creates a new StructureShape builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().from(this).members(getAllMembers().values());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.structureShape(this);
    }

    @Override
    public Optional<StructureShape> asStructureShape() {
        return Optional.of(this);
    }

    /**
     * Gets the members of the structure.
     *
     * @return Returns the immutable member map.
     */
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    /**
     * Returns a list of member names in the order in which they were added.
     *
     * @return Returns list of member names.
     */
    public List<String> getMemberNames() {
        return new ArrayList<>(members.keySet());
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
    public boolean equals(Object other) {
        return super.equals(other) && members.equals(((StructureShape) other).members);
    }

    /**
     * Builder used to create a {@link StructureShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, StructureShape> {

        private Map<String, MemberShape> members = new LinkedHashMap<>();

        @Override
        public StructureShape build() {
            return new StructureShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.STRUCTURE;
        }

        /**
         * Replaces the members of the builder.
         *
         * @param structureMembers Members to add to the builder.
         * @return Returns the builder.
         */
        public Builder members(Collection<MemberShape> structureMembers) {
            members.clear();
            Objects.requireNonNull(structureMembers).forEach(this::addMember);
            return this;
        }

        /**
         * Removes all members from the structure.
         *
         * @return Returns the builder.
         */
        public Builder clearMembers() {
            members.clear();
            return this;
        }

        /**
         * Adds a member to the builder.
         *
         * @param member StructureMember targeted by the member.
         * @return Returns the builder.
         */
        @Override
        public Builder addMember(MemberShape member) {
            members.put(member.getMemberName(), member);
            return this;
        }

        /**
         * Removes a member by name.
         *
         * @param member Member name to remove.
         * @return Returns the builder.
         */
        public Builder removeMember(String member) {
            members.remove(member);
            return this;
        }
    }
}
