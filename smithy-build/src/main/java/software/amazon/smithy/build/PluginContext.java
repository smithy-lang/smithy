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

package software.amazon.smithy.build;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Context object used in plugin execution.
 */
public final class PluginContext {
    private final Projection projection;
    private final Model model;
    private final Model originalModel;
    private final List<ValidationEvent> events;
    private final ObjectNode settings;
    private final FileManifest fileManifest;
    private final ClassLoader pluginClassLoader;

    private PluginContext(Builder builder) {
        this.model = SmithyBuilder.requiredState("model", builder.model);
        this.fileManifest = SmithyBuilder.requiredState("fileManifest", builder.fileManifest);
        this.projection = builder.projection;
        this.originalModel = builder.originalModel;
        this.events = Collections.unmodifiableList(builder.events);
        this.settings = builder.settings;
        this.pluginClassLoader = builder.pluginClassLoader;
    }

    /**
     * Creates a new PluginContext Builder.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Get the projection the plugin is optionally attached to.
     */
    public Optional<Projection> getProjection() {
        return Optional.ofNullable(projection);
    }

    /**
     * Gets the model that was projected.
     *
     * @return Get the projected model.
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
     * Gets the validation events encountered after projecting the model.
     *
     * @return Get the validation events that were encountered.
     */
    public List<ValidationEvent> getEvents() {
        return events;
    }

    /**
     * Gets the plugin configuration settings.
     *
     * @return Plugins settings object.
     */
    public ObjectNode getSettings() {
        return settings;
    }

    /**
     * Gets the FileManifest used to create files in the projection.
     *
     * <p>All files written by a plugin should either be written using this
     * manifest or added to the manifest via {@link FileManifest#addFile}.
     *
     * @return Returns the file manifest.
     */
    public FileManifest getFileManifest() {
        return fileManifest;
    }

    /**
     * Gets the ClassLoader that should be used in build plugins to load
     * services.
     *
     * @return Returns the optionally set ClassLoader.
     */
    public Optional<ClassLoader> getPluginClassLoader() {
        return Optional.ofNullable(pluginClassLoader);
    }

    /**
     * Builds a {@link PluginContext}.
     */
    public static final class Builder implements SmithyBuilder<PluginContext> {
        private Projection projection;
        private Model model;
        private Model originalModel;
        private List<ValidationEvent> events = Collections.emptyList();
        private ObjectNode settings = Node.objectNode();
        private FileManifest fileManifest;
        private ClassLoader pluginClassLoader;

        private Builder() {}

        @Override
        public PluginContext build() {
            return new PluginContext(this);
        }

        /**
         * Sets the <strong>required</strong> {@link FileManifest} to use
         * in the plugin.
         *
         * @param fileManifest FileManifest to use.
         * @return Returns the builder.
         */
        public Builder fileManifest(FileManifest fileManifest) {
            this.fileManifest = fileManifest;
            return this;
        }

        /**
         * Sets the <strong>required</strong> model that is being built.
         *
         * @param model Model to set.
         * @return Returns the builder.
         */
        public Builder model(Model model) {
            this.model = Objects.requireNonNull(model);
            return this;
        }

        /**
         * Sets the projection that the plugin belongs to.
         *
         * @param projection Projection to set.
         * @return Returns the builder.
         */
        public Builder projection(Projection projection) {
            this.projection = Objects.requireNonNull(projection);
            return this;
        }

        /**
         * Sets the model that is being built before it was transformed in
         * the projection.
         *
         * @param originalModel Original Model to set.
         * @return Returns the builder.
         */
        public Builder originalModel(Model originalModel) {
            this.originalModel = Objects.requireNonNull(originalModel);
            return this;
        }

        /**
         * Sets the validation events that occurred after projecting the model.
         *
         * @param events Validation events to set.
         * @return Returns the builder.
         */
        public Builder events(List<ValidationEvent> events) {
            this.events = Objects.requireNonNull(events);
            return this;
        }

        /**
         * Sets the settings of the plugin.
         *
         * @param settings Settings to set.
         * @return Returns the builder.
         */
        public Builder settings(ObjectNode settings) {
            this.settings = Objects.requireNonNull(settings);
            return this;
        }

        /**
         * Sets a ClassLoader that should be used by build plugins when loading
         * services.
         *
         * @param pluginClassLoader ClassLoader to use in build plugins.
         * @return Retruns the builder.
         */
        public Builder pluginClassLoader(ClassLoader pluginClassLoader) {
            this.pluginClassLoader = pluginClassLoader;
            return this;
        }
    }
}
