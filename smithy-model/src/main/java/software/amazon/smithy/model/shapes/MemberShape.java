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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a member that targets another shape by ID.
 */
public final class MemberShape extends Shape implements ToSmithyBuilder<MemberShape> {

    private final ShapeId target;
    private final String memberName;

    private MemberShape(Builder builder) {
        super(builder, true);
        this.target = SmithyBuilder.requiredState("target", builder.target);
        this.memberName = getId().getMember().orElse("");
    }

    @Override
    protected void validateMixins(Collection<ShapeId> mixins) {
        // This can only happen by manipulating the semantic model in code.
        if (mixins.size() > 1) {
            throw new SourceException("Members must not have more than one mixin: " + getId(), this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder()).target(target);
    }

    /**
     * Gets the shape in which the member is contained.
     *
     * @return Returns the containing shape id.
     */
    public ShapeId getContainer() {
        return getId().withoutMember();
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.memberShape(this);
    }

    @Override
    public Optional<MemberShape> asMemberShape() {
        return Optional.of(this);
    }

    /**
     * Get the targeted member shape ID.
     *
     * @return Returns the member shape ID.
     */
    public ShapeId getTarget() {
        return target;
    }

    /**
     * Get the member name of the member.
     *
     * @return Returns the member name.
     */
    public String getMemberName() {
        return memberName;
    }

    /**
     * @return Returns true if the member has the required trait.
     */
    public boolean isRequired() {
        return findTrait("required").isPresent();
    }

    /**
     * @return Returns true if the member is not required.
     */
    public boolean isOptional() {
        return !isRequired();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && getTarget().equals(((MemberShape) other).getTarget());
    }

    @Override
    public <T extends Trait> Optional<T> getMemberTrait(Model model, Class<T> trait) {
        return OptionalUtils.or(
                getTrait(trait),
                () -> model.getShape(getTarget()).flatMap(targetedShape -> targetedShape.getTrait(trait))
        );
    }

    @Override
    public Optional<Trait> findMemberTrait(Model model, String traitName) {
        return OptionalUtils.or(
                findTrait(traitName),
                () -> model.getShape(getTarget()).flatMap(targetedShape -> targetedShape.findTrait(traitName))
        );
    }

    /**
     * Builder used to create a {@link MemberShape}.
     */
    public static class Builder extends AbstractShapeBuilder<Builder, MemberShape> {

        private ShapeId target;

        @Override
        public MemberShape build() {
            return new MemberShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.MEMBER;
        }

        /**
         * Sets a member shape ID on the builder.
         *
         * @param shapeId MemberShape targeted shape ID.
         * @return Returns the builder.
         */
        public Builder target(ToShapeId shapeId) {
            target = shapeId.toShapeId();
            return this;
        }

        /**
         * Sets a member shape ID on the builder.
         *
         * @param shapeId Targeted shape ID as an absolute member.
         * @return Returns the builder.
         * @throws ShapeIdSyntaxException if the shape ID is invalid.
         */
        public Builder target(String shapeId) {
            return target(ShapeId.from(shapeId));
        }
    }
}
