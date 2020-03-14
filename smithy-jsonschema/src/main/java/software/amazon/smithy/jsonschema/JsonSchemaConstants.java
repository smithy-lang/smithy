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

package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.node.ObjectNode;

public final class JsonSchemaConstants {
    /**
     * Keeps Smithy namespaces in the converted shape ID that is generated
     * in the definitions map of a JSON Schema document for a shape.
     *
     * <p>This property has no effect if a {@link RefStrategy} is
     * manually configured on a {@link JsonSchemaConverter}.
     */
    public static final String KEEP_NAMESPACES = "keepNamespaces";

    /**
     * Creates shape name pointers that strip out non-alphanumeric characters.
     *
     * <p>This is necessary for compatibility with some vendors like
     * Amazon API Gateway that only allow alphanumeric shape names.
     */
    public static final String ALPHANUMERIC_ONLY_REFS = "alphanumericOnlyRefs";

    /**
     * Uses the value of the jsonName trait when creating JSON schema
     * properties for structure and union shapes.
     *
     * <p>This property has no effect if a {@link PropertyNamingStrategy} is
     * manually configured on a {@link JsonSchemaConverter}.
     */
    public static final String USE_JSON_NAME = "useJsonName";

    /**
     * Sets the assumed timestampFormat trait for timestamps with no
     * timestampFormat trait. The provided value is expected to be a string.
     *
     * <p>Defaults to "date-time" if not set. Can be set to "date-time",
     * "epoch-seconds", or "http-date".
     */
    public static final String DEFAULT_TIMESTAMP_FORMAT = "defaultTimestampFormat";

    /**
     * Configures how Smithy union shapes are converted to JSON Schema.
     *
     * <p>This value expects a string that can be set to one of the following
     * value:
     *
     * <ul>
     *     <li>"oneOf": Converts to a schema that uses "oneOf". This is the
     *     default setting used if not configured.</li>
     *     <li>"object": Converts to an empty object "{}".</li>
     *     <li>"structure": Converts to an object with properties just like
     *     a structure.</li>
     * </ul>
     *
     * <p>Any other value will raise an {@link UnsupportedOperationException}
     * when the first union shape is encountered.
     */
    public static final String UNION_STRATEGY = "unionStrategy";

    /**
     * Configures location of where definitions are written using JSON Pointer.
     *
     * <p>The provided String value MUST start with "#/" and can use nested "/"
     * characters to place schemas in nested object properties. The provided
     * JSON Pointer does not support escaping.
     *
     * <p>Defaults to "#/definitions" if no value is specified. OpenAPI
     * artifacts will want to use "#/components/schemas".
     */
    public static final String DEFINITION_POINTER = "definitionPointer";

    /**
     * Adds custom key-value pairs to the JSON Schema document generated from
     * a Smithy model.
     *
     * <p>The value provided for this configuration setting is required to be
     * a {@link ObjectNode}.
     */
    public static final String SCHEMA_DOCUMENT_EXTENSIONS = "schemaDocumentExtensions";

    /** Strips any instances of "const" from schemas. */
    public static final String DISABLE_CONST = "disable.const";

    /** Strips any instances of "default" from schemas. */
    public static final String DISABLE_DEFAULT = "disable.default";

    /** Strips any instances of "enum" from schemas. */
    public static final String DISABLE_ENUM = "disable.enum";

    /** Strips any instances of "multipleOf" from schemas. */
    public static final String DISABLE_MULTIPLE_OF = "disable.multipleOf";

    /** Strips any instances of "maximum" from schemas. */
    public static final String DISABLE_MAXIMUM = "disable.maximum";

    /** Strips any instances of "exclusiveMaximum" from schemas. */
    public static final String DISABLE_EXCLUSIVE_MAXIMUM = "disable.exclusiveMaximum";

    /** Strips any instances of "minimum" from schemas. */
    public static final String DISABLE_MINIMUM = "disable.minimum";

    /** Strips any instances of "exclusiveMinimum" from schemas. */
    public static final String DISABLE_EXCLUSIVE_MINIMUM = "disable.exclusiveMinimum";

    /** Strips any instances of "maxLength" from schemas. */
    public static final String DISABLE_MAX_LENGTH = "disable.maxLength";

    /** Strips any instances of "minLength" from schemas. */
    public static final String DISABLE_MIN_LENGTH = "disable.minLength";

    /** Strips any instances of "pattern" from schemas. */
    public static final String DISABLE_PATTERN = "disable.pattern";

    /** Strips any instances of "items" from schemas. */
    public static final String DISABLE_ITEMS = "disable.items";

    /** Strips any instances of "maxItems" from schemas. */
    public static final String DISABLE_MAX_ITEMS = "disable.maxItems";

    /** Strips any instances of "minItems" from schemas. */
    public static final String DISABLE_MIN_ITEMS = "disable.minItems";

    /** Strips any instances of "uniqueItems" from schemas. */
    public static final String DISABLE_UNIQUE_ITEMS = "disable.uniqueItems";

    /** Strips any instances of "properties" from schemas. */
    public static final String DISABLE_PROPERTIES = "disable.properties";

    /** Strips any instances of "additionalProperties" from schemas. */
    public static final String DISABLE_ADDITIONAL_PROPERTIES = "disable.additionalProperties";

    /** Strips any instances of "required" from schemas. */
    public static final String DISABLE_REQUIRED = "disable.required";

    /** Strips any instances of "maxProperties" from schemas. */
    public static final String DISABLE_MAX_PROPERTIES = "disable.maxProperties";

    /** Strips any instances of "minProperties" from schemas. */
    public static final String DISABLE_MIN_PROPERTIES = "disable.minProperties";

    /** Strips any instances of "propertyNames" from schemas. */
    public static final String DISABLE_PROPERTY_NAMES = "disable.propertyNames";

    /** Strips any instances of "allOf" from schemas. */
    public static final String DISABLE_ALL_OF = "disable.allOf";

    /** Strips any instances of "anyOf" from schemas. */
    public static final String DISABLE_ANY_OF = "disable.anyOf";

    /** Strips any instances of "oneOf" from schemas. */
    public static final String DISABLE_ONE_OF = "disable.oneOf";

    /** Strips any instances of "not" from schemas. */
    public static final String DISABLE_NOT = "disable.not";

    /** Strips any instances of "title" from schemas. */
    public static final String DISABLE_TITLE = "disable.title";

    /** Strips any instances of "description" from schemas. */
    public static final String DISABLE_DESCRIPTION = "disable.description";

    /** Strips any instances of "format" from schemas. */
    public static final String DISABLE_FORMAT = "disable.format";

    /** Strips any instances of "readOnly" from schemas. */
    public static final String DISABLE_READ_ONLY = "disable.readOnly";

    /** Strips any instances of "writeOnly" from schemas. */
    public static final String DISABLE_WRITE_ONLY = "disable.writeOnly";

    /** Strips any instances of "comment" from schemas. */
    public static final String DISABLE_COMMENT = "disable.comment";

    /** Strips any instances of "contentEncoding" from schemas. */
    public static final String DISABLE_CONTENT_ENCODING = "disable.contentEncoding";

    /** Strips any instances of "contentMediaType" from schemas. */
    public static final String DISABLE_CONTENT_MEDIA_TYPE = "disable.contentMediaType";

    /** Strips any instances of "examples" from schemas. */
    public static final String DISABLE_EXAMPLES = "disable.examples";

    private JsonSchemaConstants() {}
}
