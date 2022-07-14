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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code map} shape.
 */
public final class MapShape extends Shape implements ToSmithyBuilder<MapShape> {

    private final MemberShape key;
    private final MemberShape value;

    private MapShape(Builder builder) {
        super(builder, false);
        key = builder.key != null ? builder.key : getRequiredMixinMember(builder, "key");
        value = builder.value != null ? builder.value : getRequiredMixinMember(builder, "value");
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
    public Collection<MemberShape> members() {
        return ListUtils.of(key, value);
    }

    @Override
    public Optional<MemberShape> getMember(String name) {
        switch (name) {
            case "key":
                return Optional.of(key);
            case "value":
                return Optional.of(value);
            default:
                return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        } else {
            MapShape otherShape = (MapShape) other;
            return super.equals(otherShape)
                    && getKey().equals(otherShape.getKey())
                    && getValue().equals(((MapShape) other).getValue());
        }
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
                throw new IllegalStateException("Invalid member given to MapShape builder: " + member.getId());
            }
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
