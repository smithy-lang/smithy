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
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;

/**
 * Represents a {@code set} shape.
 *
 * <p>Sets are deprecated. Use list shapes with the uniqueItems trait instead.
 */
@Deprecated
public final class SetShape extends ListShape {

    private SetShape(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        // Always add a synthetic UniqueItems trait.
        return new Builder().addTrait(new UniqueItemsTrait(true));
    }

    @Override
    public Builder toBuilder() {
        return (Builder) builder().from(this).member(getMember());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.setShape(this);
    }

    @Override
    public Optional<SetShape> asSetShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.SET;
    }

    /**
     * Builder used to create a {@link SetShape}.
     */
    public static final class Builder extends ListShape.Builder {
        @Override
        public SetShape build() {
            return new SetShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.SET;
        }

        @Override
        public Builder member(MemberShape member) {
            return (Builder) super.member(member);
        }

        @Override
        public Builder member(ShapeId target) {
            return (Builder) super.member(target);
        }

        @Override
        public Builder member(ShapeId target, Consumer<MemberShape.Builder> memberUpdater) {
            return (Builder) super.member(target, memberUpdater);
        }

        @Override
        public Builder id(ShapeId shapeId) {
            return (Builder) super.id(shapeId);
        }

        @Override
        public Builder addMember(MemberShape member) {
            return (Builder) super.addMember(member);
        }

        @Override
        public Builder id(String shapeId) {
            return (Builder) super.id(shapeId);
        }

        @Override
        public Builder source(SourceLocation sourceLocation) {
            return (Builder) super.source(sourceLocation);
        }

        @Override
        public Builder source(String filename, int line, int column) {
            return (Builder) super.source(filename, line, column);
        }

        @Override
        public Builder traits(Collection<Trait> traitsToSet) {
            return (Builder) super.traits(traitsToSet);
        }

        @Override
        public Builder addTraits(Collection<Trait> traitsToAdd) {
            return (Builder) super.addTraits(traitsToAdd);
        }

        @Override
        public Builder addTrait(Trait trait) {
            return (Builder) super.addTrait(trait);
        }

        @Override
        public Builder removeTrait(String traitId) {
            return (Builder) super.removeTrait(traitId);
        }

        @Override
        public Builder removeTrait(ShapeId traitId) {
            return (Builder) super.removeTrait(traitId);
        }

        @Override
        public Builder clearTraits() {
            return (Builder) super.clearTraits();
        }
    }
}
