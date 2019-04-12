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

package software.amazon.smithy.openapi.fromsmithy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.jsonschema.JsonSchemaConstants;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaBuilderMapper;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.Tagged;
import software.amazon.smithy.model.knowledge.AuthIndex;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.model.ComponentsObject;
import software.amazon.smithy.openapi.model.ExternalDocumentation;
import software.amazon.smithy.openapi.model.InfoObject;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.openapi.model.TagObject;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Converts a Smithy model to OpenAPI.
 */
public final class OpenApiConverter {
    private static final Logger LOGGER = Logger.getLogger(OpenApiConverter.class.getName());

    private Map<String, Node> settings = new HashMap<>();
    private ClassLoader classLoader = OpenApiConverter.class.getClassLoader();
    private JsonSchemaConverter jsonSchemaConverter;
    private String protocolName;
    private List<OpenApiProtocol> customProtocols = new ArrayList<>();

    private OpenApiConverter() {}

    public static OpenApiConverter create() {
        return new OpenApiConverter();
    }

    /**
     * Set the converter used to build Smithy shapes.
     *
     * @param jsonSchemaConverter Shape converter to use.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter jsonSchemaConverter(JsonSchemaConverter jsonSchemaConverter) {
        this.jsonSchemaConverter = jsonSchemaConverter;
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @param <T> value type to set.
     * @return Returns the OpenApiConverter.
     */
    public <T extends ToNode> OpenApiConverter putSetting(String setting, T value) {
        settings.put(setting, value.toNode());
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter putSetting(String setting, String value) {
        settings.put(setting, Node.from(value));
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter putSetting(String setting, Number value) {
        settings.put(setting, Node.from(value));
        return this;
    }

    /**
     * Puts a setting on the converter.
     *
     * @param setting Setting name to set.
     * @param value Setting value to set.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter putSetting(String setting, boolean value) {
        settings.put(setting, Node.from(value));
        return this;
    }

    /**
     * Sets a {@link ClassLoader} to use to discover {@link SchemaBuilderMapper},
     * {@link SmithyOpenApiPlugin}, and {@link OpenApiProtocol} service providers
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
     * Sets the name of the protocol to use when converting the model.
     *
     * @param protocolName Name of the protocol.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter protocolName(String protocolName) {
        this.protocolName = protocolName;
        return this;
    }

    /**
     * Registers a custom protocol that is not registered through service discovery.
     *
     * @param protocol Protocol implementation to register.
     * @return Returns the OpenApiConverter.
     */
    public OpenApiConverter addCustomProtocol(OpenApiProtocol protocol) {
        customProtocols.add(protocol);
        return this;
    }

    /**
     * Converts the given service shape to OpenAPI model using the given
     * Smithy model.
     *
     * @param model Smithy model to convert.
     * @param serviceShapeId Service to convert.
     * @return Returns the converted model.
     */
    public OpenApi convert(Model model, ShapeId serviceShapeId) {
        return convertWithEnvironment(createConversionEnvironment(model, serviceShapeId));
    }

    /**
     * Converts the given service shape to a JSON/Node representation of an
     * OpenAPI model using the given Smithy model.
     *
     * <p>The result of this method may differ from the result of calling
     * {@link OpenApi#toNode()} because this method will pass the Node
     * representation of the OpenAPI through the {@link SmithyOpenApiPlugin#updateNode}
     * method of each registered {@link SmithyOpenApiPlugin}. This may cause
     * the returned value to no longer be a valid OpenAPI model but still
     * representative of the desired artifact (for example, an OpenAPI model
     * used with Amazon CloudFormation might used intrinsic JSON functions or
     * variable expressions that are replaced when synthesized).
     *
     * @param model Smithy model to convert.
     * @param serviceShapeId Service to convert.
     * @return Returns the converted model.
     */
    public ObjectNode convertToNode(Model model, ShapeId serviceShapeId) {
        ConversionEnvironment environment = createConversionEnvironment(model, serviceShapeId);
        OpenApi openApi = convertWithEnvironment(environment);
        ObjectNode node = openApi.toNode().expectObjectNode();
        return environment.plugin.updateNode(environment.context, openApi, node);
    }

    private ConversionEnvironment createConversionEnvironment(Model model, ShapeId serviceShapeId) {
        getJsonSchemaConverter().discoverSchemaMappersWith(classLoader);

        // Update the JSON schema config with the settings from this class and
        // configure it to use OpenAPI settings.
        ObjectNode.Builder configBuilder = getJsonSchemaConverter()
                .getConfig()
                .toBuilder()
                .withMember(OpenApiConstants.OPEN_API_MODE, true)
                .withMember(JsonSchemaConstants.DEFINITION_POINTER, OpenApiConstants.SCHEMA_COMPONENTS_POINTER);
        settings.forEach(configBuilder::withMember);
        ObjectNode config = configBuilder.build();
        getJsonSchemaConverter().config(config);

        ServiceShape service = model.getShapeIndex().getShape(serviceShapeId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Shape `%s` not found in shape index", serviceShapeId)))
                .asServiceShape()
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Shape `%s` is not a service shape", serviceShapeId)));

        Pair<Protocol, OpenApiProtocol> protocolPair = resolveProtocol(service);
        Protocol resolvedProtocol = protocolPair.getLeft();
        OpenApiProtocol openApiProtocol = protocolPair.getRight();
        // Set a protocol name if one wasn't set but instead derived.
        protocolName = protocolName != null ? protocolName : resolvedProtocol.getName();
        ComponentsObject.Builder components = ComponentsObject.builder();
        SchemaDocument schemas = addSchemas(components, model.getShapeIndex(), service);
        List<SecuritySchemeConverter> securitySchemeConverters = loadSecuritySchemes(service);
        Context context = new Context(
                model, service, getJsonSchemaConverter(),
                resolvedProtocol, openApiProtocol, schemas, securitySchemeConverters);

        SmithyOpenApiPlugin plugin = loadPlugins();
        return new ConversionEnvironment(context, plugin, components);
    }

    private static final class ConversionEnvironment {
        private final Context context;
        private final SmithyOpenApiPlugin plugin;
        private final ComponentsObject.Builder components;

        private ConversionEnvironment(
                Context context,
                SmithyOpenApiPlugin plugin,
                ComponentsObject.Builder components
        ) {
            this.context = context;
            this.plugin = plugin;
            this.components = components;
        }
    }

    private OpenApi convertWithEnvironment(ConversionEnvironment environment) {
        ServiceShape service = environment.context.getService();
        Context context = environment.context;
        SmithyOpenApiPlugin plugin = environment.plugin;
        OpenApiProtocol openApiProtocol = environment.context.getOpenApiProtocol();
        OpenApi.Builder openapi = OpenApi.builder().openapi(OpenApiConstants.VERSION).info(createInfo(service));

        plugin.before(context, openapi);

        // The externalDocumentation trait of the service maps to externalDocs.
        service.getTrait(ExternalDocumentationTrait.class)
                .ifPresent(trait -> openapi.externalDocs(
                        ExternalDocumentation.builder().url(trait.getValue()).build()));

        // Include @tags trait tags that are compatible with OpenAPI settings.
        if (environment.context.getConfig().getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_TAGS)) {
            getSupportedTags(service).forEach(tag -> openapi.addTag(TagObject.builder().name(tag).build()));
        }

        addPaths(context, openapi, openApiProtocol, plugin);
        addSecurityComponents(context, openapi, environment.components, plugin);
        openapi.components(environment.components.build());

        return plugin.after(context, openapi.build());
    }

    private JsonSchemaConverter getJsonSchemaConverter() {
        if (jsonSchemaConverter == null) {
            jsonSchemaConverter = JsonSchemaConverter.create();
        }

        return jsonSchemaConverter;
    }

    private SmithyOpenApiPlugin loadPlugins() {
        return SmithyOpenApiPlugin.compose(discover(SmithyOpenApiPlugin.class));
    }

    // Determine which OpenApiProtocol service provider and which service trait protocol to use.
    private Pair<Protocol, OpenApiProtocol> resolveProtocol(ServiceShape service) {
        List<OpenApiProtocol> protocols = discover(OpenApiProtocol.class);
        ProtocolsTrait protoTrait = service.getTrait(ProtocolsTrait.class)
                .orElseThrow(() -> new OpenApiException("No `protocols` trait found on `" + service.getId() + "`"));

        if (protocolName == null) {
            for (Protocol protocolEntry : protoTrait.getProtocols()) {
                Optional<OpenApiProtocol> maybeProtocol = findProtocol(protocolEntry.getName(), protocols);
                if (maybeProtocol.isPresent()) {
                    return Pair.of(protocolEntry, maybeProtocol.get());
                }
            }
        } else if (protoTrait.getProtocol(protocolName).isPresent()) {
            Optional<OpenApiProtocol> maybeProtocol = findProtocol(protocolName, protocols);
            if (maybeProtocol.isPresent()) {
                return Pair.of(protoTrait.getProtocol(protocolName).get(), maybeProtocol.get());
            }
        }

        throw new OpenApiException(String.format(
                "Unable to resolve a supported protocol for service: `%s`. Protocol service providers were "
                + "found for the following protocol patterns: [%s]. This service supports the following "
                + "protocols: [%s]",
                service.getId(),
                ValidationUtils.tickedList(protocols.stream().map(OpenApiProtocol::getProtocolNamePattern)),
                ValidationUtils.tickedList(protoTrait.getProtocolNames())));
    }

    // Finds an OpenAPI protocol matching the given protocol name.
    private Optional<OpenApiProtocol> findProtocol(String protocolName, List<OpenApiProtocol> protocols) {
        return Stream.concat(customProtocols.stream(), protocols.stream())
                .filter(protocol -> protocol.getProtocolNamePattern().matcher(protocolName).find())
                .findFirst();
    }

    // Loads all of the OpenAPI security scheme implementations that are referenced by a service.
    private List<SecuritySchemeConverter> loadSecuritySchemes(ServiceShape service) {
        List<SecuritySchemeConverter> converters = discover(SecuritySchemeConverter.class);
        // Get auth schemes of a specific protocol.
        Set<String> schemes = new HashSet<>(service.getTrait(ProtocolsTrait.class)
                .flatMap(trait -> trait.getProtocol(protocolName))
                .map(Protocol::getAuth)
                .orElse(ListUtils.of()));
        List<SecuritySchemeConverter> resolved = new ArrayList<>();

        for (SecuritySchemeConverter converter: converters) {
            if (schemes.remove(converter.getAuthSchemeName())) {
                resolved.add(converter);
            }
        }

        if (!schemes.isEmpty()) {
            LOGGER.warning(() -> String.format(
                    "Unable to find an OpenAPI authentication converter for the following schemes: [%s]", schemes));
        }

        return resolved;
    }

    // Discovers implementations of a class using a classLoader.
    private <T> List<T> discover(Class<T> clazz) {
        List<T> result = new ArrayList<>();
        ServiceLoader.load(clazz, classLoader).forEach(result::add);
        return result;
    }

    // Gets the tags of a shape that are allowed in the OpenAPI model.
    private Stream<String> getSupportedTags(Tagged tagged) {
        ObjectNode config = getJsonSchemaConverter().getConfig();
        List<String> supported = config.getArrayMember(OpenApiConstants.OPEN_API_SUPPORTED_TAGS)
                .map(array -> array.getElementsAs(StringNode::getValue))
                .orElse(null);
        return tagged.getTags().stream().filter(tag -> supported == null || supported.contains(tag));
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

    // Copies the JSON schema schemas over into the OpenAPI object.
    private SchemaDocument addSchemas(
            ComponentsObject.Builder components,
            ShapeIndex index,
            ServiceShape service
    ) {
        SchemaDocument document = getJsonSchemaConverter().convert(index, service);
        for (Map.Entry<String, Schema> entry : document.getDefinitions().entrySet()) {
            String key = entry.getKey().replace(OpenApiConstants.SCHEMA_COMPONENTS_POINTER + "/", "");
            components.putSchema(key, entry.getValue());
        }
        return document;
    }

    private void addPaths(
            Context context,
            OpenApi.Builder openApiBuilder,
            OpenApiProtocol protocolService,
            SmithyOpenApiPlugin plugin
    ) {
        TopDownIndex topDownIndex = context.getModel().getKnowledge(TopDownIndex.class);
        Map<String, PathItem.Builder> paths = new HashMap<>();

        // Add each operation connected to the service shape to the OpenAPI model.
        topDownIndex.getContainedOperations(context.getService()).forEach(shape -> {
            OptionalUtils.ifPresentOrElse(protocolService.createOperation(context, shape), result -> {
                PathItem.Builder pathItem = paths.computeIfAbsent(result.getUri(), (uri) -> PathItem.builder());
                // Add security requirements to the operation.
                addOperationSecurity(context, result.getOperation(), shape);
                // Pass the operation through the plugin system and then build it.
                OperationObject builtOperation = plugin.updateOperation(context, shape, result.getOperation().build());
                // Add tags that are on the operation.
                builtOperation = addOperationTags(context, shape, builtOperation);
                // Update each parameter of the operation and rebuild if necessary.
                builtOperation = updateParameters(context, shape, builtOperation, plugin);
                // Update each response of the operation and rebuild if necessary.
                builtOperation = updateResponses(context, shape, builtOperation, plugin);
                // Update the request body of the operation and rebuild if necessary.
                builtOperation = updateRequestBody(context, shape, builtOperation, plugin);

                switch (result.getMethod().toLowerCase(Locale.US)) {
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
                                result.getMethod(), shape.getId()));
                }
            }, () -> LOGGER.warning(String.format(
                    "The `%s` operation is not supported by the `%s` protocol (implemented by `%s`), and "
                    + "was omitted", shape.getId(), protocolService.getClass().getName(), context.getProtocolName()))
            );
        });

        for (Map.Entry<String, PathItem.Builder> entry : paths.entrySet()) {
            String pathName = entry.getKey();
            // Enact the plugin infrastructure to update the PathItem if necessary.
            PathItem pathItem = plugin.updatePathItem(context, entry.getValue().build());
            openApiBuilder.putPath(pathName, pathItem);
        }
    }

    private void addOperationSecurity(
            Context context,
            OperationObject.Builder builder,
            OperationShape shape
    ) {
        ServiceShape service = context.getService();
        AuthIndex auth = context.getModel().getKnowledge(AuthIndex.class);
        List<String> serviceSchemes = auth.getDefaultServiceSchemes(service);
        // Note: the eligible schemes have already been filtered for the protocol, so no need to do that here.
        List<String> operationSchemes = auth.getOperationSchemes(service, shape, context.getProtocolName());

        // Add a security requirement for the operation if it differs from the service.
        if (!SetUtils.copyOf(serviceSchemes).equals(SetUtils.copyOf(operationSchemes))) {
            for (SecuritySchemeConverter converter : findMatchingConverters(context, operationSchemes)) {
                List<String> result = converter.createSecurityRequirements(context, shape);
                builder.addSecurity(MapUtils.of(converter.getSecurityName(context), result));
            }
        }
    }

    private OperationObject addOperationTags(Context context, Shape shape, OperationObject operation) {
        // Include @tags trait tags of the operation that are compatible with OpenAPI settings.
        if (context.getConfig().getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_TAGS)) {
            List<String> tags = getSupportedTags(shape).collect(Collectors.toList());
            if (!tags.isEmpty()) {
                return operation.toBuilder().tags(tags).build();
            }
        }

        return operation;
    }

    // Applies plugins to parameters and updates the operation if parameters change.
    private OperationObject updateParameters(
            Context context,
            OperationShape shape,
            OperationObject operation,
            SmithyOpenApiPlugin plugin
    ) {
        List<ParameterObject> parameters = new ArrayList<>();
        for (ParameterObject parameter : operation.getParameters()) {
            parameters.add(plugin.updateParameter(context, shape, parameter));
        }

        return !parameters.equals(operation.getParameters())
               ? operation.toBuilder().parameters(parameters).build()
               : operation;
    }

    // Applies plugins to each request body and update the operation if the body changes.
    private OperationObject updateRequestBody(
            Context context,
            OperationShape shape,
            OperationObject operation,
            SmithyOpenApiPlugin plugin
    ) {
        return operation.getRequestBody()
                .map(body -> {
                    RequestBodyObject updatedBody = plugin.updateRequestBody(context, shape, body);
                    return body.equals(updatedBody)
                           ? operation
                           : operation.toBuilder().requestBody(updatedBody).build();
                })
                .orElse(operation);
    }

    // Ensures that responses have at least one entry, and applies plugins to
    // responses and updates the operation is a response changes.
    private OperationObject updateResponses(
            Context context,
            OperationShape shape,
            OperationObject operation,
            SmithyOpenApiPlugin plugin
    ) {
        Map<String, ResponseObject> newResponses = new LinkedHashMap<>();

        // OpenAPI requires at least one response, so track the "original"
        // responses vs new/mutated responses.
        Map<String, ResponseObject> originalResponses = operation.getResponses();
        if (operation.getResponses().isEmpty()) {
            int code = context.getModel().getKnowledge(HttpBindingIndex.class).getResponseCode(shape);
            originalResponses = MapUtils.of(String.valueOf(code), ResponseObject.builder()
                    .description(shape.getId().getName() + " response").build());
        }

        for (Map.Entry<String, ResponseObject> entry : originalResponses.entrySet()) {
            String status = entry.getKey();
            ResponseObject responseObject = plugin.updateResponse(context, shape, entry.getValue());
            newResponses.put(status, responseObject);
        }

        return !newResponses.equals(operation.getResponses())
               ? operation.toBuilder().responses(newResponses).build()
               : operation;
    }

    private void addSecurityComponents(
            Context context,
            OpenApi.Builder openApiBuilder,
            ComponentsObject.Builder components,
            SmithyOpenApiPlugin plugin
    ) {
        OptionalUtils.ifPresentOrElse(
                context.getService().getTrait(ProtocolsTrait.class),
                trait -> {
                    for (SecuritySchemeConverter converter : context.getSecuritySchemeConverters()) {
                        String securityName = converter.getSecurityName(context);
                        String authName = converter.getAuthSchemeName();
                        SecurityScheme createdScheme = converter.createSecurityScheme(context);
                        SecurityScheme securityScheme = plugin.updateSecurityScheme(
                                context, authName, securityName, createdScheme);
                        if (securityScheme != null) {
                            components.putSecurityScheme(securityName, securityScheme);
                        }
                    }
                },
                () -> LOGGER.warning("No `protocols` trait found on service while converting to OpenAPI")
        );

        // Add service-wide security requirements.
        AuthIndex authIndex = context.getModel().getKnowledge(AuthIndex.class);
        List<String> schemes = authIndex.getDefaultServiceSchemes(context.getService());
        for (SecuritySchemeConverter converter : findMatchingConverters(context, schemes)) {
            List<String> result = converter.createSecurityRequirements(context, context.getService());
            openApiBuilder.addSecurity(MapUtils.of(converter.getSecurityName(context), result));
        }
    }

    private Collection<SecuritySchemeConverter> findMatchingConverters(Context context, Collection<String> schemes) {
        return context.getSecuritySchemeConverters().stream()
                .filter(converter -> schemes.contains(converter.getAuthSchemeName()))
                .collect(Collectors.toList());
    }
}
