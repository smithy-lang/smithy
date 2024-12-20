/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Context object used when applying a {@link ProjectionTransformer}.
 *
 * <p>Implementer's note: A context object is used to allow contextual
 * information provided to projection transforms to evolve over time.
 */
public final class TransformContext implements ToSmithyBuilder<TransformContext> {

    private final ObjectNode settings;
    private final Model model;
    private final Model originalModel;
    private final Set<Path> sources;
    private final String projectionName;
    private final ModelTransformer transformer;
    private final List<ValidationEvent> originalModelValidationEvents;

    private TransformContext(Builder builder) {
        model = SmithyBuilder.requiredState("model", builder.model);
        transformer = builder.transformer != null ? builder.transformer : ModelTransformer.create();
        settings = builder.settings;
        originalModel = builder.originalModel;
        projectionName = builder.projectionName;
        sources = builder.sources.copy();
        originalModelValidationEvents = builder.originalModelValidationEvents.copy();
    }

    /**
     * @return Returns a TransformContext builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .settings(settings)
                .model(model)
                .originalModel(originalModel)
                .sources(sources)
                .projectionName(projectionName)
                .transformer(transformer)
                .originalModelValidationEvents(originalModelValidationEvents);
    }

    /**
     * Gets the arguments object of the transform.
     *
     * @return Returns the transformer arguments.
     */
    public ObjectNode getSettings() {
        return settings;
    }

    /**
     * Gets the model to transform.
     *
     * @return Returns the model to transform.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Get the original model before applying the projection.
     *
     * @return The optionally provided original model.
     */
    public Optional<Model> getOriginalModel() {
        return Optional.ofNullable(originalModel);
    }

    /**
     * Gets the source models, or models that are considered the subject
     * of the build.
     *
     * <p>This does not return an exhaustive set of model paths! There are
     * typically two kinds of models that are added to a build: source
     * models and discovered models. Discovered models are someone else's
     * models. Source models are the models owned by the package being built.
     *
     * @return Returns the source models.
     */
    public Set<Path> getSources() {
        return sources;
    }

    /**
     * Gets the name of the projection being applied.
     *
     * <p>If no projection could be found, "source" is assumed.
     *
     * @return Returns the explicit or assumed projection name.
     */
    public String getProjectionName() {
        return projectionName;
    }

    /**
     * Gets the {@code ModelTransformer} that has been configured to aid
     * in the transformation.
     *
     * @return Returns the model transformer.
     */
    public ModelTransformer getTransformer() {
        return transformer;
    }

    /**
     * Gets an immutable list of {@link ValidationEvent}s that were
     * encountered when loading the source model.
     *
     * @return Returns the encountered validation events.
     */
    public List<ValidationEvent> getOriginalModelValidationEvents() {
        return originalModelValidationEvents;
    }

    /**
     * Builds a {@link TransformContext}.
     */
    public static final class Builder implements SmithyBuilder<TransformContext> {

        private ObjectNode settings = Node.objectNode();
        private Model model;
        private Model originalModel;
        private BuilderRef<Set<Path>> sources = BuilderRef.forOrderedSet();
        private String projectionName = "source";
        private ModelTransformer transformer;
        private final BuilderRef<List<ValidationEvent>> originalModelValidationEvents = BuilderRef.forList();

        private Builder() {}

        @Override
        public TransformContext build() {
            return new TransformContext(this);
        }

        public Builder settings(ObjectNode settings) {
            this.settings = Objects.requireNonNull(settings);
            return this;
        }

        public Builder model(Model model) {
            this.model = Objects.requireNonNull(model);
            return this;
        }

        public Builder originalModel(Model originalModel) {
            this.originalModel = originalModel;
            return this;
        }

        public Builder sources(Set<Path> sources) {
            this.sources.clear();
            this.sources.get().addAll(sources);
            return this;
        }

        public Builder projectionName(String projectionName) {
            this.projectionName = Objects.requireNonNull(projectionName);
            return this;
        }

        public Builder transformer(ModelTransformer transformer) {
            this.transformer = transformer;
            return this;
        }

        public Builder originalModelValidationEvents(List<ValidationEvent> originalModelValidationEvents) {
            this.originalModelValidationEvents.clear();
            this.originalModelValidationEvents.get().addAll(originalModelValidationEvents);
            return this;
        }
    }
}
