/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.traits.CfnResource;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Smithy to CloudFormation conversion context object.
 *
 * <p>One context is used per CloudFormation resource generated.
 */
public final class Context {

    private final Model model;
    private final ServiceShape service;
    private final ResourceShape resource;
    private final CfnResource cfnResource;
    private final StructureShape resourceStructure;
    private final JsonSchemaConverter jsonSchemaConverter;
    private final CfnConfig config;

    Context(
            Model model,
            ServiceShape service,
            ResourceShape resource,
            CfnResource cfnResource,
            StructureShape resourceStructure,
            CfnConfig config,
            JsonSchemaConverter jsonSchemaConverter
    ) {
        this.model = model;
        this.service = service;
        this.resource = resource;
        this.cfnResource = cfnResource;
        this.resourceStructure = resourceStructure;
        this.config = config;
        this.jsonSchemaConverter = jsonSchemaConverter;
    }

    /**
     * Gets the Smithy model being converted.
     *
     * @return Returns the Smithy model.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Gets the service shape containing the resource being converted.
     *
     * @return Returns the service shape.
     */
    public ServiceShape getService() {
        return service;
    }

    /**
     * Gets the resource shape being converted.
     *
     * @return Returns the resource shape.
     */
    public ResourceShape getResource() {
        return resource;
    }

    /**
     * Gets the {@link CfnResource} index data for this resource.
     *
     * @return Returns the CfnResource index data.
     */
    public CfnResource getCfnResource() {
        return cfnResource;
    }

    /**
     * Gets the structure shape that represents the consolidated properties of the resource.
     *
     * @return Returns the structure shape.
     */
    public StructureShape getResourceStructure() {
        return resourceStructure;
    }

    /**
     * Gets the configuration object used for the conversion.
     *
     * <p>Plugins can query this object for configuration values.
     *
     * @return Returns the configuration object.
     */
    public CfnConfig getConfig() {
        return config;
    }

    /**
     * Gets the JSON schema converter.
     *
     * @return Returns the JSON Schema converter.
     */
    public JsonSchemaConverter getJsonSchemaConverter() {
        return jsonSchemaConverter;
    }

    /**
     * Gets the JSON pointer string to a specific property.
     *
     * @param propertyName Property name to build a JSON pointer to.
     * @return Returns the JSON pointer to the property.
     */
    public String getPropertyPointer(String propertyName) {
        MemberShape member = resourceStructure.getMember(propertyName).get();
        return "/properties/" + getJsonSchemaConverter().toPropertyName(member);
    }
}
