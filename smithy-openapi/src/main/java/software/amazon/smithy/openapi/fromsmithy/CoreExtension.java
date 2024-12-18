/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.util.List;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.mappers.CheckForGreedyLabels;
import software.amazon.smithy.openapi.fromsmithy.mappers.CheckForPrefixHeaders;
import software.amazon.smithy.openapi.fromsmithy.mappers.OpenApiJsonAdd;
import software.amazon.smithy.openapi.fromsmithy.mappers.OpenApiJsonSubstitutions;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveEmptyComponents;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponents;
import software.amazon.smithy.openapi.fromsmithy.mappers.SpecificationExtensionsMapper;
import software.amazon.smithy.openapi.fromsmithy.mappers.UnsupportedTraits;
import software.amazon.smithy.openapi.fromsmithy.protocols.AwsRestJson1Protocol;
import software.amazon.smithy.openapi.fromsmithy.security.AwsV4Converter;
import software.amazon.smithy.openapi.fromsmithy.security.HttpApiKeyAuthConverter;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBasicConverter;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBearerConverter;
import software.amazon.smithy.openapi.fromsmithy.security.HttpDigestConverter;
import software.amazon.smithy.utils.ListUtils;

/**
 * Registers the core Smithy2OpenApi functionality.
 */
public final class CoreExtension implements Smithy2OpenApiExtension {
    @Override
    public List<SecuritySchemeConverter<? extends Trait>> getSecuritySchemeConverters() {
        return ListUtils.of(
                new HttpBasicConverter(),
                new HttpBearerConverter(),
                new HttpDigestConverter(),
                new AwsV4Converter(),
                new HttpApiKeyAuthConverter());
    }

    @Override
    public List<OpenApiProtocol<? extends Trait>> getProtocols() {
        return ListUtils.of(new AwsRestJson1Protocol());
    }

    @Override
    public List<OpenApiMapper> getOpenApiMappers() {
        return ListUtils.of(
                new CheckForGreedyLabels(),
                new CheckForPrefixHeaders(),
                new OpenApiJsonSubstitutions(),
                new OpenApiJsonAdd(),
                new RemoveUnusedComponents(),
                new UnsupportedTraits(),
                new RemoveEmptyComponents(),
                new SpecificationExtensionsMapper());
    }

    @Override
    public List<JsonSchemaMapper> getJsonSchemaMappers() {
        return ListUtils.of(new OpenApiJsonSchemaMapper());
    }
}
