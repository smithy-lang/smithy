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
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.openapi.model.SecurityScheme;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Converts a Smithy authentication scheme to an OpenAPI security
 * scheme and applies security requirements to operations.
 *
 * <p>Implementations of this interface are discovered using Java SPI.
 */
public interface SecuritySchemeConverter {
    /**
     * Get the name of the Smithy auth scheme that matches this converter.
     *
     * @return The Smithy security auth scheme name.
     */
    String getAuthSchemeName();

    /**
     * Creates an OpenAPI security scheme.
     *
     * @param context Conversion context.
     * @return The generated security scheme
     *
     * @see <a href="https://swagger.io/specification/#securitySchemeObject">Security Scheme Object</a>
     */
    SecurityScheme createSecurityScheme(Context context);

    /**
     * Get the name that should be used for this security scheme throughout
     * the model.
     *
     * <p>By default, this method will return the result of
     * {@link #getAuthSchemeName()}
     *
     * @param context Conversion context.
     * @return The Smithy security authentication scheme name.
     */
    default String getSecurityName(Context context) {
        return getAuthSchemeName();
    }

    /**
     * Creates a "security" requirements property to apply to an operation
     * or top-level service using the return value of {@link #getSecurityName}
     * as the security scheme name.
     *
     * <p>The default implementation will return an empty list.
     *
     * @param context OpenAPI context
     * @param shape Service or operation shape.
     * @return The security requirements value.
     */
    default List<String> createSecurityRequirements(Context context, Shape shape) {
        return ListUtils.of();
    }

    /**
     * Gets the names of the headers set on HTTP requests used by this
     * authentication scheme.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @return A set of header names.
     */
    default Set<String> getAuthRequestHeaders() {
        return SetUtils.of();
    }

    /**
     * Gets the names of the headers set on HTTP responses used by this
     * authentication scheme.
     *
     * <p>This is useful when integrating with things like CORS.</p>
     *
     * @return A set of header names.
     */
    default Set<String> getAuthResponseHeaders() {
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
