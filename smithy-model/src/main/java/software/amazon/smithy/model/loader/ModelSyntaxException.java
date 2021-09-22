/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Thrown when the syntax of the IDL is invalid.
 */
public class ModelSyntaxException extends SourceException implements ToShapeId {
    private final ShapeId shapeId;

    public ModelSyntaxException(String message, int line, int column) {
        this(builder().message(message).sourceLocation(line, column));
    }

    public ModelSyntaxException(String message, String filename, int line, int column) {
        this(builder().message(message).sourceLocation(filename, line, column));
    }

    public ModelSyntaxException(String message, FromSourceLocation sourceLocation) {
        this(builder().message(message).sourceLocation(sourceLocation.getSourceLocation()));
    }

    private ModelSyntaxException(Builder builder) {
        super(builder.message, builder.sourceLocation);
        this.shapeId = builder.shapeId;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public ShapeId toShapeId() {
        return shapeId;
    }

    static final class Builder implements SmithyBuilder<ModelSyntaxException> {
        private SourceLocation sourceLocation = SourceLocation.NONE;
        private ShapeId shapeId = null;
        private String message;

        private Builder() {}

        @Override
        public ModelSyntaxException build() {
            SmithyBuilder.requiredState("message", message);
            return new ModelSyntaxException(this);
        }

        Builder shapeId(ShapeId shapeId) {
            this.shapeId = shapeId;
            return this;
        }

        Builder message(String message) {
            this.message = message;
            return this;
        }

        Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation.getSourceLocation();
            return this;
        }

        Builder sourceLocation(String filename, int line, int column) {
            return sourceLocation(new SourceLocation(filename, line, column));
        }

        Builder sourceLocation(int line, int column) {
            return sourceLocation(SourceLocation.NONE.getFilename(), line, column);
        }
    }
}
