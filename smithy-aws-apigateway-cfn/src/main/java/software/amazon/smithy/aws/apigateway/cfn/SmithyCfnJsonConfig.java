/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Configuration for the {@code smithy-cfn-json} plugin.
 *
 * <p>Example {@code smithy-build.json} configuration:
 * <pre>{@code
 * {
 *   "plugins": {
 *     "smithy-cfn-json": {
 *       "service": "com.example#MyService",
 *       "disableCloudFormationSubstitution": false
 *     }
 *   }
 * }
 * }</pre>
 */
public final class SmithyCfnJsonConfig {

    private ShapeId service;
    private boolean disableCloudFormationSubstitution;
    private Map<String, Node> jsonAdd = java.util.Collections.emptyMap();

    /**
     * Gets the service shape ID to export.
     *
     * @return Returns the service shape ID.
     */
    public ShapeId getService() {
        return service;
    }

    public void setService(ShapeId service) {
        this.service = service;
    }

    /**
     * Gets whether CloudFormation substitution is disabled.
     *
     * @return Returns true if substitution is disabled.
     */
    public boolean getDisableCloudFormationSubstitution() {
        return disableCloudFormationSubstitution;
    }

    public void setDisableCloudFormationSubstitution(boolean disableCloudFormationSubstitution) {
        this.disableCloudFormationSubstitution = disableCloudFormationSubstitution;
    }

    /**
     * Gets the JSON Pointer patches to apply to the output.
     *
     * @return Returns the jsonAdd map.
     */
    public Map<String, Node> getJsonAdd() {
        return jsonAdd;
    }

    public void setJsonAdd(Map<String, Node> jsonAdd) {
        this.jsonAdd = java.util.Objects.requireNonNull(jsonAdd);
    }
}
