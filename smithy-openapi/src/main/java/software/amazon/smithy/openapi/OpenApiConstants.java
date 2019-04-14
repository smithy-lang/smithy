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

package software.amazon.smithy.openapi;

import software.amazon.smithy.build.JsonSubstitutions;
import software.amazon.smithy.model.node.ArrayNode;

public final class OpenApiConstants {
    /** The supported version of OpenAPI. */
    public static final String VERSION = "3.0.2";

    /** The Smithy service Shape ID being converted. */
    public static final String SERVICE = "service";

    /** Whether or not to include tags in the result. */
    public static final String OPEN_API_TAGS = "openapi.tags";

    /** Limits the exported tags to a specific set of tags. The value must be an {@link ArrayNode} of Strings. */
    public static final String OPEN_API_SUPPORTED_TAGS = "openapi.supportedTags";

    /**
     * Configures JSON schema shapes to use OpenAPI features and not use
     * unsupported OpenAPI features.
     *
     * @see <a href="https://swagger.io/docs/specification/data-models/keywords/">Supported JSON Schema Keywords</a>
     */
    public static final String OPEN_API_MODE = "openapi.mode";

    /** Adds support for "nullable" from OpenAPI, added for any boxed primitive. */
    public static final String OPEN_API_USE_NULLABLE = "openapi.use.nullable";

    /** Adds support for OpenAPI's "deprecated" keyword. */
    public static final String OPEN_API_USE_DEPRECATED = "openapi.use.deprecated";

    /** Adds support for OpenAPI's "externalDocs" keyword. */
    public static final String OPEN_API_USE_EXTERNAL_DOCS = "openapi.use.externalDocs";

    /** Adds support for OpenAPI's custom JSON Schema formats. */
    public static final String OPEN_API_USE_FORMATS = "openapi.use.formats";

    /**
     * Sets the default format of blob shapes when used with {@link #OPEN_API_USE_FORMATS}.
     *
     * <p>Defaults to "byte", meaning Base64 encoded.</p>
     */
    public static final String OPEN_API_DEFAULT_BLOB_FORMAT = "openapi.defaultBlobFormat";

    /** Adds support for "xml" from OpenAPI. TODO: Not implemented. */
    public static final String OPEN_API_USE_XML = "openapi.use.xml";

    /** The JSON pointer to where OpenAPI schema components should be written. */
    public static final String SCHEMA_COMPONENTS_POINTER = "#/components/schemas";

    /** Set to true to prevent unused components from being removed from the artifact. */
    public static final String OPENAPI_KEEP_UNUSED_COMPONENTS = "openapi.keepUnusedComponents";

    /** The content-type to use with aws.json and aws.rest-json protocols. */
    public static final String AWS_JSON_CONTENT_TYPE = "openapi.aws.jsonContentType";

    /**
     * Defines if greedy URI path labels are forbidden. By default, greedy
     * labels will appear as-is in the path generated for an operation.
     * For example, "/{foo+}"
     */
    public static final String FORBID_GREEDY_LABELS = "openapi.forbidGreedyLabels";

    /**
     * Determines what to do when the {@code httpPrefixHeaders} trait is found
     * in a model. OpenAPI does not support httpPrefixHeaders. By default, the
     * conversion will fail. This setting must be set to a string. The following
     * string values are supported generally, though additional values may be
     * supported by other mappers or protocols.
     *
     * <ul>
     *     <li>FAIL: The default setting that causes the build to fail.</li>
     *     <li>WARN: The header is omitted from the OpenAPI model and a warning is logged.</li>
     * </ul>
     *
     * TODO: Should we add something like "STAR" to append a "*" to the header name and reference an object?
     */
    public static final String ON_HTTP_PREFIX_HEADERS = "openapi.onHttpPrefixHeaders";
    public static final String ON_HTTP_PREFIX_HEADERS_FAIL = "FAIL";
    public static final String ON_HTTP_PREFIX_HEADERS_WARN = "WARN";

    /** Ignores unsupported trait like eventStream and logs them rather than fail. */
    public static final String IGNORE_UNSUPPORTED_TRAIT = "openapi.ignoreUnsupportedTrait";

    /**
     * The prefix used to provide a custom OpenAPI security scheme name for
     * Smithy authentication schemes, specifically, keys with this prefix
     * can change the key used to register a security scheme in the
     * "#/components/securitySchemes" section of the model.
     *
     * <p>By default, if no security scheme name is found using this prefix
     * concatenated with the Smithy authentication scheme name, then the name
     * of the Smithy authentication scheme is used.
     */
    public static final String SECURITY_NAME_PREFIX = "openapi.security.name.";

    /**
     * Defines a map of String to any Node value to find and replace in the
     * generated OpenAPI model.
     *
     * <p>String values are replaced if the string in its entirety matches
     * one of the keys provided in the {@code openapi.substitutions} map. The
     * corresponding value is then substituted for the string-- this could even
     * result in a string changing into an object, array, etc.
     *
     * @see JsonSubstitutions
     */
    public static final String SUBSTITUTIONS = "openapi.substitutions";

    private OpenApiConstants() {}
}
