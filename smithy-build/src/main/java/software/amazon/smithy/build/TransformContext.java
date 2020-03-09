/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.build;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.SetUtils;
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
    private final Set<String> visited;

    private TransformContext(Builder builder) {
        model = SmithyBuilder.requiredState("model", builder.model);
        transformer = builder.transformer != null ? builder.transformer : ModelTransformer.create();
        settings = builder.settings;
        originalModel = builder.originalModel;
        sources = SetUtils.copyOf(builder.sources);
        projectionName = builder.projectionName;
        visited = new LinkedHashSet<>(builder.visited);
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
                .visited(visited);
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
     * Gets the set of previously visited transforms.
     *
     * <p>This method is used as bookkeeping for the {@code apply}
     * plugin to detect cycles.
     *
     * @return Returns the ordered set of visited projections.
     */
    public Set<String> getVisited() {
        return visited;
    }

    /**
     * Builds a {@link TransformContext}.
     */
    public static final class Builder implements SmithyBuilder<TransformContext> {

        private ObjectNode settings = Node.objectNode();
        private Model model;
        private Model originalModel;
        private Set<Path> sources = Collections.emptySet();
        private String projectionName = "source";
        private ModelTransformer transformer;
        private Set<String> visited = Collections.emptySet();

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
            this.sources = Objects.requireNonNull(sources);
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

        public Builder visited(Set<String> visited) {
            this.visited = visited;
            return this;
        }
    }
}
