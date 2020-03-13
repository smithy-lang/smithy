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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents an API operation.
 */
public final class OperationShape extends Shape implements ToSmithyBuilder<OperationShape> {
    private final ShapeId input;
    private final ShapeId output;
    private final List<ShapeId> errors;

    private OperationShape(Builder builder) {
        super(builder, false);
        errors = ListUtils.copyOf(builder.errors);
        input = builder.input;
        output = builder.output;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().from(this)
                .input(input)
                .output(output)
                .errors(errors);
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.operationShape(this);
    }

    @Override
    public Optional<OperationShape> asOperationShape() {
        return Optional.of(this);
    }

    @Override
    public OperationShape expectOperationShape() {
        return this;
    }

    /**
     * <p>Gets the optional shape ID of the input of the operation.</p>
     *
     * @return Returns the optional shape ID.
     */
    public Optional<ShapeId> getInput() {
        return Optional.ofNullable(input);
    }

    /**
     * <p>Gets the optional shape ID of the output of the operation.</p>
     *
     * @return Returns the optional shape ID.
     */
    public Optional<ShapeId> getOutput() {
        return Optional.ofNullable(output);
    }

    /**
     * <p>Gets a list of the error shape IDs that can be encountered.</p>
     *
     * <p>Each returned {@link ShapeId} must resolve to a
     * {@link StructureShape} that is targeted by an error trait; however,
     * this is only guaranteed after a model is validated.</p>
     *
     * @return Returns the errors.
     */
    public List<ShapeId> getErrors() {
        return errors;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        } else {
            OperationShape otherShape = (OperationShape) other;
            return Objects.equals(input, otherShape.input)
                   && Objects.equals(output, otherShape.output)
                   && errors.equals(otherShape.errors);
        }
    }

    /**
     * Builder used to create a {@link OperationShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, OperationShape> {
        private ShapeId input;
        private ShapeId output;
        private List<ShapeId> errors = new ArrayList<>();

        @Override
        public ShapeType getShapeType() {
            return ShapeType.OPERATION;
        }

        /**
         * Sets the input shape ID of the operation.
         *
         * @param inputShape Shape ID that MUST reference a structure.
         *  Set to null to clear.
         * @return Returns the builder.
         */
        public Builder input(ToShapeId inputShape) {
            input = inputShape == null ? null : inputShape.toShapeId();
            return this;
        }

        /**
         * Sets the output shape ID of the operation.
         *
         * @param outputShape Shape ID that MUST reference a structure.
         *  Set to null to clear.
         * @return Returns the builder.
         */
        public Builder output(ToShapeId outputShape) {
            output = outputShape == null ? null : outputShape.toShapeId();
            return this;
        }

        /**
         * Sets and replaces the errors of the operation.
         *
         * @param errorShapeIds Error shape IDs to set.
         * @return Returns the builder.
         */
        public Builder errors(Collection<ShapeId> errorShapeIds) {
            errors.clear();
            errorShapeIds.forEach(this::addError);
            return this;
        }

        /**
         * Adds an error to the operation.
         *
         * @param errorShapeId Error shape ID to add.
         * @return Returns the builder.
         */
        public Builder addError(ToShapeId errorShapeId) {
            errors.add(errorShapeId.toShapeId());
            return this;
        }

        /**
         * Adds an error to the operation.
         *
         * @param errorShapeId Error shape ID to add.
         * @return Returns the builder.
         * @throws ShapeIdSyntaxException if the shape ID is invalid.
         */
        public Builder addError(String errorShapeId) {
            return addError(ShapeId.from(errorShapeId));
        }

        /**
         * Adds an each of the errors to the operation.
         *
         * @param errorShapeIds Error shape IDs to add.
         * @return Returns the builder.
         */
        public Builder addErrors(List<ShapeId> errorShapeIds) {
            errors.addAll(Objects.requireNonNull(errorShapeIds));
            return this;
        }

        /**
         * Removes an error by Shape ID.
         *
         * @param errorShapeId Error shape ID to remove.
         * @return Returns the builder.
         */
        public Builder removeError(ToShapeId errorShapeId) {
            errors.remove(errorShapeId.toShapeId());
            return this;
        }

        /**
         * Removes all errors.
         * @return Returns the builder.
         */
        public Builder clearErrors() {
            errors.clear();
            return this;
        }

        @Override
        public OperationShape build() {
            return new OperationShape(this);
        }
    }
}
