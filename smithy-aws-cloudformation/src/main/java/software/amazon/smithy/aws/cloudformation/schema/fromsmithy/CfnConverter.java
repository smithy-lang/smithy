/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.CfnException;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.TaggingMapper;
import software.amazon.smithy.aws.cloudformation.schema.model.Property;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.cloudformation.traits.CfnNameTrait;
import software.amazon.smithy.aws.cloudformation.traits.CfnResource;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceIndex;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceTrait;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.jsonschema.PropertyNamingStrategy;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.StringUtils;

public final class CfnConverter {
    private ClassLoader classLoader = CfnConverter.class.getClassLoader();
    private CfnConfig config = new CfnConfig();
    private final List<Smithy2CfnExtension> extensions = new ArrayList<>();

    private CfnConverter() {}

    public static CfnConverter create() {
        return new CfnConverter();
    }

    /**
     * Get the CloudFormation configuration settings.
     *
     * @return Returns the config object.
     */
    public CfnConfig getConfig() {
        return config;
    }

    /**
     * Set the CloudFormation configuration settings.
     *
     * @param config Config object to set.
     * @return Returns the converter.
     */
    public CfnConverter config(CfnConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Sets a {@link ClassLoader} to use to discover {@link Smithy2CfnExtension}
     * service providers through SPI.
     *
     * <p>
     * The {@code CfnConverter} will use its own ClassLoader by default.
     *
     * @param classLoader ClassLoader to use.
     * @return Returns the converter.
     */
    public CfnConverter classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Converts resource shapes annotated with the {@code @cfnResource} trait to
     * JSON/Node representations of CloudFormation Resource Schemas using the
     * given Smithy model.
     *
     * <p>
     * The result of this method may differ from the result of calling
     * {@link ResourceSchema#toNode()} because this method will pass the Node
     * representation of the ResourceSchema through the {@link CfnMapper#updateNode}
     * method of each registered {@link CfnMapper}.
     *
     * @param model Smithy model to convert.
     * @return A map of CloudFormation resource type names to their converted schema
     *         nodes.
     */
    public Map<String, ObjectNode> convertToNodes(Model model) {
        List<ConversionEnvironment> environments = createConversionEnvironments(model);
        Map<ShapeId, ResourceSchema> resources = convertWithEnvironments(environments);

        Map<String, ObjectNode> convertedNodes = new HashMap<>();
        for (ConversionEnvironment environment : environments) {
            ResourceSchema resourceSchema = resources.get(environment.context.getResource().getId());
            ObjectNode node = resourceSchema.toNode().expectObjectNode();

            // Apply all the mappers' updateNode methods.
            for (CfnMapper mapper : environment.mappers) {
                node = mapper.updateNode(environment.context, resourceSchema, node);
            }

            // CloudFormation resource schemas require the presence of a top-level
            // additionalProperties setting with the value of false to be validated.
            node = node.withMember("additionalProperties", false);

            convertedNodes.put(resourceSchema.getTypeName(), node);
        }
        return convertedNodes;
    }

    /**
     * Converts the annotated resources in the Smithy model to CloudFormation
     * Resource Schemas.
     *
     * @param model Smithy model containing resources to convert.
     * @return Returns the converted resources.
     */
    public List<ResourceSchema> convert(Model model) {
        return ListUtils.copyOf(convertWithEnvironments(createConversionEnvironments(model)).values());
    }

    private Map<ShapeId, ResourceSchema> convertWithEnvironments(List<ConversionEnvironment> environments) {
        Map<ShapeId, ResourceSchema> resourceSchemas = new HashMap<>();
        for (ConversionEnvironment environment : environments) {
            ResourceShape resourceShape = environment.context.getResource();
            ResourceSchema resourceSchema = convertResource(environment, resourceShape);
            resourceSchemas.put(resourceShape.getId(), resourceSchema);
        }
        return resourceSchemas;
    }

    private List<ConversionEnvironment> createConversionEnvironments(Model model) {
        ShapeId serviceShapeId = config.getService();

        if (serviceShapeId == null) {
            throw new CfnException("cloudformation is missing required property, `service`");
        }

        // Load the Smithy2Cfn extensions.
        ServiceLoader.load(Smithy2CfnExtension.class, classLoader).forEach(extensions::add);

        // Find the service shape.
        ServiceShape serviceShape = model.expectShape(serviceShapeId, ServiceShape.class);

        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Set<ResourceShape> resourceShapes = topDownIndex.getContainedResources(serviceShape);

        // Create an environment for each of the resources to be converted with.
        List<ConversionEnvironment> environments = new ArrayList<>();
        for (ResourceShape resourceShape : resourceShapes) {
            if (resourceShape.getTrait(CfnResourceTrait.class).isPresent()) {
                ConversionEnvironment environment = createConversionEnvironment(model, serviceShape, resourceShape);
                environments.add(environment);
            }
        }

        return environments;
    }

    private ConversionEnvironment createConversionEnvironment(
            Model model,
            ServiceShape serviceShape,
            ResourceShape resourceShape
    ) {
        // Prepare the JSON Schema Converter.
        JsonSchemaConverter.Builder jsonSchemaConverterBuilder = JsonSchemaConverter.builder()
                .config(config)
                .propertyNamingStrategy(getPropertyNamingStrategy());

        List<CfnMapper> mappers = new ArrayList<>();
        for (Smithy2CfnExtension extension : extensions) {
            mappers.addAll(extension.getCfnMappers());
            // Add JSON schema mappers from found extensions.
            for (JsonSchemaMapper mapper : extension.getJsonSchemaMappers()) {
                jsonSchemaConverterBuilder.addMapper(mapper);
            }
        }
        mappers.sort(Comparator.comparingInt(CfnMapper::getOrder));

        CfnResourceIndex resourceIndex = CfnResourceIndex.of(model);
        CfnResource cfnResource = resourceIndex.getResource(resourceShape)
                .orElseThrow(() -> new CfnException("Attempted to generate a CloudFormation resource schema "
                        + "not found to have resource data."));

        // Prepare a structure representing the CFN resource to be created and
        // add that structure to a temporary model that's used for conversion.
        // JSON Schema conversion requires that the shape being converted is
        // present in the model. See the docs for getCfnResourceStructure for
        // more information.
        StructureShape pseudoResource = getCfnResourceStructure(model, resourceShape, cfnResource);
        Model updatedModel = model.toBuilder().addShape(pseudoResource).build();
        jsonSchemaConverterBuilder.model(updatedModel);

        Context context = new Context(updatedModel,
                serviceShape,
                resourceShape,
                cfnResource,
                pseudoResource,
                config,
                jsonSchemaConverterBuilder.build());

        return new ConversionEnvironment(context, mappers);
    }

    private PropertyNamingStrategy getPropertyNamingStrategy() {
        return (containingShape, member, config) -> {
            // The cfnName trait's value takes precedence, even over any settings.
            Optional<CfnNameTrait> cfnNameTrait = member.getTrait(CfnNameTrait.class);
            if (cfnNameTrait.isPresent()) {
                return cfnNameTrait.get().getValue();
            }
            // The property trait's name value takes next precedence.
            Optional<PropertyTrait> propertyTrait = member.getTrait(PropertyTrait.class);
            if (propertyTrait.isPresent() && propertyTrait.flatMap(PropertyTrait::getName).isPresent()) {
                return this.config.getDisableCapitalizedProperties()
                        ? StringUtils.capitalize(propertyTrait.get().getName().get())
                        : propertyTrait.get().getName().get();
            }

            // Otherwise, respect the property capitalization setting.
            String name = PropertyNamingStrategy.createMemberNameStrategy()
                    .toPropertyName(containingShape, member, config);

            return this.config.getDisableCapitalizedProperties()
                    ? name
                    : StringUtils.capitalize(name);
        };
    }

    private static final class ConversionEnvironment {
        private final Context context;
        private final List<CfnMapper> mappers;

        private ConversionEnvironment(
                Context context,
                List<CfnMapper> mappers
        ) {
            this.context = context;
            this.mappers = mappers;
        }
    }

    private ResourceSchema convertResource(ConversionEnvironment environment, ResourceShape resourceShape) {
        Context context = environment.context;
        JsonSchemaConverter jsonSchemaConverter = context.getJsonSchemaConverter()
                .toBuilder()
                .rootShape(context.getResourceStructure())
                .build();
        SchemaDocument document = jsonSchemaConverter.convert();

        // Prepare the initial contents
        CfnResourceTrait resourceTrait = resourceShape.expectTrait(CfnResourceTrait.class);
        ResourceSchema.Builder builder = ResourceSchema.builder();
        String typeName = resolveResourceTypeName(environment, resourceTrait);
        builder.typeName(typeName);

        // Apply the resource's documentation if present, or default.
        builder.description(resourceShape.getTrait(DocumentationTrait.class)
                .map(StringTrait::getValue)
                .orElse("Definition of " + typeName + " Resource Type"));

        // Apply all the mappers' before methods.
        for (CfnMapper mapper : environment.mappers) {
            mapper.before(context, builder);
        }

        // Add the properties from the converted shape.
        document.getRootSchema().getProperties().forEach((name, schema) -> {
            Property property = Property.builder()
                    .schema(schema)
                    .build();
            builder.addProperty(name, property);
        });

        // Supply all the definitions that were created.
        for (Map.Entry<String, Schema> definition : document.getDefinitions().entrySet()) {
            String definitionName = definition.getKey()
                    .replace(CfnConfig.SCHEMA_COMPONENTS_POINTER, "")
                    .substring(1);
            builder.addDefinition(definitionName, definition.getValue());
        }

        // Apply all the mappers' after methods.
        ResourceSchema resourceSchema = builder.build();
        for (CfnMapper mapper : environment.mappers) {
            resourceSchema = mapper.after(context, resourceSchema);
        }

        return resourceSchema;
    }

    private String resolveResourceTypeName(
            ConversionEnvironment environment,
            CfnResourceTrait resourceTrait
    ) {
        CfnConfig config = environment.context.getConfig();
        ServiceShape serviceShape = environment.context.getModel().expectShape(config.getService(), ServiceShape.class);
        Optional<ServiceTrait> serviceTrait = serviceShape.getTrait(ServiceTrait.class);

        String organizationName = config.getOrganizationName();
        if (organizationName == null) {
            // Services utilizing the AWS service trait default to being in the
            // "AWS" organization instead of requiring the configuration value.
            organizationName = serviceTrait
                    .map(t -> "AWS")
                    .orElseThrow(
                            () -> new CfnException("cloudformation is missing required property, `organizationName`"));
        }

        String serviceName = config.getServiceName();
        if (serviceName == null) {
            // Services utilizing the AWS service trait have the `cloudFormationName`
            // member, so use that if present. Otherwise, default to the service
            // shape's name.
            serviceName = serviceTrait
                    .map(ServiceTrait::getCloudFormationName)
                    .orElse(serviceShape.getId().getName());
        }

        // Use the trait's name if present, or default to the resource shape's name.
        String resourceName = resourceTrait.getName()
                .orElse(environment.context.getResource().getId().getName());

        return String.format("%s::%s::%s", organizationName, serviceName, resourceName);
    }

    /*
     * JSON Schema conversion requires that the shape being converted is present
     * in the model. Since the properties of a CloudFormation resource are derived
     * from multiple locations, these properties need to be added to a single
     * StructureShape that can be added to a model for converting.
     *
     * To do so, the derived properties of a CloudFormation resource are added
     * to a synthetic structure. Members are reparented and identifiers are
     * added as new members.
     */
    private StructureShape getCfnResourceStructure(Model model, ResourceShape resource, CfnResource cfnResource) {
        StructureShape.Builder builder = StructureShape.builder();
        ShapeId resourceId = resource.getId();
        builder.id(ShapeId.fromParts(resourceId.getNamespace(), resourceId.getName() + "__SYNTHETIC__"));

        cfnResource.getProperties().forEach((name, definition) -> {
            Shape definitionShape = model.expectShape(definition.getShapeId());
            // We got a member that's pulled in, so reparent it.
            if (definitionShape.isMemberShape()) {
                MemberShape member = definitionShape.asMemberShape().get();
                // Adjust the ID of the member.
                member = member.toBuilder().id(builder.getId().withMember(name)).build();
                builder.addMember(member);
            } else {
                // This is an identifier, create a new member.
                builder.addMember(name, definition.getShapeId());
            }
        });

        TaggingMapper.injectTagsMember(config, model, resource, builder);

        return builder.build();
    }
}
