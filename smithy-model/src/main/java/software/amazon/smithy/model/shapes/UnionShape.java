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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.ToSmithyBuilder;

/**
 * Tagged union shape that maps member names to member definitions.
 */
public final class UnionShape extends Shape implements ToSmithyBuilder<UnionShape> {
    private final Map<String, MemberShape> members;

    private UnionShape(Builder builder) {
        super(builder, ShapeType.UNION, false);
        if (builder.members.isEmpty()) {
            throw new SourceException("union shapes require at least one member", builder.source);
        }

        builder.members.forEach((key, value) -> {
            ShapeId expected = getId().withMember(key);
            if (!value.getId().equals(expected)) {
                throw new IllegalArgumentException(
                        format("Expected the `%s` member of `%s` to have an ID of `%s` but found `%s`",
                               key, getId(), expected, value.getId()));
            }
        });

        members = new LinkedHashMap<>(builder.members);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().from(this).members(getAllMembers().values());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.unionShape(this);
    }

    @Override
    public Optional<UnionShape> asUnionShape() {
        return Optional.of(this);
    }

    /**
     * Returns true if the union is empty and contains no members.
     *
     * @return Returns true if empty.
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * Gets a member by name, case-sensitively.
     *
     * @param tag Name of the member to retrieve.
     * @return Returns the member wrapped in an {@link Optional}.
     */
    public Optional<MemberShape> getMember(String tag) {
        return Optional.ofNullable(members.get(tag));
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
     * Gets the member mapping of the union.
     *
     * @return Returns the members.
     */
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && members.equals(((UnionShape) other).members);
    }

    /**
     * Builder used to create a {@link UnionShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, UnionShape> {
        private final Map<String, MemberShape> members = new LinkedHashMap<>();

        @Override
        public UnionShape build() {
            return new UnionShape(this);
        }

        public Builder members(Collection<MemberShape> members) {
            clearMembers();
            members.forEach(this::addMember);
            return this;
        }

        @Override
        public Builder addMember(MemberShape member) {
            members.put(member.getMemberName(), member);
            return this;
        }

        public Builder removeMember(String member) {
            members.remove(member);
            return this;
        }

        public Builder clearMembers() {
            members.clear();
            return this;
        }
    }
}
