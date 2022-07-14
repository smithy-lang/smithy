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
 * Represents an {@code integer} shape.
 */
public class IntegerShape extends NumberShape implements ToSmithyBuilder<IntegerShape> {

    IntegerShape(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.integerShape(this);
    }

    @Override
    public Optional<IntegerShape> asIntegerShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.INTEGER;
    }

    /**
     * Builder used to create a {@link IntegerShape}.
     */
    public static class Builder extends AbstractShapeBuilder<Builder, IntegerShape> {
        @Override
        public IntegerShape build() {
            return new IntegerShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.INTEGER;
        }
    }
}
