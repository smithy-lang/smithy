/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiUtils;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.traits.SpecificationExtensionTrait;

/**
 * Maps trait shapes tagged with {@link SpecificationExtensionTrait} into <a href="https://spec.openapis.org/oas/v3.1.0#specification-extensions">OpenAPI specification extensions</a>.
 */
public class SpecificationExtensionsMapper implements OpenApiMapper {
    /**
     * Attach Specification Extensions to Service.
     */
    @Override
    public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        openapi.getExtensions()
                .putAll(
                        OpenApiUtils.getSpecificationExtensionsMap(context.getModel(), context.getService()));
        return openapi;
    }

    /**
     * Attach Specification Extensions to Operation.
     */
    @Override
    public OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        operation.getExtensions().putAll(OpenApiUtils.getSpecificationExtensionsMap(context.getModel(), shape));
        return operation;
    }
}
