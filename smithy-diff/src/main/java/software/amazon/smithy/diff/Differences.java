/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Queryable container for detected structural differences between two models.
 */
public final class Differences implements ToSmithyBuilder<Differences> {
    private final Model oldModel;
    private final Model newModel;

    private final List<Shape> addedShapes;
    private final List<Shape> removedShapes;
    private final List<ChangedShape<Shape>> changedShapes;

    private final List<Pair<String, Node>> addedMetadata;
    private final List<Pair<String, Node>> removedMetadata;
    private final List<ChangedMetadata> changedMetadata;

    private Differences(Builder builder) {
        this.oldModel = SmithyBuilder.requiredState("oldModel", builder.oldModel);
        this.newModel = SmithyBuilder.requiredState("newModel", builder.newModel);
        this.addedShapes = builder.addedShapes.copy();
        this.removedShapes = builder.removedShapes.copy();
        this.changedShapes = builder.changedShapes.copy();
        this.addedMetadata = builder.addedMetadata.copy();
        this.removedMetadata = builder.removedMetadata.copy();
        this.changedMetadata = builder.changedMetadata.copy();
    }

    /**
     * Detects all differences between two models.
     *
     * @param oldModel The previous state of the model.
     * @param newModel The new state of the model.
     * @return The set of differences between the two models.
     */
    public static Differences detect(Model oldModel, Model newModel) {
        return builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .detectShapeChanges()
                .detectMetadataChanges()
                .build();
    }

    /**
     * Gets the old model.
     *
     * @return Returns the old model.
     */
    public Model getOldModel() {
        return oldModel;
    }

    /**
     * Gets the new model.
     *
     * @return Returns the new model.
     */
    public Model getNewModel() {
        return newModel;
    }

    /**
     * Gets all added shapes.
     *
     * @return Returns a stream of each added shape.
     */
    public Stream<Shape> addedShapes() {
        return addedShapes.stream();
    }

    /**
     * Gets all of the added shapes of a specific type.
     *
     * @param shapeType Type of shape to find.
     * @param <T> Type of shape.
     * @return Returns a stream of each added shape of a specific type.
     */
    public <T extends Shape> Stream<T> addedShapes(Class<T> shapeType) {
        return addedShapes().filter(shapeType::isInstance).map(shapeType::cast);
    }

    /**
     * Gets all added metadata.
     *
     * <p>Each Pair returned contains the name of the metadata key on
     * the left of the Pair and the metadata value on the right.
     *
     * @return Returns a stream of added metadata.
     */
    public Stream<Pair<String, Node>> addedMetadata() {
        return newModel.getMetadata()
                .entrySet()
                .stream()
                .filter(entry -> !oldModel.getMetadata().containsKey(entry.getKey()))
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
    }

    /**
     * Gets all removed shapes.
     *
     * @return Returns a stream of each removed shape.
     */
    public Stream<Shape> removedShapes() {
        return removedShapes.stream();
    }

    /**
     * Gets all of the removed shapes of a specific type.
     *
     * @param shapeType Type of shape to find.
     * @param <T> Type of shape.
     * @return Returns a stream of each removed shape of a specific type.
     */
    public <T extends Shape> Stream<T> removedShapes(Class<T> shapeType) {
        return removedShapes().filter(shapeType::isInstance).map(shapeType::cast);
    }

    /**
     * Gets all removed metadata.
     *
     * <p>Each Pair returned contains the name of the metadata key on
     * the left of the Pair and the metadata value on the right.
     *
     * @return Returns a stream of removed metadata.
     */
    public Stream<Pair<String, Node>> removedMetadata() {
        return removedMetadata.stream();
    }

    /**
     * Gets all changed shapes.
     *
     * @return Returns a stream of changed shapes.
     */
    public Stream<ChangedShape<Shape>> changedShapes() {
        return changedShapes.stream();
    }

    /**
     * Gets all changed shapes of a specific type.
     *
     * @param type Type of shape to find.
     * @param <T> Type of shape.
     * @return Returns a stream of matching changed shapes.
     */
    @SuppressWarnings("unchecked")
    public <T extends Shape> Stream<ChangedShape<T>> changedShapes(Class<T> type) {
        return changedShapes()
                .filter(change -> type.isInstance(change.getOldShape()) && type.isInstance(change.getNewShape()))
                .map(change -> (ChangedShape<T>) change);
    }

    /**
     * Gets a stream of all changed metadata.
     *
     * @return Returns the changed metadata.
     */
    public Stream<ChangedMetadata> changedMetadata() {
        return changedMetadata.stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Differences)) {
            return false;
        } else {
            // The differences between the models are always equivalent if the
            // models are equivalent, so no need to compare them.
            Differences that = (Differences) o;
            return getOldModel().equals(that.getOldModel()) && getNewModel().equals(that.getNewModel());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOldModel(), getNewModel());
    }


    /**
     * Constructs a Builder for {@link Differences}.
     *
     * <p>For most uses of {@link Differences}, it should be constructed with {@link Differences#detect}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<Differences> toBuilder() {
        return builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .addedShapes(addedShapes)
                .removedShapes(removedShapes)
                .changedShapes(changedShapes)
                .addedMetadata(addedMetadata)
                .removedMetadata(removedMetadata)
                .changedMetadata(changedMetadata);
    }

    /**
     * A Builder for {@link Differences}.
     *
     * <p>For most uses of {@link Differences}, it should be constructed with {@link Differences#detect}.
     *
     * <p>This is intended to be used for evaluating subsets of the models or synthetic
     * differences between them. For example, two completely different shapes could be
     * evaluated against each other to see what the differences are.
     */
    public static final class Builder implements SmithyBuilder<Differences> {
        private Model oldModel;
        private Model newModel;

        private final BuilderRef<List<Shape>> addedShapes = BuilderRef.forList();
        private final BuilderRef<List<Shape>> removedShapes = BuilderRef.forList();
        private final BuilderRef<List<ChangedShape<Shape>>> changedShapes = BuilderRef.forList();

        private final BuilderRef<List<Pair<String, Node>>> addedMetadata = BuilderRef.forList();
        private final BuilderRef<List<Pair<String, Node>>> removedMetadata = BuilderRef.forList();
        private final BuilderRef<List<ChangedMetadata>> changedMetadata = BuilderRef.forList();

        @Override
        public Differences build() {
            return new Differences(this);
        }

        /**
         * Sets the model to be used as the base model.
         *
         * @param oldModel The model to base changes on.
         * @return Returns the builder.
         */
        public Builder oldModel(Model oldModel) {
            this.oldModel = oldModel;
            return this;
        }

        /**
         * Sets the model to be used as the new model state.
         *
         * @param newModel The model to use as the new state.
         * @return Returns the builder.
         */
        public Builder newModel(Model newModel) {
            this.newModel = newModel;
            return this;
        }

        /**
         * Sets what shapes have been added.
         *
         * <p>For most uses, {@link #detectShapeChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param addedShapes The shapes to consider as having been added.
         * @return Returns the builder.
         */
        public Builder addedShapes(Collection<Shape> addedShapes) {
            this.addedShapes.clear();
            this.addedShapes.get().addAll(addedShapes);
            return this;
        }

        /**
         * Sets what shapes have been removed.
         *
         * <p>For most uses, {@link #detectShapeChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param removedShapes The shapes to consider as having been removed.
         * @return Returns the builder.
         */
        public Builder removedShapes(Collection<Shape> removedShapes) {
            this.removedShapes.clear();
            this.removedShapes.get().addAll(removedShapes);
            return this;
        }

        /**
         * Sets what shapes have been changed.
         *
         * <p>For most uses, {@link #detectShapeChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param changedShapes The shapes to consider as having changed.
         * @return Returns the builder.
         */
        public Builder changedShapes(Collection<ChangedShape<Shape>> changedShapes) {
            this.changedShapes.clear();
            this.changedShapes.get().addAll(changedShapes);
            return this;
        }

        /**
         * Adds a shape to the set of shapes that have been changed.
         *
         * <p>For most uses, {@link #detectShapeChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param changedShape A shape to consider as having changed.
         * @return Returns the builder.
         */
        public Builder changedShape(ChangedShape<Shape> changedShape) {
            this.changedShapes.get().add(changedShape);
            return this;
        }

        /**
         * Sets the metadata that is considered to have been added.
         *
         * <p>For most uses, {@link #detectMetadataChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param addedMetadata The metadata to consider as having been added.
         * @return Returns the builder.
         */
        public Builder addedMetadata(Collection<Pair<String, Node>> addedMetadata) {
            this.addedMetadata.clear();
            this.addedMetadata.get().addAll(addedMetadata);
            return this;
        }

        /**
         * Sets the metadata that is considered to have been removed.
         *
         * <p>For most uses, {@link #detectMetadataChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param removedMetadata The metadata to consider as having been removed.
         * @return Returns the builder.
         */
        public Builder removedMetadata(Collection<Pair<String, Node>> removedMetadata) {
            this.removedMetadata.clear();
            this.removedMetadata.get().addAll(removedMetadata);
            return this;
        }

        /**
         * Sets the metadata that is considered to have been changed.
         *
         * <p>For most uses, {@link #detectMetadataChanges()} or {@link Differences#detect(Model, Model)}
         * should be used instead.
         *
         * @param changedMetadata The metadata to consider as having been changed.
         * @return Returns the builder.
         */
        public Builder changedMetadata(Collection<ChangedMetadata> changedMetadata) {
            this.changedMetadata.clear();
            this.changedMetadata.get().addAll(changedMetadata);
            return this;
        }

        /**
         * Detects all shape additions, removals, and changes.
         *
         * @return Returns the builder.
         */
        public Builder detectShapeChanges() {
            addedShapes.clear();
            removedShapes.clear();
            changedShapes.clear();
            for (Shape oldShape : oldModel.toSet()) {
                Optional<Shape> newShape = newModel.getShape(oldShape.getId());
                if (newShape.isPresent()) {
                    if (!oldShape.equals(newShape.get())) {
                        changedShapes.get().add(new ChangedShape<>(oldShape, newShape.get()));
                    }
                } else {
                    removedShapes.get().add(oldShape);
                }
            }

            for (Shape newShape : newModel.toSet()) {
                if (!oldModel.getShape(newShape.getId()).isPresent()) {
                    addedShapes.get().add(newShape);
                }
            }

            return this;
        }

        /**
         * Detects all metadata additions, removals, and changes.
         *
         * @return Returns the builder.
         */
        public Builder detectMetadataChanges() {
            addedMetadata.clear();
            removedMetadata.clear();
            changedMetadata.clear();
            for (Map.Entry<String, Node> entry : oldModel.getMetadata().entrySet()) {
                String k = entry.getKey();
                Node v = entry.getValue();
                if (newModel.getMetadata().containsKey(k)) {
                    if (!newModel.getMetadata().get(k).equals(v)) {
                        changedMetadata.get().add(new ChangedMetadata(k, v, newModel.getMetadata().get(k)));
                    }
                } else {
                    removedMetadata.get().add(Pair.of(k, v));
                }
            }

            for (Map.Entry<String, Node> entry : newModel.getMetadata().entrySet()) {
                if (!oldModel.getMetadata().containsKey(entry.getKey())) {
                    addedMetadata.get().add(Pair.of(entry.getKey(), entry.getValue()));
                }
            }

            return this;
        }
    }
}
