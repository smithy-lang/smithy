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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code map} shape.
 */
public final class MapShape extends Shape implements ToSmithyBuilder<MapShape> {

    private final MemberShape key;
    private final MemberShape value;
    private transient Map<String, MemberShape> memberMap;

    private MapShape(Builder builder) {
        super(builder, false);
        MemberShape[] members = getRequiredMembers(builder, "key", "value");
        key = members[0];
        value = members[1];
        validateMemberShapeIds();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder()).key(key).value(value);
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.mapShape(this);
    }

    @Override
    public Optional<MapShape> asMapShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.MAP;
    }

    /**
     * Get the value member shape of the map.
     *
     * @return Returns the value member shape.
     */
    public MemberShape getValue() {
        return value;
    }

    /**
     * Get the key member shape of the map.
     *
     * @return Returns the key member shape.
     */
    public MemberShape getKey() {
        return key;
    }

    @Override
    public Optional<MemberShape> getMember(String memberName) {
        if ("key".equals(memberName)) {
            return Optional.of(key);
        }
        if ("value".equals(memberName)) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, MemberShape> getAllMembers() {
        Map<String, MemberShape> result = memberMap;

        // Create a two-entry map that only knows about "key" and "value".
        if (result == null) {
            result = new AbstractMap<String, MemberShape>() {
                private transient Set<Entry<String, MemberShape>> entries;
                private transient List<MemberShape> values;

                @Override
                public MemberShape get(Object keyName) {
                    if ("key".equals(keyName)) {
                        return key;
                    } else if ("value".equals(keyName)) {
                        return value;
                    } else {
                        return null;
                    }
                }

                @Override
                public boolean containsKey(Object keyName) {
                    return "key".equals(keyName) || "value".equals(keyName);
                }

                @Override
                public Set<String> keySet() {
                    return SetUtils.of("key", "value");
                }

                @Override
                public Collection<MemberShape> values() {
                    List<MemberShape> result = values;
                    if (result == null) {
                        result = ListUtils.of(key, value);
                        values = result;
                    }
                    return result;
                }

                @Override
                public Set<Entry<String, MemberShape>> entrySet() {
                    Set<Entry<String, MemberShape>> result = entries;
                    if (result == null) {
                        result = SetUtils.of(Pair.of("key", key), Pair.of("value", value));
                        entries = result;
                    }
                    return result;
                }
            };
            memberMap = result;
        }

        return result;
    }

    /**
     * Builder used to create a {@link ListShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, MapShape> {

        private MemberShape key;
        private MemberShape value;

        @Override
        public MapShape build() {
            return new MapShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.MAP;
        }

        @Override
        public Optional<MemberShape> getMember(String memberName) {
            if ("key".equals(memberName)) {
                return Optional.ofNullable(key);
            } else if ("value".equals(memberName)) {
                return Optional.ofNullable(value);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Builder id(ShapeId shapeId) {
            // If the shape id has changed then the key and value member ids also need to be updated.
            if (key != null) {
                key(key.toBuilder().id(shapeId.withMember(key.getMemberName())).build());
            }
            if (value != null) {
                value(value.toBuilder().id(shapeId.withMember(value.getMemberName())).build());
            }
            return super.id(shapeId);
        }

        public Builder key(MemberShape member) {
            this.key = Objects.requireNonNull(member);
            return this;
        }

        public Builder value(MemberShape member) {
            this.value = Objects.requireNonNull(member);
            return this;
        }

        @Override
        public Builder addMember(MemberShape member) {
            if (member.getMemberName().equals("key")) {
                return key(member);
            } else if (member.getMemberName().equals("value")) {
                return value(member);
            } else {
                String message = String.format("Map shapes may only have `key` and `value` members, but found `%s`",
                        member.getMemberName());
                throw new SourceException(message, member);
            }
        }

        @Override
        public Builder clearMembers() {
            key = null;
            value = null;
            return this;
        }

        /**
         * Sets the key member of the map.
         *
         * @param target Target of the member.
         * @return Returns the builder.
         */
        public Builder key(ShapeId target) {
            return key(target, null);
        }

        /**
         * Sets the key member of the map.
         *
         * @param target Target of the member.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public Builder key(ShapeId target, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(target)
                    .id(getId().withMember("key"));

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return key(builder.build());
        }

        /**
         * Sets the value member of the map.
         *
         * @param target Target of the member.
         * @return Returns the builder.
         */
        public Builder value(ShapeId target) {
            return value(target, null);
        }

        /**
         * Sets the value member of the map.
         *
         * @param target Target of the member.
         * @param memberUpdater Consumer that can updated the created member shape.
         * @return Returns the builder.
         */
        public Builder value(ShapeId target, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(target)
                    .id(getId().withMember("value"));

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return value(builder.build());
        }

        @Override
        public Builder flattenMixins() {
            if (getMixins().isEmpty()) {
                return this;
            }

            for (Shape mixin : getMixins().values()) {
                for (MemberShape member : mixin.members()) {
                    SourceLocation location = getSourceLocation();
                    Collection<Trait> localTraits = Collections.emptyList();

                    MemberShape existing;
                    if (member.getMemberName().equals("key")) {
                        existing = key;
                    } else {
                        existing = value;
                    }

                    if (existing != null) {
                        localTraits = existing.getIntroducedTraits().values();
                        location = existing.getSourceLocation();
                    }

                    addMember(MemberShape.builder()
                            .id(getId().withMember(member.getMemberName()))
                            .target(member.getTarget())
                            .addTraits(member.getAllTraits().values())
                            .addTraits(localTraits)
                            .source(location)
                            .build());
                }
            }

            return super.flattenMixins();
        }
    }
}
