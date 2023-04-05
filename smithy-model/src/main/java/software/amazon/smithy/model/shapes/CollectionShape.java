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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;

/**
 * Abstract class representing Set and List shapes.
 */
public abstract class CollectionShape extends Shape {

    private final MemberShape member;
    private transient Map<String, MemberShape> memberMap;

    CollectionShape(Builder<?, ?> builder) {
        super(builder, false);
        member = getRequiredMixinMember(builder, builder.member, "member");

        ShapeId expected = getId().withMember("member");
        if (!member.getId().equals(expected)) {
            throw new IllegalArgumentException(String.format(
                    "Expected member of `%s` to have an ID of `%s` but found `%s`",
                    getId(), expected, member.getId()));
        }
    }

    /**
     * Get the member shape of the collection.
     *
     * @return Returns the member shape.
     */
    public final MemberShape getMember() {
        return member;
    }

    @Override
    public Optional<MemberShape> getMember(String memberName) {
        if ("member".equals(memberName)) {
            return Optional.of(member);
        }
        return Optional.empty();
    }

    @Override
    public final Map<String, MemberShape> getAllMembers() {
        Map<String, MemberShape> members = memberMap;
        if (members == null) {
            members = Collections.singletonMap("member", member);
            memberMap = members;
        }
        return members;
    }

    /**
     * Builder used to create a List or Set shape.
     * @param <B> Concrete builder type.
     * @param <S> Shape type being created.
     */
    public abstract static class Builder<B extends Builder<B, S>, S extends CollectionShape>
            extends AbstractShapeBuilder<B, S> {

        private MemberShape member;

        @Override
        public B id(ShapeId shapeId) {
            if (member != null) {
                // Update the member name so it isn't pointing to the old shape id.
                member(member.toBuilder().id(shapeId.withMember(member.getMemberName())).build());
            }
            return super.id(shapeId);
        }

        /**
         * Sets the member of the collection.
         * @param member Member of the collection to set.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B member(MemberShape member) {
            this.member = Objects.requireNonNull(member);
            return (B) this;
        }

        /**
         * Sets the member of the collection.
         * @param target Target of the member.
         * @return Returns the builder.
         */
        public B member(ShapeId target) {
            return member(target, null);
        }

        /**
         * Sets the member of the collection.
         *
         * @param target Target of the member.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public B member(ShapeId target, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(target)
                    .id(getId().withMember("member"));

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return member(builder.build());
        }

        @Override
        public B addMember(MemberShape member) {
            return member(member);
        }

        @Override
        public B clearMembers() {
            member = null;
            return super.clearMembers();
        }

        @Override
        @SuppressWarnings("unchecked")
        public B flattenMixins() {
            if (getMixins().isEmpty()) {
                return (B) this;
            }

            for (Shape mixin : getMixins().values()) {
                SourceLocation location = getSourceLocation();
                Collection<Trait> localTraits = Collections.emptyList();
                MemberShape mixinMember = ((CollectionShape) mixin).getMember();
                MemberShape existing = member;
                if (existing != null) {
                    localTraits = existing.getIntroducedTraits().values();
                    location = existing.getSourceLocation();
                }
                member = MemberShape.builder()
                        .id(getId().withMember(mixinMember.getMemberName()))
                        .target(mixinMember.getTarget())
                        .addTraits(mixinMember.getAllTraits().values())
                        .addTraits(localTraits)
                        .source(location)
                        .build();
            }
            return super.flattenMixins();
        }
    }
}
