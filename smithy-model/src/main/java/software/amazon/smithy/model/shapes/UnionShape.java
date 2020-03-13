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

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Tagged union shape that maps member names to member definitions.
 */
public final class UnionShape extends NamedMembersShape implements ToSmithyBuilder<UnionShape> {

    private UnionShape(Builder builder) {
        super(builder);
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

    @Override
    public UnionShape expectUnionShape() {
        return this;
    }

    /**
     * Builder used to create a {@link UnionShape}.
     */
    public static final class Builder extends NamedMembersShape.Builder<Builder, UnionShape> {
        @Override
        public UnionShape build() {
            return new UnionShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.UNION;
        }
    }
}
