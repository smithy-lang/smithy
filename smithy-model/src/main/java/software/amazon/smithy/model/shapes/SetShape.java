/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
 * When serialized using IDL v2, sets are converted to lists with the
 * uniqueItems trait.
 */
@Deprecated
public final class SetShape extends ListShape {

    private SetShape(Builder builder) {
        super(prepareBuilder(builder));
        validateMemberShapeIds();
    }

    private static Builder prepareBuilder(Builder builder) {
        // Always add a UniqueItems trait that is serialized when the set is serialized as a list for IDL v2.
        builder.addTrait(new UniqueItemsTrait(builder.getSourceLocation()));
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return (Builder) updateBuilder(builder()).member(getMember());
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
        public Builder clearMembers() {
            return (Builder) super.clearMembers();
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
        public Builder addTraits(Collection<? extends Trait> traitsToAdd) {
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
