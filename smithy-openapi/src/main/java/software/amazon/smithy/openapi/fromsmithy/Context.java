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

import java.util.List;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
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

    public Context(
            Model model,
            ServiceShape service,
            JsonSchemaConverter jsonSchemaConverter,
            OpenApiProtocol<T> openApiProtocol,
            SchemaDocument schemas,
            List<SecuritySchemeConverter<? extends Trait>> securitySchemeConverters
    ) {
        this.model = model;
        this.service = service;
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
     * Gets the queryable configuration object used for the conversion.
     *
     * <p>Plugins can query this object for configuration values.
     *
     * @return Returns the configuration object.
     */
    public ObjectNode getConfig() {
        return jsonSchemaConverter.getConfig();
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
     * Gets the the JSON pointer string to a specific shape.
     *
     * @param shapeId Shape ID to convert into a JSON pointer to a schema component.
     * @return Returns the JSON pointer to this shape as a schema component.
     */
    public String getPointer(ToShapeId shapeId) {
        return getJsonSchemaConverter().getRefStrategy().toPointer(shapeId.toShapeId(), getConfig());
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
}
