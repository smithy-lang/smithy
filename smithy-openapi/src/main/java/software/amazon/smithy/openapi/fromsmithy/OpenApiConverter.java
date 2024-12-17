/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.OpenApiVersion;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.InfoObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.openapi.model.TagObject;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Tagged;

/**
 * Converts a Smithy model to OpenAPI.
 */
public final class OpenApiConverter {
    private static final Logger LOGGER = Logger.getLogger(OpenApiConverter.class.getName());

    private ClassLoader classLoader = OpenApiConverter.class.getClassLoader();
    private OpenApiConfig config = new OpenApiConfig();
    private final List<OpenApiMapper> mappers = new ArrayList<>();

    private OpenApiConverter() {}

    public static OpenApiConverter create() {
        return new OpenApiConverter();
    }

    /**
     * Get the OpenAPI configuration settings.
     *
     * @return Returns the config object.
     */
    public OpenApiConfig getConfig() {
        return config;
    }

    /**
     * Set the OpenAPI configuration settings.
     *
     * <p>This also updates the configuration object of any previously set
     * {@link JsonSchemaConfig}.
     *
     * @param config Config object to set.
     * @return Returns the converter.
     */
    public OpenApiConverter config(OpenApiConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Adds an {@link OpenApiMapper} to the converter.
     *
     * <p>This method is used to add custom OpenApiMappers to a converter that
     * are not automatically added by {@link Smithy2OpenApiExtension} objects
     * detected through Java SPI.
     *
     * @param mapper Mapper to add.
     * @return Returns the converter.
     */
    public OpenApiConverter addOpenApiMapper(OpenApiMapper mapper) {
        mappers.add(mapper);
        return this;
    }

    /**
     * Sets a {@link ClassLoader} to use to discover {@link JsonSchemaMapper},
     * {@link OpenApiMapper}, and {@link OpenApiProtocol} service providers
     * through SPI.
     *
     * <p>The {@code OpenApiConverter} will use its own ClassLoader by default.
     *
     * @param classLoader ClassLoader to use.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Converts the Smithy model to OpenAPI.
     *
     * @param model Smithy model to convert.
     * @return Returns the converted model.
     */
    public OpenApi convert(Model model) {
        return convertWithEnvironment(createConversionEnvironment(model));
    }

    /**
     * Converts the given service shape to a JSON/Node representation of an
     * OpenAPI model using the given Smithy model.
     *
     * <p>The result of this method may differ from the result of calling
     * {@link OpenApi#toNode()} because this method will pass the Node
     * representation of the OpenAPI through the {@link OpenApiMapper#updateNode}
     * method of each registered {@link OpenApiMapper}. This may cause
     * the returned value to no longer be a valid OpenAPI model but still
     * representative of the desired artifact (for example, an OpenAPI model
     * used with Amazon CloudFormation might used intrinsic JSON functions or
     * variable expressions that are replaced when synthesized).
     *
     * @param model Smithy model to convert.
     * @return Returns the converted model.
     */
    public ObjectNode convertToNode(Model model) {
        ConversionEnvironment<? extends Trait> environment = createConversionEnvironment(model);
        OpenApi openApi = convertWithEnvironment(environment);
        ObjectNode node = openApi.toNode().expectObjectNode();
        return environment.mapper.updateNode(environment.context, openApi, node);
    }

    private ConversionEnvironment<? extends Trait> createConversionEnvironment(Model model) {
        ShapeId serviceShapeId = config.getService();

        if (serviceShapeId == null) {
            throw new OpenApiException("openapi is missing required property, `service`");
        }

        if (config.getAddReferenceDescriptions() && config.getVersion() == OpenApiVersion.VERSION_3_0_2) {
            throw new OpenApiException(
                    "openapi property `addReferenceDescriptions` requires openapi version 3.1.0 or later.\n"
                            + "Suggestion: Add `\"version\"`: \"3.1.0\" to your openapi config.");
        }

        // Find the service shape.
        ServiceShape service = model.getShape(serviceShapeId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Shape `%s` not found in model",
                        serviceShapeId)))
                .asServiceShape()
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Shape `%s` is not a service shape",
                        serviceShapeId)));

        // Copy service errors onto each operation to ensure that common errors are
        // generated for each operation.
        model = ModelTransformer.create().copyServiceErrorsToOperations(model, service);

        // Remove mixins from the model.
        model = ModelTransformer.create().flattenAndRemoveMixins(model);

        // Dejjjqconflict errors that share the same status code.
        if (OpenApiConfig.ErrorStatusConflictHandlingStrategy.ONE_OF == config.getOnErrorStatusConflict()) {
            model = ModelTransformer.create().deconflictErrorsWithSharedStatusCode(model, service);
        }

        JsonSchemaConverter.Builder jsonSchemaConverterBuilder = JsonSchemaConverter.builder();
        jsonSchemaConverterBuilder.model(model);

        // Discover OpenAPI extensions.
        List<Smithy2OpenApiExtension> extensions = new ArrayList<>();

        for (Smithy2OpenApiExtension extension : ServiceLoader.load(Smithy2OpenApiExtension.class, classLoader)) {
            extensions.add(extension);
            // Add JSON schema mappers from found extensions.
            for (JsonSchemaMapper mapper : extension.getJsonSchemaMappers()) {
                jsonSchemaConverterBuilder.addMapper(mapper);
            }
        }

        Trait protocolTrait = loadOrDeriveProtocolTrait(model, service);
        OpenApiProtocol<Trait> openApiProtocol = loadOpenApiProtocol(service, protocolTrait, extensions);

        // Add default values from mappers. This is needed instead of just using `before`
        // because the JSON schema machinery uses configuration settings like
        // `alphanumericOnlyRefs` when it is created.
        OpenApiMapper composedMapper = createComposedMapper(extensions, mappers);
        composedMapper.updateDefaultSettings(model, config);

        // Update with protocol default values.
        openApiProtocol.updateDefaultSettings(model, config);
        jsonSchemaConverterBuilder.config(config);

        // Only convert shapes in the closure of the targeted service.
        jsonSchemaConverterBuilder.rootShape(service);
        JsonSchemaConverter jsonSchemaConverter = jsonSchemaConverterBuilder.build();
        SchemaDocument document = jsonSchemaConverter.convert();
        ComponentsObject.Builder components = ComponentsObject.builder();

        // Populate component schemas from the built document.
        for (Map.Entry<String, Schema> entry : document.getDefinitions().entrySet()) {
            String key = entry.getKey().replace(config.getDefinitionPointer() + "/", "");
            components.putSchema(key, entry.getValue());
        }

        // Load security scheme converters.
        List<SecuritySchemeConverter<? extends Trait>> securitySchemeConverters = loadSecuritySchemes(
                model,
                service,
                extensions);

        Context<Trait> context = new Context<>(
                model,
                service,
                config,
                jsonSchemaConverter,
                openApiProtocol,
                document,
                securitySchemeConverters);

        return new ConversionEnvironment<>(context, extensions, components, composedMapper);
    }

    private static OpenApiMapper createComposedMapper(
            List<Smithy2OpenApiExtension> extensions,
            List<OpenApiMapper> mappers
    ) {
        return OpenApiMapper.compose(Stream.concat(
                extensions.stream().flatMap(extension -> extension.getOpenApiMappers().stream()),
                mappers.stream()).collect(Collectors.toList()));
    }

    // Gets the protocol configured in `protocol` if set.
    //
    // If not set, defaults to the protocol applied to the service IFF the service
    // defines a single protocol.
    //
    // If the derived protocol trait cannot be found on the service, an exception
    // is thrown.
    private Trait loadOrDeriveProtocolTrait(Model model, ServiceShape service) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Set<ShapeId> serviceProtocols = serviceIndex.getProtocols(service).keySet();

        if (config.getProtocol() != null) {
            ShapeId protocolTraitId = config.getProtocol();
            return service.findTrait(protocolTraitId).orElseThrow(() -> {
                return new OpenApiException(String.format(
                        "Unable to find protocol `%s` on service `%s`. This service supports the following "
                                + "protocols: %s",
                        protocolTraitId,
                        service.getId(),
                        serviceProtocols));
            });
        } else if (serviceProtocols.isEmpty()) {
            throw new OpenApiException(String.format(
                    "No Smithy protocol was configured and `%s` does not define any protocols.",
                    service.getId()));
        } else if (serviceProtocols.size() > 1) {
            throw new OpenApiException(String.format(
                    "No Smithy protocol was configured and `%s` defines multiple protocols: %s",
                    service.getId(),
                    serviceProtocols));
        } else {
            // Get the first and only service protocol trait.
            return serviceIndex.getProtocols(service).values().iterator().next();
        }
    }

    private static final class ConversionEnvironment<T extends Trait> {
        private final Context<T> context;
        private final List<Smithy2OpenApiExtension> extensions;
        private final ComponentsObject.Builder components;
        private final OpenApiMapper mapper;

        private ConversionEnvironment(
                Context<T> context,
                List<Smithy2OpenApiExtension> extensions,
                ComponentsObject.Builder components,
                OpenApiMapper composedMapper
        ) {
            this.context = context;
            this.extensions = extensions;
            this.components = components;
            this.mapper = composedMapper;
        }
    }

    private <T extends Trait> OpenApi convertWithEnvironment(ConversionEnvironment<T> environment) {
        ServiceShape service = environment.context.getService();
        Context<T> context = environment.context;
        OpenApiMapper mapper = environment.mapper;
        OpenApiProtocol<T> openApiProtocol = environment.context.getOpenApiProtocol();
        String version = context.getConfig().getVersion().toString();
        OpenApi.Builder openapi = OpenApi.builder().openapi(version).info(createInfo(service));

        mapper.before(context, openapi);

        // The externalDocumentation trait of the service maps to externalDocs.
        OpenApiJsonSchemaMapper.getResolvedExternalDocs(service, context.getConfig())
                .ifPresent(openapi::externalDocs);

        // Include @tags trait tags that are compatible with OpenAPI settings.
        for (String tag : getSupportedTags(service)) {
            openapi.addTag(TagObject.builder().name(tag).build());
        }

        addPaths(context, openapi, openApiProtocol, mapper);
        addSecurityComponents(context, openapi, environment.components, mapper);

        // Merge in any schemas that needed to be created during translation.
        for (Map.Entry<String, Schema> entry : context.getSynthesizedSchemas().entrySet()) {
            environment.components.putSchema(entry.getKey(), entry.getValue());
        }

        openapi.components(environment.components.build());

        // Add arbitrary extensions if they're configured.
        openapi.getExtensions().putAll(context.getConfig().getSchemaDocumentExtensions().getStringMap());

        return mapper.after(context, openapi.build());
    }

    // Find the corresponding protocol OpenApiProtocol service provider.
    @SuppressWarnings("unchecked")
    private <T extends Trait> OpenApiProtocol<T> loadOpenApiProtocol(
            ServiceShape service,
            T protocolTrait,
            List<Smithy2OpenApiExtension> extensions
    ) {
        // Collect into a list so that a better error message can be presented if the
        // protocol converter can't be found.
        List<OpenApiProtocol> protocolProviders = extensions.stream()
                .flatMap(e -> e.getProtocols().stream())
                .collect(Collectors.toList());

        return protocolProviders.stream()
                .filter(openApiProtocol -> openApiProtocol.getProtocolType().equals(protocolTrait.getClass()))
                .findFirst()
                .map(result -> (OpenApiProtocol<T>) result)
                .orElseThrow(() -> {
                    Stream<String> supportedProtocols = protocolProviders.stream()
                            .map(OpenApiProtocol::getProtocolType)
                            .map(Class::getCanonicalName);
                    return new OpenApiException(String.format(
                            "Unable to find an OpenAPI service provider for the `%s` protocol when converting `%s`. "
                                    + "Protocol service providers were found for the following protocol classes: [%s].",
                            protocolTrait.toShapeId(),
                            service.getId(),
                            ValidationUtils.tickedList(supportedProtocols)));
                });
    }

    // Loads all of the OpenAPI security scheme implementations that are referenced by a service.
    private List<SecuritySchemeConverter<? extends Trait>> loadSecuritySchemes(
            Model model,
            ServiceShape service,
            List<Smithy2OpenApiExtension> extensions
    ) {
        // Note: Using a LinkedHashSet here in case order is ever important.
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Set<Class<? extends Trait>> schemes = getTraitMapTypes(serviceIndex.getAuthSchemes(service));

        List<SecuritySchemeConverter<? extends Trait>> converters = extensions.stream()
                .flatMap(extension -> extension.getSecuritySchemeConverters().stream())
                .collect(Collectors.toList());

        List<SecuritySchemeConverter<? extends Trait>> resolved = new ArrayList<>();
        for (SecuritySchemeConverter<? extends Trait> converter : converters) {
            if (schemes.remove(converter.getAuthSchemeType())) {
                resolved.add(converter);
            }
        }

        if (!schemes.isEmpty()) {
            LOGGER.warning(() -> String.format(
                    "Unable to find an OpenAPI authentication converter for the following schemes: [%s]",
                    schemes));
        }

        return resolved;
    }

    // Gets the tags of a shape that are allowed in the OpenAPI model.
    private List<String> getSupportedTags(Tagged tagged) {
        if (!config.getTags()) {
            return Collections.emptyList();
        }

        List<String> supported = config.getSupportedTags();
        return tagged.getTags()
                .stream()
                .filter(tag -> supported == null || supported.contains(tag))
                .collect(Collectors.toList());
    }

    private InfoObject createInfo(ServiceShape service) {
        InfoObject.Builder infoBuilder = InfoObject.builder();
        // Service documentation maps to info.description.
        service.getTrait(DocumentationTrait.class).ifPresent(trait -> infoBuilder.description(trait.getValue()));
        // Service version maps to info.version.
        infoBuilder.version(service.getVersion());
        // The title trait maps to info.title.
        infoBuilder.title(service.getTrait(TitleTrait.class)
                .map(TitleTrait::getValue)
                .orElse(service.getId().getName()));
        return infoBuilder.build();
    }

    private <T extends Trait> void addPaths(
            Context<T> context,
            OpenApi.Builder openApiBuilder,
            OpenApiProtocol<T> protocolService,
            OpenApiMapper plugin
    ) {
        TopDownIndex topDownIndex = TopDownIndex.of(context.getModel());
        Map<String, PathItem.Builder> paths = new HashMap<>();

        // Add each operation connected to the service shape to the OpenAPI model.
        topDownIndex.getContainedOperations(context.getService()).forEach(shape -> {
            OptionalUtils.ifPresentOrElse(protocolService.createOperation(context, shape), result -> {
                String method = result.getMethod();
                String path = result.getUri();
                PathItem.Builder pathItem = paths.computeIfAbsent(result.getUri(), (uri) -> PathItem.builder());

                // Mark the operation deprecated if the trait's present.
                if (shape.hasTrait(DeprecatedTrait.class)) {
                    result.getOperation().deprecated(true);
                }

                // Add security requirements to the operation.
                addOperationSecurity(context, result.getOperation(), shape, plugin);

                // Add the documentation trait to the operation if present.
                shape.getTrait(DocumentationTrait.class)
                        .map(DocumentationTrait::getValue)
                        .ifPresent(description -> result.getOperation().description(description));

                // The externalDocumentation trait of the operation maps to externalDocs.
                OpenApiJsonSchemaMapper.getResolvedExternalDocs(shape, context.getConfig())
                        .ifPresent(result.getOperation()::externalDocs);

                OperationObject builtOperation = result.getOperation().build();

                // Pass the operation through the plugin system.
                builtOperation = plugin.updateOperation(context, shape, builtOperation, method, path);
                // Add tags that are on the operation.
                builtOperation = addOperationTags(context, shape, builtOperation);
                // Update each parameter of the operation and rebuild if necessary.
                builtOperation = updateParameters(context, shape, builtOperation, method, path, plugin);
                // Update each response of the operation and rebuild if necessary.
                builtOperation = updateResponses(context, shape, builtOperation, method, path, plugin);
                // Update the request body of the operation and rebuild if necessary.
                builtOperation = updateRequestBody(context, shape, builtOperation, method, path, plugin);
                // Pass the operation through the plugin system for post-processing.
                builtOperation = plugin.postProcessOperation(context, shape, builtOperation, method, path);

                switch (method.toLowerCase(Locale.ENGLISH)) {
                    case "get":
                        pathItem.get(builtOperation);
                        break;
                    case "put":
                        pathItem.put(builtOperation);
                        break;
                    case "delete":
                        pathItem.delete(builtOperation);
                        break;
                    case "post":
                        pathItem.post(builtOperation);
                        break;
                    case "patch":
                        pathItem.patch(builtOperation);
                        break;
                    case "head":
                        pathItem.head(builtOperation);
                        break;
                    case "trace":
                        pathItem.trace(builtOperation);
                        break;
                    case "options":
                        pathItem.options(builtOperation);
                        break;
                    default:
                        LOGGER.warning(String.format(
                                "The %s HTTP method of `%s` is not supported by OpenAPI",
                                result.getMethod(),
                                shape.getId()));
                }
            },
                    () -> LOGGER.warning(String.format(
                            "The `%s` operation is not supported by the `%s` protocol (implemented by `%s`), and "
                                    + "was omitted",
                            shape.getId(),
                            protocolService.getClass().getName(),
                            context.getProtocolTrait().toShapeId())));
        });

        for (Map.Entry<String, PathItem.Builder> entry : paths.entrySet()) {
            String pathName = entry.getKey();
            // Enact the plugin infrastructure to update the PathItem if necessary.
            PathItem pathItem = plugin.updatePathItem(context, pathName, entry.getValue().build());
            openApiBuilder.putPath(pathName, pathItem);
        }
    }

    private <T extends Trait> void addOperationSecurity(
            Context<T> context,
            OperationObject.Builder builder,
            OperationShape shape,
            OpenApiMapper plugin
    ) {
        ServiceShape service = context.getService();
        ServiceIndex serviceIndex = ServiceIndex.of(context.getModel());
        Map<ShapeId, Trait> serviceSchemes = serviceIndex.getEffectiveAuthSchemes(service);
        Map<ShapeId, Trait> operationSchemes = serviceIndex.getEffectiveAuthSchemes(service, shape);

        // If the operation explicitly removes authentication, ensure that "security" is set to an empty
        // list as opposed to simply being unset as unset will result in the operation inheriting global
        // configuration.
        if (shape.getTrait(AuthTrait.class).map(trait -> trait.getValueSet().isEmpty()).orElse(false)) {
            builder.security(Collections.emptyList());
            return;
        }

        // Add a security requirement for the operation if it differs from the service.
        if (!operationSchemes.equals(serviceSchemes)) {
            Collection<Class<? extends Trait>> authSchemeClasses = getTraitMapTypes(operationSchemes);
            // Find all the converters with matching types of auth traits on the service.
            Collection<SecuritySchemeConverter<? extends Trait>> converters = findMatchingConverters(
                    context,
                    authSchemeClasses);
            for (SecuritySchemeConverter<? extends Trait> converter : converters) {
                List<String> result = createSecurityRequirements(context, converter, service);
                String openApiAuthName = converter.getOpenApiAuthSchemeName();
                Map<String, List<String>> authMap = MapUtils.of(openApiAuthName, result);
                Map<String, List<String>> requirement = plugin.updateSecurity(context, shape, converter, authMap);
                if (requirement != null) {
                    builder.addSecurity(requirement);
                }
            }
        }
    }

    // This method exists primarily to appease the type-checker.
    private <P extends Trait, A extends Trait> List<String> createSecurityRequirements(
            Context<P> context,
            SecuritySchemeConverter<A> converter,
            ServiceShape service
    ) {
        return converter.createSecurityRequirements(
                context,
                service.expectTrait(converter.getAuthSchemeType()),
                context.getService());
    }

    private OperationObject addOperationTags(
            Context<? extends Trait> context,
            Shape shape,
            OperationObject operation
    ) {
        // Include @tags trait tags of the operation that are compatible with OpenAPI settings.
        if (context.getConfig().getTags()) {
            return operation.toBuilder().tags(getSupportedTags(shape)).build();
        }

        return operation;
    }

    // Applies mappers to parameters and updates the operation if parameters change.
    private <T extends Trait> OperationObject updateParameters(
            Context<T> context,
            OperationShape shape,
            OperationObject operation,
            String method,
            String path,
            OpenApiMapper plugin
    ) {
        List<ParameterObject> parameters = new ArrayList<>();
        for (ParameterObject parameter : operation.getParameters()) {
            parameters.add(plugin.updateParameter(context, shape, method, path, parameter));
        }

        return !parameters.equals(operation.getParameters())
                ? operation.toBuilder().parameters(parameters).build()
                : operation;
    }

    // Applies mappers to each request body and update the operation if the body changes.
    private <T extends Trait> OperationObject updateRequestBody(
            Context<T> context,
            OperationShape shape,
            OperationObject operation,
            String method,
            String path,
            OpenApiMapper plugin
    ) {
        return operation.getRequestBody()
                .map(body -> {
                    RequestBodyObject updatedBody = plugin.updateRequestBody(context, shape, method, path, body);
                    return body.equals(updatedBody)
                            ? operation
                            : operation.toBuilder().requestBody(updatedBody).build();
                })
                .orElse(operation);
    }

    // Ensures that responses have at least one entry, and applies mappers to
    // responses and updates the operation is a response changes.
    private <T extends Trait> OperationObject updateResponses(
            Context<T> context,
            OperationShape shape,
            OperationObject operation,
            String methodName,
            String path,
            OpenApiMapper plugin
    ) {
        Map<String, ResponseObject> newResponses = new LinkedHashMap<>();
        for (Map.Entry<String, ResponseObject> entry : operation.getResponses().entrySet()) {
            String status = entry.getKey();
            ResponseObject responseObject = plugin.updateResponse(
                    context,
                    shape,
                    status,
                    methodName,
                    path,
                    entry.getValue());
            newResponses.put(status, responseObject);
        }

        if (newResponses.equals(operation.getResponses())) {
            return operation;
        } else {
            return operation.toBuilder().responses(newResponses).build();
        }
    }

    private <T extends Trait> void addSecurityComponents(
            Context<T> context,
            OpenApi.Builder openApiBuilder,
            ComponentsObject.Builder components,
            OpenApiMapper plugin
    ) {
        ServiceShape service = context.getService();
        ServiceIndex serviceIndex = ServiceIndex.of(context.getModel());

        // Create security components for each referenced security scheme.
        for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
            SecurityScheme createdScheme = createAndUpdateSecurityScheme(context, plugin, converter, service);
            if (createdScheme != null) {
                components.putSecurityScheme(converter.getOpenApiAuthSchemeName(), createdScheme);
            }
        }

        // Assign the components to the "security" of the service. This is only the
        // auth schemes that apply by default across the entire service.
        Map<ShapeId, Trait> authTraitMap = serviceIndex.getEffectiveAuthSchemes(context.getService());
        Collection<Class<? extends Trait>> defaultAuthTraits = getTraitMapTypes(authTraitMap);

        for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
            if (defaultAuthTraits.contains(converter.getAuthSchemeType())) {
                List<String> result = createSecurityRequirements(context, converter, context.getService());
                String authSchemeName = converter.getOpenApiAuthSchemeName();
                Map<String, List<String>> requirement = plugin.updateSecurity(
                        context,
                        context.getService(),
                        converter,
                        MapUtils.of(authSchemeName, result));
                if (requirement != null) {
                    openApiBuilder.addSecurity(requirement);
                }
            }
        }
    }

    private Set<Class<? extends Trait>> getTraitMapTypes(Map<ShapeId, Trait> traitMap) {
        return traitMap.values().stream().map(Trait::getClass).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // This method exists primarily to appease the type-checker.
    private <P extends Trait, A extends Trait> SecurityScheme createAndUpdateSecurityScheme(
            Context<P> context,
            OpenApiMapper plugin,
            SecuritySchemeConverter<A> converter,
            ServiceShape service
    ) {
        A authTrait = service.expectTrait(converter.getAuthSchemeType());
        SecurityScheme createdScheme = converter.createSecurityScheme(context, authTrait);
        return plugin.updateSecurityScheme(context, authTrait, createdScheme);
    }

    @SuppressWarnings("unchecked")
    private Collection<SecuritySchemeConverter<? extends Trait>> findMatchingConverters(
            Context<? extends Trait> context,
            Collection<Class<? extends Trait>> schemes
    ) {
        return context.getSecuritySchemeConverters()
                .stream()
                .filter(converter -> schemes.contains(converter.getAuthSchemeType()))
                .map(converter -> (SecuritySchemeConverter<Trait>) converter)
                .collect(Collectors.toList());
    }
}
