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
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.openapi.fromsmithy.mappers.CheckForGreedyLabels;
import software.amazon.smithy.openapi.fromsmithy.mappers.CheckForPrefixHeaders;
import software.amazon.smithy.openapi.fromsmithy.mappers.InlineReferencesToPrimitiveTypes;
import software.amazon.smithy.openapi.fromsmithy.mappers.OpenApiJsonSubstitutions;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveEmptyComponents;
import software.amazon.smithy.openapi.fromsmithy.mappers.RemoveUnusedComponents;
import software.amazon.smithy.openapi.fromsmithy.mappers.UnsupportedTraits;
import software.amazon.smithy.openapi.fromsmithy.protocols.AwsRestJsonProtocol;
import software.amazon.smithy.openapi.fromsmithy.security.AwsV4;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBasic;
import software.amazon.smithy.openapi.fromsmithy.security.HttpBearer;
import software.amazon.smithy.openapi.fromsmithy.security.HttpDigest;
import software.amazon.smithy.openapi.fromsmithy.security.XApiKey;
import software.amazon.smithy.utils.ListUtils;

/**
 * Registers the core Smithy2OpenApi functionality.
 */
public final class CoreExtension implements Smithy2OpenApiExtension {
    @Override
    public List<SecuritySchemeConverter> getSecuritySchemeConverters() {
        return ListUtils.of(
            new AwsV4(),
            new HttpBasic(),
            new HttpBearer(),
            new HttpDigest(),
            new XApiKey()
        );
    }

    @Override
    public List<OpenApiProtocol> getProtocols() {
        return ListUtils.of(new AwsRestJsonProtocol());
    }

    @Override
    public List<OpenApiMapper> getOpenApiMappers() {
        return ListUtils.of(
                new CheckForGreedyLabels(),
                new CheckForPrefixHeaders(),
                new OpenApiJsonSubstitutions(),
                new RemoveUnusedComponents(),
                new UnsupportedTraits(),
                new InlineReferencesToPrimitiveTypes(),
                new RemoveEmptyComponents()
        );
    }

    @Override
    public List<JsonSchemaMapper> getJsonSchemaMappers() {
        return ListUtils.of(new OpenApiJsonSchemaMapper());
    }
}
