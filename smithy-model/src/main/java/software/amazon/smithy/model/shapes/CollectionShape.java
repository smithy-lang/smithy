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

import java.util.Objects;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Abstract class representing Set and List shapes.
 */
public abstract class CollectionShape extends Shape {

    private final MemberShape member;

    CollectionShape(Builder builder) {
        super(builder, false);
        member = SmithyBuilder.requiredState("member", builder.member);
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
    public boolean equals(Object other) {
        return super.equals(other) && getMember().equals(((CollectionShape) other).getMember());
    }

    /**
     * Builder used to create a List or Set shape.
     * @param <B> Concrete builder type.
     * @param <S> Shape type being created.
     */
    public abstract static class Builder<B extends Builder, S extends CollectionShape>
            extends AbstractShapeBuilder<B, S> {

        private MemberShape member;

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

        @Override
        public final B addMember(MemberShape member) {
            return member(member);
        }
    }
}
