/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;

/**
 * Smithy to OpenAPI conversion context object.
 */
public final class Context<T extends Trait> {

    private final Model model;
    private final ServiceShape service;
    private final JsonSchemaConverter jsonSchemaConverter;
    private final T protocolTrait;
    private final OpenApiProtocol<T> openApiProtocol;
    private final SchemaDocument schemas;
    private final List<SecuritySchemeConverter<? extends Trait>> securitySchemeConverters;
    private Map<String, Schema> synthesizedSchemas = Collections.synchronizedMap(new TreeMap<>());
    private OpenApiConfig config;

    Context(
            Model model,
            ServiceShape service,
            OpenApiConfig config,
            JsonSchemaConverter jsonSchemaConverter,
            OpenApiProtocol<T> openApiProtocol,
            SchemaDocument schemas,
            List<SecuritySchemeConverter<? extends Trait>> securitySchemeConverters
    ) {
        this.model = model;
        this.service = service;
        this.config = config;
        this.jsonSchemaConverter = jsonSchemaConverter;
        this.protocolTrait = service.expectTrait(openApiProtocol.getProtocolType());
        this.openApiProtocol = openApiProtocol;
        this.schemas = schemas;
        this.securitySchemeConverters = securitySchemeConverters;
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
     * Gets the service shape being converted.
     *
     * @return Returns the service shape.
     */
    public ServiceShape getService() {
        return service;
    }

    /**
     * Gets the configuration object used for the conversion.
     *
     * <p>Plugins can query this object for configuration values.
     *
     * @return Returns the configuration object.
     */
    public OpenApiConfig getConfig() {
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
     * Gets the protocol trait that is being converted.
     *
     * @return Returns the protocol ID.
     */
    public T getProtocolTrait() {
        return protocolTrait;
    }

    /**
     * Gets the OpenAPI protocol conversion object.
     *
     * @return Returns the OpenAPI protocol.
     */
    public OpenApiProtocol<T> getOpenApiProtocol() {
        return openApiProtocol;
    }

    /**
     * Gets a converted schema from the context object by JSON pointer
     * and throws if the schema does not exist.
     *
     * @param pointer Schema to retrieve by JSON pointer (e.g., "#/components/schemas/foo").
     * @return Returns the found schema.
     * @throws OpenApiException if the schema cannot be found.
     */
    public Schema getSchema(String pointer) {
        return schemas.getDefinition(pointer)
                .orElseThrow(() -> new OpenApiException("Expected JSON schema definition not found: " + pointer));
    }

    /**
     * Gets the JSON pointer string to a specific shape.
     *
     * @param shapeId Shape ID to convert into a JSON pointer to a schema component.
     * @return Returns the JSON pointer to this shape as a schema component.
     */
    public String getPointer(ToShapeId shapeId) {
        return getJsonSchemaConverter().toPointer(shapeId.toShapeId());
    }

    /**
     * Creates a schema that contains a $ref that points to a schema component.
     *
     * @param shapeId Shape ID to point to with a $ref schema.
     * @return Returns the creates schema.
     */
    public Schema createRef(ToShapeId shapeId) {
        return Schema.builder().ref(getPointer(shapeId)).build();
    }

    /**
     * Gets the exiting schema of the shape if it's meant to be inlined,
     * otherwise creates a $ref to the shape if it is meant to be reused
     * across the generated schema.
     *
     * @param member Member to inline or reference.
     * @return Returns the schema for the member.
     */
    public Schema inlineOrReferenceSchema(MemberShape member) {
        if (getJsonSchemaConverter().isInlined(member)) {
            return getJsonSchemaConverter().convertShape(member).getRootSchema();
        } else {
            return createRef(member);
        }
    }

    /**
     * Gets the security scheme converters that are compatible with the
     * selected protocol.
     *
     * @return Returns the security scheme converters.
     */
    public List<SecuritySchemeConverter<? extends Trait>> getSecuritySchemeConverters() {
        return securitySchemeConverters;
    }

    /**
     * Reports if any authentication mechanisms in the entire model use HTTP
     * credentials, such as cookies, browser-managed usernames and passwords,
     * or TLS client certificates.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @return Whether any authentication mechanism relies on browser-managed credentials.
     * @see <a href="https://fetch.spec.whatwg.org/#credentials" target="_blank">Browser-managed credentials</a>
     */
    public boolean usesHttpCredentials() {
        return getSecuritySchemeConverters().stream().anyMatch(SecuritySchemeConverter::usesHttpCredentials);
    }

    /**
     * Gets an alphabetically sorted set of request headers used by every
     * security scheme associated with the API.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @return Returns the set of every request header used by every security scheme.
     */
    public Set<String> getAllSecuritySchemeRequestHeaders() {
        Set<String> headers = new TreeSet<>();
        for (SecuritySchemeConverter<?> converter : getSecuritySchemeConverters()) {
            headers.addAll(getSecuritySchemeRequestHeaders(this, converter));
        }
        return headers;
    }

    /**
     * Gets an alphabetically sorted set of response headers used by every
     * security scheme associated with the API.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @return Returns the set of every response header used by every security scheme.
     */
    public Set<String> getAllSecuritySchemeResponseHeaders() {
        Set<String> headers = new TreeSet<>();
        for (SecuritySchemeConverter<?> converter : getSecuritySchemeConverters()) {
            headers.addAll(getSecuritySchemeResponseHeaders(this, converter));
        }
        return headers;
    }

    private static <T extends Trait> Set<String> getSecuritySchemeRequestHeaders(
            Context<? extends Trait> context,
            SecuritySchemeConverter<T> converter
    ) {
        T t = context.getService().expectTrait(converter.getAuthSchemeType());
        return converter.getAuthRequestHeaders(context, t);
    }

    private static <T extends Trait> Set<String> getSecuritySchemeResponseHeaders(
            Context<? extends Trait> context,
            SecuritySchemeConverter<T> converter
    ) {
        T t = context.getService().expectTrait(converter.getAuthSchemeType());
        return converter.getAuthResponseHeaders(context, t);
    }

    /**
     * Gets all of the synthesized schemas that needed to be created while
     * generating the OpenAPI artifact.
     *
     * @return Returns the "synthesized" schemas as an immutable map.
     */
    public Map<String, Schema> getSynthesizedSchemas() {
        return Collections.unmodifiableMap(synthesizedSchemas);
    }

    /**
     * Puts a new synthesized schema that is needed to convert to OpenAPI.
     *
     * <p>Synthesized schemas are used when ad-hoc schemas are necessary in
     * order to materialize some change in OpenAPI while still providing an
     * explicit name. For example, when generating many of the RESTful
     * protocols, members from the input of an operation might come together
     * to form the payload of a request. In Smithy, it's fine to use only
     * part of the input to derive the payload, whereas in OpenAPI, you need
     * a schema that's dedicated to the payload.
     *
     * <p>The primary alternative to synthesized schemas is inlined schema
     * definitions. The problem with inline schemas is that they don't
     * have an explicit or even deterministic name when used with other
     * platforms (for example, API Gateway will generate a random name for
     * an object if one is not given).
     *
     * <p>This method is thread-safe.
     *
     * @param name Name of the schema to put into components/schemas. Nested
     *   pointers are not supported.
     * @param schema The schema to put.
     * @return Returns a JSON pointer to the created schema.
     */
    public String putSynthesizedSchema(String name, Schema schema) {
        synthesizedSchemas.put(Objects.requireNonNull(name), Objects.requireNonNull(schema));
        return config.getDefinitionPointer() + "/" + name;
    }
}
