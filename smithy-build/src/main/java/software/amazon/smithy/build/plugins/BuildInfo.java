/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * POJO to represents a smithy-build-info.json file.
 */
public final class BuildInfo {
    private String version = SmithyBuild.VERSION;
    private String projectionName = "source";
    private ProjectionConfig projection;
    private List<ValidationEvent> validationEvents = Collections.emptyList();
    private List<ShapeId> traitNames = Collections.emptyList();
    private List<ShapeId> traitDefNames = Collections.emptyList();
    private List<ShapeId> serviceShapeIds = Collections.emptyList();
    private List<ShapeId> operationShapeIds = Collections.emptyList();
    private List<ShapeId> resourceShapeIds = Collections.emptyList();
    private Map<String, Node> metadata = Collections.emptyMap();

    /**
     * @return Gets the version of the build-info file format.
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return Gets the name of the projection used in this build.
     */
    public String getProjectionName() {
        return projectionName;
    }

    public void setProjectionName(String projectionName) {
        this.projectionName = projectionName;
    }

    /**
     * @return Gets the projection configuration.
     */
    public ProjectionConfig getProjection() {
        return projection;
    }

    public void setProjection(ProjectionConfig projection) {
        this.projection = projection;
    }

    /**
     * @return Gets the validation events encountered by the projection.
     */
    public List<ValidationEvent> getValidationEvents() {
        return validationEvents;
    }

    public void setValidationEvents(List<ValidationEvent> validationEvents) {
        this.validationEvents = validationEvents;
    }

    /**
     * @return Gets the shape ID of every trait used in the projected model.
     */
    public List<ShapeId> getTraitNames() {
        return traitNames;
    }

    public void setTraitNames(List<ShapeId> traitNames) {
        this.traitNames = traitNames;
    }

    /**
     * @return Gets the shape ID of every trait shape defined in the projection.
     */
    public List<ShapeId> getTraitDefNames() {
        return traitDefNames;
    }

    public void setTraitDefNames(List<ShapeId> traitDefNames) {
        this.traitDefNames = traitDefNames;
    }

    /**
     * @return Gets the shape ID of every service in the projection.
     */
    public List<ShapeId> getServiceShapeIds() {
        return serviceShapeIds;
    }

    public void setServiceShapeIds(List<ShapeId> serviceShapeIds) {
        this.serviceShapeIds = serviceShapeIds;
    }

    /**
     * @return Gets the shape ID of every operation in the projection.
     */
    public List<ShapeId> getOperationShapeIds() {
        return operationShapeIds;
    }

    public void setOperationShapeIds(List<ShapeId> operationShapeIds) {
        this.operationShapeIds = operationShapeIds;
    }

    /**
     * @return Gets the shape ID of every resource in the projection.
     */
    public List<ShapeId> getResourceShapeIds() {
        return resourceShapeIds;
    }

    public void setResourceShapeIds(List<ShapeId> resourceShapeIds) {
        this.resourceShapeIds = resourceShapeIds;
    }

    /**
     * @return Gets the model metadata in the projection.
     */
    public Map<String, Node> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Node> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof BuildInfo)) {
            return false;
        }

        BuildInfo buildInfo = (BuildInfo) o;
        return Objects.equals(getVersion(), buildInfo.getVersion())
                && Objects.equals(getProjectionName(), buildInfo.getProjectionName())
                && Objects.equals(getProjection(), buildInfo.getProjection())
                && Objects.equals(getValidationEvents(), buildInfo.getValidationEvents())
                && Objects.equals(getTraitNames(), buildInfo.getTraitNames())
                && Objects.equals(getTraitDefNames(), buildInfo.getTraitDefNames())
                && Objects.equals(getServiceShapeIds(), buildInfo.getServiceShapeIds())
                && Objects.equals(getOperationShapeIds(), buildInfo.getOperationShapeIds())
                && Objects.equals(getResourceShapeIds(), buildInfo.getResourceShapeIds())
                && Objects.equals(getMetadata(), buildInfo.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVersion(),
                getProjectionName(),
                getProjection(),
                getValidationEvents(),
                getTraitNames(),
                getTraitDefNames(),
                getServiceShapeIds(),
                getOperationShapeIds(),
                getResourceShapeIds(),
                getMetadata());
    }
}
