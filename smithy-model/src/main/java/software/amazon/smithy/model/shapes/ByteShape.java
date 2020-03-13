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
 * Represents a {@code byte} shape.
 */
public final class ByteShape extends NumberShape implements ToSmithyBuilder<ByteShape> {

    private ByteShape(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().from(this);
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.byteShape(this);
    }

    @Override
    public Optional<ByteShape> asByteShape() {
        return Optional.of(this);
    }

    @Override
    public ByteShape expectByteShape() {
        return this;
    }

    /**
     * Builder used to create a {@link ByteShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, ByteShape> {
        @Override
        public ByteShape build() {
            return new ByteShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.BYTE;
        }
    }
}
