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
 * Structure shape that maps shape names to members.
 */
public final class StructureShape extends NamedMembersShape implements ToSmithyBuilder<StructureShape> {

    private StructureShape(Builder builder) {
        super(builder);
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

    @Override
    public StructureShape expectStructureShape() {
        return this;
    }

    /**
     * Builder used to create a {@link StructureShape}.
     */
    public static final class Builder extends NamedMembersShape.Builder<Builder, StructureShape> {
        @Override
        public StructureShape build() {
            return new StructureShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.STRUCTURE;
        }
    }
}
