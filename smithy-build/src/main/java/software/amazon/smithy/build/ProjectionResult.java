/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.BuilderRef;
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
        this.events = builder.events.copy();
        this.pluginManifests = builder.pluginManifests.copy();
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
        for (ValidationEvent e : events) {
            if (e.getSeverity() == Severity.ERROR || e.getSeverity() == Severity.DANGER) {
                return true;
            }
        }
        return false;
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
     * Gets the result of a specific plugin by plugin artifact name.
     *
     * <p>If no artifact name is configured for a plugin in smithy-build.json (e.g., "plugin::artifact"), the
     * artifact name defaults to the plugin name.
     *
     * @param artifactName Name of the plugin artifact to retrieve.
     * @return Returns files created by the given plugin or an empty list.
     */
    public Optional<FileManifest> getPluginManifest(String artifactName) {
        return Optional.ofNullable(pluginManifests.get(artifactName));
    }

    /**
     * Builds up a {@link ProjectionResult}.
     */
    public static final class Builder implements SmithyBuilder<ProjectionResult> {
        private String projectionName;
        private Model model;
        private final BuilderRef<Map<String, FileManifest>> pluginManifests = BuilderRef.forUnorderedMap();
        private final BuilderRef<List<ValidationEvent>> events = BuilderRef.forList();

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
         * Adds an artifact result for a plugin.
         *
         * <p>If no artifact name is configured for a plugin in smithy-build.json (e.g., "plugin::artifact"), the
         * artifact name defaults to the plugin name.
         *
         * @param artifactName Name of the plugin artifact to set.
         * @param manifest File manifest used by the plugin.
         * @return Returns the builder.
         */
        public Builder addPluginManifest(String artifactName, FileManifest manifest) {
            pluginManifests.get().put(artifactName, manifest);
            return this;
        }

        /**
         * Adds validation events to the result.
         *
         * @param event Validation event to add.
         * @return Returns the builder.
         */
        public Builder addEvent(ValidationEvent event) {
            events.get().add(Objects.requireNonNull(event));
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
