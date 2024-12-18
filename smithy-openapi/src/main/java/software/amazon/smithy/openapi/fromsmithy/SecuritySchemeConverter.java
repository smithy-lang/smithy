/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Converts a Smithy authentication scheme to an OpenAPI security
 * scheme and applies security requirements to operations.
 *
 * <p>Implementations of this interface are discovered using Java SPI.
 *
 * @param <T> the auth scheme trait to convert.
 */
public interface SecuritySchemeConverter<T extends Trait> {
    /**
     * Get the U that matches this converter.
     *
     * @return The Smithy security auth scheme ID.
     */
    Class<T> getAuthSchemeType();

    /**
     * Gets the shape ID of the auth scheme type.
     *
     * <p>By default, this operation uses reflection to get the value of
     * a static property of the auth scheme class named "ID". If that is
     * not how a specific auth scheme class is implemented, then this
     * method must be overridden.
     *
     * @return Returns the auth scheme's shape ID.
     */
    default ShapeId getAuthSchemeId() {
        try {
            Class<T> type = getAuthSchemeType();
            return (ShapeId) type.getField("ID").get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to find an ID property on " + getAuthSchemeType().getName());
        }
    }

    /**
     * Creates an OpenAPI security scheme.
     *
     * @param context Conversion context.
     * @param authTrait Authentication trait to convert.
     * @return The generated security scheme
     *
     * @see <a href="https://swagger.io/specification/#securitySchemeObject">Security Scheme Object</a>
     */
    SecurityScheme createSecurityScheme(Context<? extends Trait> context, T authTrait);

    /**
     * Creates a "security" requirements property to apply to an operation
     * or top-level service using the Smithy auth scheme name as the key.
     *
     * <p>The default implementation will return an empty list.
     *
     * @param context OpenAPI context
     * @param authTrait Authentication trait to convert.
     * @param shape Service or operation shape.
     * @return The security requirements value.
     */
    default List<String> createSecurityRequirements(Context<? extends Trait> context, T authTrait, Shape shape) {
        return ListUtils.of();
    }

    /**
     * Gets the name of OpenApi auth scheme.
     *
     * <p>For compatibility with Amazon API Gateway, the `#` is replaced with
     * an `.` when deriving the name from the auth scheme's shape ID.
     *
     * @return Returns the auth scheme's name.
     */
    default String getOpenApiAuthSchemeName() {
        return getAuthSchemeId().toString().replace("#", ".");
    }

    /**
     * Gets the names of the headers set on HTTP requests used by this
     * authentication scheme.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @param context Context for the conversion.
     * @param authTrait The auth trait that is being used.
     * @return A set of header names.
     */
    default Set<String> getAuthRequestHeaders(Context<? extends Trait> context, T authTrait) {
        return SetUtils.of();
    }

    /**
     * Gets the names of the headers set on HTTP responses used by this
     * authentication scheme.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @param context Context for the conversion.
     * @param authTrait The auth trait that is being used.
     * @return A set of header names.
     */
    default Set<String> getAuthResponseHeaders(Context<? extends Trait> context, T authTrait) {
        return SetUtils.of();
    }

    /**
     * Reports if this authentication mechanism uses HTTP credentials, such as
     * cookies, browser-managed usernames and passwords, or TLS client
     * certificates.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @return Whether this authentication mechanism relies on browser-managed credentials
     *
     * @see <a href="https://fetch.spec.whatwg.org/#credentials" target="_blank">Browser-managed credentials</a>
     */
    default boolean usesHttpCredentials() {
        return false;
    }
}
