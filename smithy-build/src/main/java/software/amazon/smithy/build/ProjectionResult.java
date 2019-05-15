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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * The result of applying a projection to a model.
 */
public final class ProjectionResult {
    private final String projectionName;
    private final Model model;
    private final Map<String, FileManifest> pluginManifests;
    private final List<ValidationEvent> events;

    private ProjectionResult(Builder builder) {
        this.projectionName = SmithyBuilder.requiredState("projectionName", builder.projectionName);
        this.model = SmithyBuilder.requiredState("model", builder.model);
        this.events = ListUtils.copyOf(builder.events);
        this.pluginManifests = MapUtils.copyOf(builder.pluginManifests);
    }

    /**
     * Creates a {@link ProjectionResult} builder.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the name of the projection that was executed.
     *
     * @return Returns the projection name.
     */
    public String getProjectionName() {
        return projectionName;
    }

    /**
     * Gets the result of projecting the model.
     *
     * @return Returns the projected model.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Returns true if the projection contains error or danger events.
     *
     * @return Returns true if the projected model is broken.
     */
    public boolean isBroken() {
        return events.stream().anyMatch(e -> e.getSeverity() == Severity.ERROR || e.getSeverity() == Severity.DANGER);
    }

    /**
     * Gets the validation events encountered after projecting the model.
     *
     * @return Returns the encountered validation events.
     */
    public List<ValidationEvent> getEvents() {
        return events;
    }

    /**
     * Gets the results of each plugin.
     *
     * @return Returns a map of plugin names to their file manifests.
     */
    public Map<String, FileManifest> getPluginManifests() {
        return pluginManifests;
    }

    /**
     * Gets the result of a specific plugin.
     *
     * @param pluginName Name of the plugin to retrieve.
     * @return Returns the files created by the given plugin or an empty list.
     */
    public Optional<FileManifest> getPluginManifest(String pluginName) {
        return Optional.ofNullable(pluginManifests.get(pluginName));
    }

    /**
     * Builds up a {@link ProjectionResult}.
     */
    public static final class Builder implements SmithyBuilder<ProjectionResult> {
        private String projectionName;
        private Model model;
        private final Map<String, FileManifest> pluginManifests = new HashMap<>();
        private final Collection<ValidationEvent> events = new ArrayList<>();

        @Override
        public ProjectionResult build() {
            return new ProjectionResult(this);
        }

        /**
         * Sets the <strong>required</strong> model that was projected.
         *
         * @param model Model to set.
         * @return Returns the builder.
         */
        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the <strong>required</strong> projection name.
         *
         * @param projectionName ProjectionConfig name to set.
         * @return Returns the builder.
         */
        public Builder projectionName(String projectionName) {
            this.projectionName = projectionName;
            return this;
        }

        /**
         * Adds a plugin result.
         *
         * @param pluginName Name of the plugin.
         * @param manifest File manifest used by the plugin.
         * @return Returns the builder.
         */
        public Builder addPluginManifest(String pluginName, FileManifest manifest) {
            pluginManifests.put(pluginName, manifest);
            return this;
        }

        /**
         * Adds validation events to the result.
         *
         * @param event Validation event to add.
         * @return Returns the builder.
         */
        public Builder addEvent(ValidationEvent event) {
            events.add(Objects.requireNonNull(event));
            return this;
        }

        /**
         * Sets the validation events of the projection.
         *
         * @param events Validation events to set.
         * @return Returns the builder.
         */
        public Builder events(List<ValidationEvent> events) {
            this.events.clear();
            events.forEach(this::addEvent);
            return this;
        }
    }
}
