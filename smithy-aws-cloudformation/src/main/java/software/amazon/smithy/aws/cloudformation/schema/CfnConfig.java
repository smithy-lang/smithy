/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.jsonschema.JsonSchemaVersion;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

/**
 * "cloudformation" smithy-build plugin configuration settings.
 */
public final class CfnConfig extends JsonSchemaConfig {

    /** The JSON pointer to where CloudFormation schema shared resource properties should be written. */
    public static final String SCHEMA_COMPONENTS_POINTER = "#/definitions";

    private boolean disableHandlerPermissionGeneration = false;
    private boolean disableDeprecatedPropertyGeneration = false;
    private boolean disableRequiredPropertyGeneration = false;
    private boolean disableCapitalizedProperties = false;
    private List<String> externalDocs = ListUtils.of(
            "Documentation Url",
            "DocumentationUrl",
            "API Reference",
            "User Guide",
            "Developer Guide",
            "Reference",
            "Guide");
    private Map<ShapeId, Map<String, Node>> jsonAdd = Collections.emptyMap();
    private String organizationName;
    private String serviceName;
    private List<String> sourceDocs = ListUtils.of(
            "Source Url",
            "SourceUrl",
            "Source",
            "Source Code");

    public CfnConfig() {
        super();

        // CloudFormation Resource Schemas MUST use alphanumeric only references.
        // Invoke the parent class's method directly since we override it to lock
        // this functionality.
        //
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L303
        super.setAlphanumericOnlyRefs(true);

        setDefinitionPointer(SCHEMA_COMPONENTS_POINTER);

        // CloudFormation Resource Schemas MUST use the patternProperties schema
        // property for maps. Invoke the parent class's method directly since
        // we override it to lock this functionality.
        //
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L166-L177
        super.setMapStrategy(MapStrategy.PATTERN_PROPERTIES);

        //
        // CloudFormation Resource Schemas MUST use the oneOf schema property for
        // unions. Invoke the parent class's method directly since we override it
        // to lock this functionality.
        //
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L210
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L166
        super.setUnionStrategy(UnionStrategy.ONE_OF);

        // @cfnResource's additionalSchemas property references shapes that aren't in the service closure
        // so conversions must be able to reference those shapes
        super.setEnableOutOfServiceReferences(true);
    }

    @Override
    public void setAlphanumericOnlyRefs(boolean alphanumericOnlyRefs) {
        // CloudFormation Resource Schemas MUST use alphanumeric only references.
        // Throw if customers tried to set it to false.
        //
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L303
        if (!alphanumericOnlyRefs) {
            throw new CfnException("CloudFormation Resource Schemas MUST use alphanumeric only "
                    + "references. `alphanumericOnlyRefs` value of `false` was provided.");
        }
    }

    public boolean getDisableHandlerPermissionGeneration() {
        return disableHandlerPermissionGeneration;
    }

    /**
     * Set to true to disable generating {@code handler} property's {@code permissions}
     * lists for Resource Schemas.
     *
     * <p>By default, handler permissions are automatically added to the {@code handler}
     * property's {@code permissions} list. This includes the lifecycle operation used
     * and any permissions listed in the {@code aws.iam#requiredActions} trait.
     *
     * @param disableHandlerPermissionGeneration True to disable handler {@code permissions}
     *   generation
     */
    public void setDisableHandlerPermissionGeneration(boolean disableHandlerPermissionGeneration) {
        this.disableHandlerPermissionGeneration = disableHandlerPermissionGeneration;
    }

    public boolean getDisableDeprecatedPropertyGeneration() {
        return disableDeprecatedPropertyGeneration;
    }

    /**
     * Set to true to disable generating {@code deprecatedProperties} for Resource Schemas.
     *
     * <p>By default, deprecated members are automatically added to the
     * {@code deprecatedProperties} schema property.
     *
     * @param disableDeprecatedPropertyGeneration True to disable {@code deprecatedProperties}
     *   generation, false otherwise.
     */
    public void setDisableDeprecatedPropertyGeneration(boolean disableDeprecatedPropertyGeneration) {
        this.disableDeprecatedPropertyGeneration = disableDeprecatedPropertyGeneration;
    }

    public boolean getDisableRequiredPropertyGeneration() {
        return disableRequiredPropertyGeneration;
    }

    /**
     * Set to true to disable generating {@code required} for Resource Schemas.
     *
     * <p>By default, required members are automatically added to the
     * {@code required} schema property.
     *
     * @param disableRequiredPropertyGeneration True to disable {@code required}
     *   generation, false otherwise.
     */
    public void setDisableRequiredPropertyGeneration(boolean disableRequiredPropertyGeneration) {
        this.disableRequiredPropertyGeneration = disableRequiredPropertyGeneration;
    }

    public boolean getDisableCapitalizedProperties() {
        return disableCapitalizedProperties;
    }

    /**
     * Set to true to disable automatically capitalizing names of properties
     * of Resource Schemas.
     *
     * <p>By default, property names of Resource Schemas are capitalized if
     * no {@code cfnName} trait is applied.
     *
     * @param disableCapitalizedProperties True to disable capitalizing property names,
     *   false otherwise.
     */
    public void setDisableCapitalizedProperties(boolean disableCapitalizedProperties) {
        this.disableCapitalizedProperties = disableCapitalizedProperties;
    }

    public List<String> getExternalDocs() {
        return externalDocs;
    }

    /**
     * Limits the source of converted "externalDocs" fields to the specified
     * priority ordered list of names in an externalDocumentation trait.
     *
     * <p>This list is case insensitive. By default, this is a list of the
     * following values: "Documentation Url", "DocumentationUrl", "API Reference",
     * "User Guide", "Developer Guide", "Reference", and "Guide".
     *
     * @param externalDocs External docs to look for and convert, in order.
     */
    public void setExternalDocs(List<String> externalDocs) {
        this.externalDocs = externalDocs;
    }

    public Map<ShapeId, Map<String, Node>> getJsonAdd() {
        return jsonAdd;
    }

    /**
     * Adds or replaces the JSON value in the generated resource schema
     * document at the given JSON pointer locations with a different JSON
     * value.
     *
     * <p>The value must be a map where each key is a resource shape ID. The
     * value is a map where each key is a valid JSON pointer string as defined
     * in RFC 6901. Each value in the nested map is the JSON value to add or
     * replace at the given target.
     *
     * <p>Values are added using similar semantics of the "add" operation
     * of JSON Patch, as specified in RFC 6902, with the exception that
     * adding properties to an undefined object will create nested
     * objects in the result as needed.
     *
     * @param jsonAdd Map of JSON path to values to patch in.
     */
    public void setJsonAdd(Map<ShapeId, Map<String, Node>> jsonAdd) {
        this.jsonAdd = Objects.requireNonNull(jsonAdd);
    }

    @Override
    public void setUseJsonName(boolean useJsonName) {
        // CloudFormation Resource Schemas use a separate strategy, via @cfnName,
        // for naming JSON Schema properties for structures and unions. Throw if
        // customers tried to set it at all.
        //
        // See CfnConverter::getPropertyNamingStrategy
        throw new CfnException(String.format("CloudFormation Resource Schemas use the `@cfnName` trait for "
                + "naming JSON Schema properties for structures and unions. `useJsonName` value of `%b` was provided.",
                useJsonName));
    }

    @Override
    public void setMapStrategy(MapStrategy mapStrategy) {
        // CloudFormation Resource Schemas MUST use the patternProperties schema
        // property for maps, which was already set in the constructor. Throw if
        // customers tried to set it to another MapStrategy.
        //
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L166-L177
        if (mapStrategy != MapStrategy.PATTERN_PROPERTIES) {
            throw new CfnException(String.format("CloudFormation Resource Schemas require the use of "
                    + "`patternProperties` for defining maps in JSON Schema. `mapStrategy` value of `%s` was provided.",
                    mapStrategy));
        }
    }

    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Sets the "Organization" component for each of the generated resource's
     * type name.
     *
     * <p>This value defaults to "AWS" if the {@code aws.api#service} trait is
     * present. Otherwise, the value is required configuration.
     *
     * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-typeName">Type Name</a>
     *
     * @param organizationName Name to use for the "Organization" component of resource type names.
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the "Service" component for each of the generated resource's
     * type name.
     *
     * <p>This value defaults to the value of the {@code aws.api#service/cloudFormationName}
     * if the trait is present. Otherwise, the value defaults to the shape name of the
     * specified service shape.
     *
     * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-typeName">Type Name</a>
     *
     * @param serviceName Name to use for the "Service" component of resource type names.
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<String> getSourceDocs() {
        return sourceDocs;
    }

    /**
     * Limits the source of converted "sourceDocs" fields to the specified
     * priority ordered list of names in an externalDocumentation trait.
     *
     * <p>This list is case insensitive. By default, this is a list of the
     * following values: "Source Url", "SourceUrl", "Source", and "Source Code".
     *
     * @param sourceDocs Source docs to look for and convert, in order.
     */
    public void setSourceDocs(List<String> sourceDocs) {
        this.sourceDocs = sourceDocs;
    }

    @Override
    public void setUnionStrategy(UnionStrategy unionStrategy) {
        // CloudFormation Resource Schemas MUST use the oneOf schema property
        // for unions, which was already set in the constructor. Schemas are
        // not allowed to define additionalProperties as true, and modeling
        // as a structure is incorrect when oneOf is supported. Throw if
        // customers tried to set it to another UnionStrategy.
        //
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L210
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L166
        if (unionStrategy != UnionStrategy.ONE_OF) {
            throw new CfnException(String.format("CloudFormation Resource Schemas require the use of `oneOf` "
                    + "for defining unions in JSON Schema. `unionStrategy` value of `%s` was provided.",
                    unionStrategy));
        }
    }

    /**
     * Creates a CfnConfig from a Node value.
     *
     * <p>This method uses the {@link NodeMapper} on the converted input object.
     * Note that this class can be deserialized using a NodeMapper too since the
     * NodeMapper will look for a static, public, fromNode method.
     *
     * <p>This method also serializes unknown properties into the
     * "extensions" map so that they are accessible to CfnMapper implementations.
     *
     * @param settings Input to deserialize.
     * @return Returns the deserialized
     */
    public static CfnConfig fromNode(Node settings) {
        NodeMapper mapper = new NodeMapper();

        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.IGNORE);

        ObjectNode node = settings.expectObjectNode();
        CfnConfig config = new CfnConfig();
        mapper.deserializeInto(node, config);

        // Load a ShapeId map for the jsonAdd setting.
        node.getObjectMember("jsonAdd").ifPresent(jsonAddNode -> {
            Map<ShapeId, Map<String, Node>> jsonAddMap = new HashMap<>();

            for (Map.Entry<StringNode, Node> jsonAddMember : jsonAddNode.getMembers().entrySet()) {
                jsonAddMap.put(ShapeId.from(jsonAddMember.getKey().getValue()),
                        jsonAddMember.getValue().expectObjectNode().getStringMap());
            }

            config.setJsonAdd(jsonAddMap);
        });

        // Add all properties to "extensions" to make them accessible
        // in plugins.
        for (Map.Entry<String, Node> entry : node.getStringMap().entrySet()) {
            config.putExtension(entry.getKey(), entry.getValue());
        }

        return config;
    }

    @Override
    public void setJsonSchemaVersion(JsonSchemaVersion schemaVersion) {
        // CloudFormation Resource Schemas MUST use schema version draft07
        // https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.json#L210
        if (!schemaVersion.equals(JsonSchemaVersion.DRAFT07)) {
            throw new CfnException(String.format("CloudFormation Resource Schemas require the use of JSON Schema"
                    + " version draft07. `jsonSchemaVersion` value of `%s` was provided.", schemaVersion));
        }
    }
}
