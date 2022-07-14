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

import static java.util.function.Function.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.model.ExternalDocumentation;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Applies OpenAPI extensions to a {@link Schema} using configuration settings
 * found in {@link OpenApiConfig}.
 *
 * <p>Note: the properties and features added by this mapper can be removed using
 * {@link OpenApiConfig#setDisableFeatures}.
 */
public final class OpenApiJsonSchemaMapper implements JsonSchemaMapper {

    /** See https://swagger.io/docs/specification/data-models/keywords/. */
    private static final Set<String> UNSUPPORTED_KEYWORD_DIRECTIVES = SetUtils.of(
            "propertyNames",
            "contentMediaType");

    @Override
    public Schema.Builder updateSchema(Shape shape, Schema.Builder builder, JsonSchemaConfig config) {
        getResolvedExternalDocs(shape, config)
                .map(ExternalDocumentation::toNode)
                .ifPresent(docs -> builder.putExtension("externalDocs", docs));

        if (shape.hasTrait(DeprecatedTrait.class)) {
            builder.putExtension("deprecated", Node.from(true));
        }

        // Handle OpenAPI's custom "integer" type.
        // https://swagger.io/specification/#data-types
        if (shape.isByteShape() || shape.isShortShape() || shape.isIntegerShape() || shape.isLongShape()) {
            if (config instanceof OpenApiConfig && ((OpenApiConfig) config).getUseIntegerType()) {
                builder.type("integer");
            }
        }

        // Don't overwrite an existing format setting.
        if (!builder.getFormat().isPresent()) {
            if (shape.isIntegerShape()) {
                builder.format("int32");
            } else if (shape.isLongShape()) {
                builder.format("int64");
            } else if (shape.isFloatShape()) {
                updateFloatFormat(builder, config, "float");
            } else if (shape.isDoubleShape()) {
                updateFloatFormat(builder, config, "double");
            } else if (shape.isBlobShape()) {
                if (config instanceof OpenApiConfig) {
                    String blobFormat = ((OpenApiConfig) config).getDefaultBlobFormat();
                    return builder.format(blobFormat);
                }
            } else if (shape.isTimestampShape()) {
                // Add the "double" format when epoch-seconds is used
                // to account for optional millisecond precision.
                config.detectJsonTimestampFormat(shape)
                        .filter(format -> format.equals(TimestampFormatTrait.EPOCH_SECONDS))
                        .ifPresent(format -> builder.format("double"));
            } else if (shape.hasTrait(SensitiveTrait.class)) {
                builder.format("password");
            }
        }

        // Remove unsupported JSON Schema keywords.
        UNSUPPORTED_KEYWORD_DIRECTIVES.forEach(builder::disableProperty);

        return builder;
    }

    private void updateFloatFormat(Schema.Builder builder, JsonSchemaConfig config, String format) {
        if (config.getSupportNonNumericFloats()) {
            List<Schema> newOneOf = new ArrayList<>();
            for (Schema schema : builder.build().getOneOf()) {
                if (schema.getType().isPresent() && schema.getType().get().equals("number")) {
                    newOneOf.add(schema.toBuilder().format(format).build());
                } else {
                    newOneOf.add(schema);
                }
            }
            builder.oneOf(newOneOf);
        } else {
            builder.format(format);
        }
    }

    static Optional<ExternalDocumentation> getResolvedExternalDocs(Shape shape, JsonSchemaConfig config) {
        Optional<ExternalDocumentationTrait> traitOptional = shape.getTrait(ExternalDocumentationTrait.class);

        if (!traitOptional.isPresent() || !(config instanceof OpenApiConfig)) {
            return Optional.empty();
        }

        OpenApiConfig openApiConfig = (OpenApiConfig) config;

        // Get the valid list of lower case names to look for when converting.
        List<String> externalDocKeys = new ArrayList<>(openApiConfig.getExternalDocs().size());
        for (String key : openApiConfig.getExternalDocs()) {
            externalDocKeys.add(key.toLowerCase(Locale.ENGLISH));
        }

        // Get lower case keys to check for when converting.
        Map<String, String> traitUrls = traitOptional.get().getUrls();
        Map<String, String> lowercaseKeyMap = traitUrls.keySet().stream()
                .collect(MapUtils.toUnmodifiableMap(i -> i.toLowerCase(Locale.US), identity()));

        for (String externalDocKey : externalDocKeys) {
            // Compare the lower case name, but use the specified name.
            if (lowercaseKeyMap.containsKey(externalDocKey)) {
                String traitKey = lowercaseKeyMap.get(externalDocKey);
                // Return an ExternalDocumentation object assembled from the trait.
                return Optional.of(ExternalDocumentation.builder()
                        .description(traitKey)
                        .url(traitUrls.get(traitKey))
                        .build());
            }
        }

        // We didn't find any external docs with the a name in the specified set.
        return Optional.empty();
    }
}
