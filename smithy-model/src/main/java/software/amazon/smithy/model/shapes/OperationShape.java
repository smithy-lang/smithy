/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents an API operation.
 */
public final class OperationShape extends Shape implements ToSmithyBuilder<OperationShape> {
    private final ShapeId input;
    private final ShapeId output;
    private final List<ShapeId> errors;
    private final List<ShapeId> introducedErrors;

    private OperationShape(Builder builder) {
        super(builder, false);

        input = Objects.requireNonNull(builder.input);
        output = Objects.requireNonNull(builder.output);

        if (getMixins().isEmpty()) {
            errors = builder.errors.copy();
            introducedErrors = errors;
        } else {
            // Compute mixin properties of the operation. Input / output are
            // forbidden in operation mixins, so we don't bother with them
            // here.
            Set<ShapeId> computedErrors = new LinkedHashSet<>();
            for (Shape shape : builder.getMixins().values()) {
                shape.asOperationShape().ifPresent(mixin -> computedErrors.addAll(mixin.getErrors()));
            }
            introducedErrors = builder.errors.copy();
            computedErrors.addAll(introducedErrors);
            errors = Collections.unmodifiableList(new ArrayList<>(computedErrors));
        }

        if (hasTrait(MixinTrait.ID) && (!input.equals(UnitTypeTrait.UNIT) || !output.equals(UnitTypeTrait.UNIT))) {
            throw new SourceException(String.format(
                    "Operation shapes with the mixin trait MUST target `%s` for their input and output. Operation "
                            + "mixin shape `%s` defines one or both of these properties.",
                    UnitTypeTrait.UNIT,
                    getId()), builder.getSourceLocation());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder())
                .input(input)
                .output(output)
                .errors(getIntroducedErrors());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.operationShape(this);
    }

    @Override
    public Optional<OperationShape> asOperationShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.OPERATION;
    }

    /**
     * <p>Gets the optional shape ID of the input of the operation.</p>
     *
     * <p>For backward compatibility, if the input targets {@code smithy.api#Unit},
     * then an empty optional is returned.
     *
     * @return Returns the optional shape ID.
     */
    public Optional<ShapeId> getInput() {
        return input.equals(UnitTypeTrait.UNIT) ? Optional.empty() : Optional.of(input);
    }

    /**
     * <p>Gets the optional shape ID of the output of the operation.</p>
     *
     * <p>For backward compatibility, if the output targets {@code smithy.api#Unit},
     * then an empty optional is returned.
     *
     * @return Returns the optional shape ID.
     */
    public Optional<ShapeId> getOutput() {
        return output.equals(UnitTypeTrait.UNIT) ? Optional.empty() : Optional.of(output);
    }

    /**
     * Gets the input of the operation.
     *
     * <p>All operations have input, and they default to target
     * {@code smithy.api#Unit}.
     *
     * @return Returns the non-nullable input.
     */
    public ShapeId getInputShape() {
        return input;
    }

    /**
     * Gets the output of the operation.
     *
     * <p>All operations have output, and they default to target
     * {@code smithy.api#Unit}.
     *
     * @return Returns the non-nullable output.
     */
    public ShapeId getOutputShape() {
        return output;
    }

    /**
     * <p>Gets a list of the error shape IDs bound directly to the operation
     * that can be encountered.
     *
     * <p>This DOES NOT include errors that are common to a service. Operations
     * can be bound to multiple services, so common service errors cannot be
     * returned by this method. Use {@link #getErrors(ServiceShape)} or
     * {@link OperationIndex#getErrors(ToShapeId, ToShapeId)} to get all of the
     * errors an operation can encounter when used within a service.</p>
     *
     * <p>Each returned {@link ShapeId} must resolve to a
     * {@link StructureShape} that is targeted by an error trait; however,
     * this is only guaranteed after a model is validated.</p>
     *
     * @return Returns the errors.
     * @see #getErrors(ServiceShape)
     * @see OperationIndex#getErrors(ToShapeId, ToShapeId)
     */
    public List<ShapeId> getErrors() {
        return errors;
    }

    /**
     * Gets the errors introduced by the shape and not inherited
     * from mixins.
     *
     * @return Returns the introduced errors.
     */
    public List<ShapeId> getIntroducedErrors() {
        return introducedErrors;
    }

    /**
     * <p>Gets a list of the error shape IDs the operation can encounter,
     * including any common errors of a service.
     *
     * <p>No validation is performed here to ensure that the operation is
     * actually bound to the given service shape.
     *
     * @return Returns the errors.
     * @see OperationIndex#getErrors(ToShapeId, ToShapeId)
     */
    public List<ShapeId> getErrors(ServiceShape service) {
        Set<ShapeId> result = new LinkedHashSet<>(service.getErrors());
        result.addAll(getErrors());
        return new ArrayList<>(result);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        } else {
            OperationShape otherShape = (OperationShape) other;
            return input.equals(otherShape.input)
                    && output.equals(otherShape.output)
                    && errors.equals(otherShape.errors);
        }
    }

    /**
     * Builder used to create a {@link OperationShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, OperationShape> {
        private ShapeId input = UnitTypeTrait.UNIT;
        private ShapeId output = UnitTypeTrait.UNIT;
        private final BuilderRef<List<ShapeId>> errors = BuilderRef.forList();

        @Override
        public ShapeType getShapeType() {
            return ShapeType.OPERATION;
        }

        /**
         * Sets the input shape ID of the operation.
         *
         * @param inputShape Shape ID that MUST reference a structure.
         * @return Returns the builder.
         */
        public Builder input(ToShapeId inputShape) {
            input = inputShape == null ? UnitTypeTrait.UNIT : inputShape.toShapeId();
            return this;
        }

        /**
         * Sets the output shape ID of the operation.
         *
         * @param outputShape Shape ID that MUST reference a structure.
         * @return Returns the builder.
         */
        public Builder output(ToShapeId outputShape) {
            output = outputShape == null ? UnitTypeTrait.UNIT : outputShape.toShapeId();
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
            errors.get().add(errorShapeId.toShapeId());
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
        public Builder addErrors(Collection<ShapeId> errorShapeIds) {
            errors.get().addAll(Objects.requireNonNull(errorShapeIds));
            return this;
        }

        /**
         * Removes an error by Shape ID.
         *
         * @param errorShapeId Error shape ID to remove.
         * @return Returns the builder.
         */
        public Builder removeError(ToShapeId errorShapeId) {
            errors.get().remove(errorShapeId.toShapeId());
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

        @Override
        public Builder flattenMixins() {
            if (getMixins().isEmpty()) {
                return this;
            }

            Set<ShapeId> computedErrors = new LinkedHashSet<>();
            for (Shape shape : getMixins().values()) {
                shape.asOperationShape().ifPresent(mixin -> computedErrors.addAll(mixin.getErrors()));
            }

            computedErrors.addAll(errors.peek());
            errors(computedErrors);
            return super.flattenMixins();
        }
    }
}
