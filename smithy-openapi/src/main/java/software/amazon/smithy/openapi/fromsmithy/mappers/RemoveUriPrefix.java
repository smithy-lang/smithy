/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.fromsmithy.mappers;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Removes a configurable prefix from every Smithy operation defined
 * in the model.
 *
 * <p>This plugin only works on operations that have the http trait.
 * It does not affect any other kind of modeled operation nor does it
 * affect OpenAPI paths created by other means like jsonAdd,
 * schemaExtensions, or other plugins.
 */
@SmithyInternalApi
public class RemoveUriPrefix implements OpenApiMapper {
    @Override
    public Model preprocessModel(Model model, OpenApiConfig config) {
        String removeUriPrefix = config.getRemoveUriPrefix();
        if (StringUtils.isEmpty(removeUriPrefix)) {
            return model;
        }

        return ModelTransformer.create().mapShapes(model, shape -> {
            return shape.asOperationShape().map(operation -> {
                if (operation.hasTrait(HttpTrait.class)) {
                    HttpTrait httpTrait = operation.expectTrait(HttpTrait.class);
                    if (httpTrait.getUri().toString().startsWith(removeUriPrefix)) {
                        String updatedUri = httpTrait.getUri().toString().substring(removeUriPrefix.length());
                        if (!updatedUri.startsWith("/")) {
                            updatedUri = "/" + updatedUri;
                        }
                        UriPattern uriPattern = UriPattern.parse(updatedUri);
                        operation = operation.toBuilder()
                                .addTrait(httpTrait.toBuilder().uri(uriPattern).build())
                                .build();
                    }
                }
                return (Shape) operation;
            }).orElse(shape);
        });
    }
}
