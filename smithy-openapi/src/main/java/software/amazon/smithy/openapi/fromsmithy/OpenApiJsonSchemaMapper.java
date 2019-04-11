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

import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaBuilderMapper;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.utils.SetUtils;

/**
 * Applies OpenAPI extensions to a {@link Schema}.
 *
 * <p>This mapper understands the following setting:
 *
 * <ul>
 *     <li>{@link OpenApiConstants#OPEN_API_MODE}: Enables all features.</li>
 *     <li>{@link OpenApiConstants#OPEN_API_USE_EXTERNAL_DOCS}: Adds the external
 *     docs property if a shape has the {@code externalDocumentation} trait.</li>
 *     <li>{@link OpenApiConstants#OPEN_API_USE_NULLABLE}: Adds the
 *     {@code nullable} property if a shape is boxed.</li>
 *     <li>{@link OpenApiConstants#OPEN_API_USE_DEPRECATED}: Adds the
 *     {@code deprecated} property if a shape has the corresponding trait.</li>
 *     <li>{@link OpenApiConstants#OPEN_API_USE_XML}: Uses XML traits to
 *     place OpenAPI XML definitions on schemas.</li>
 * </ul>
 */
public class OpenApiJsonSchemaMapper implements SchemaBuilderMapper {
    private static final Logger LOGGER = Logger.getLogger(OpenApiJsonSchemaMapper.class.getName());
    private static final String DEFAULT_BLOB_FORMAT = "byte";

    /** See https://swagger.io/docs/specification/data-models/keywords/. */
    private static final Set<String> UNSUPPORTED_KEYWORD_DIRECTIVES = SetUtils.of(
            "disable.propertyNames",
            "disable.contentMediaType");

    @Override
    public Schema.Builder updateSchema(Shape shape, Schema.Builder builder, ObjectNode config) {
        boolean enabled = config.getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_MODE);

        if (enabled || config.getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_USE_EXTERNAL_DOCS)) {
            shape.getTrait(ExternalDocumentationTrait.class)
                    .map(ExternalDocumentationTrait::getValue)
                    .ifPresent(docs -> builder.putExtension("externalDocs", Node.from(docs)));
        }

        if (enabled || config.getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_USE_NULLABLE)) {
            if (shape.hasTrait(BoxTrait.class)) {
                builder.putExtension("nullable", Node.from(true));
            }
        }

        if (enabled || config.getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_USE_DEPRECATED)) {
            if (shape.hasTrait(DeprecatedTrait.class)) {
                builder.putExtension("deprecated", Node.from(true));
            }
        }

        if (enabled || config.getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_USE_FORMATS)) {
            // Don't overwrite an existing format setting.
            if (builder.getFormat().isEmpty()) {
                if (shape.isIntegerShape()) {
                    builder.format("int32");
                } else if (shape.isLongShape()) {
                    builder.format("int64");
                } else if (shape.isFloatShape()) {
                    builder.format("float");
                } else if (shape.isDoubleShape()) {
                    builder.format("double");
                } else if (shape.isBlobShape()) {
                    return builder.format(config.getStringMemberOrDefault(
                            OpenApiConstants.OPEN_API_DEFAULT_BLOB_FORMAT, DEFAULT_BLOB_FORMAT));
                } else if (shape.hasTrait(SensitiveTrait.class)) {
                    builder.format("password");
                }
            }
        }

        if (config.getBooleanMemberOrDefault(OpenApiConstants.OPEN_API_USE_XML)) {
            LOGGER.warning(OpenApiConstants.OPEN_API_USE_XML + " is not yet implemented");
        }

        // Remove unsupported JSON Schema keywords.
        if (enabled) {
            UNSUPPORTED_KEYWORD_DIRECTIVES.forEach(builder::disableProperty);
        }

        return builder;
    }
}
