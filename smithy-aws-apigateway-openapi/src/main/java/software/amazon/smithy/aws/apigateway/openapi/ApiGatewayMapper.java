/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * An API Gateway mapper that only applies when the type of API being
 * converted matches the types of APIs handled by the mapper.
 */
public interface ApiGatewayMapper extends OpenApiMapper {

    /**
     * Gets the types of API Gateway APIs that this mapper applies to.
     *
     * <p>Return an empty list or null to apply to all possible API types
     * other than {@link ApiGatewayConfig.ApiType#DISABLED}. However, note
     * that it's typically safer to specify the exact API types that the
     * mapper supports.
     *
     * @return Returns the list of API Gateway API types to apply to.
     */
    List<ApiGatewayConfig.ApiType> getApiTypes();

    /**
     * Wraps and delegates to an {@link ApiGatewayMapper} IFF the configured
     * {@link ApiGatewayConfig.ApiType} matches the types of APIs that the
     * wrapped mapper applies to.
     *
     * @param delegate Mapper to delegate to when it applies to the configured API type.
     * @return Returns the wrapped mapper.
     */
    static OpenApiMapper wrap(ApiGatewayMapper delegate) {
        return new OpenApiMapper() {

            private boolean matchesApiType(Context<?> context) {
                return matchesApiType(context.getConfig());
            }

            private boolean matchesApiType(OpenApiConfig openApiConfig) {
                ApiGatewayConfig config = openApiConfig.getExtensions(ApiGatewayConfig.class);
                ApiGatewayConfig.ApiType setting = config.getApiGatewayType();

                // Never apply a mapper if API Gateway mappers are disabled. For
                // example, if a dependency brings them in on the classpath and that
                // dependency can't be removed.
                if (setting == ApiGatewayConfig.ApiType.DISABLED) {
                    return false;
                }

                List<ApiGatewayConfig.ApiType> supported = delegate.getApiTypes();

                // Handle the case where the mapper supports any API type.
                if (supported == null || supported.isEmpty()) {
                    return true;
                }

                return supported.contains(setting);
            }

            @Override
            public byte getOrder() {
                return delegate.getOrder();
            }

            @Override
            public void updateDefaultSettings(Model model, OpenApiConfig config) {
                if (matchesApiType(config)) {
                    delegate.updateDefaultSettings(model, config);
                }
            }

            @Override
            public OperationObject updateOperation(
                    Context<? extends Trait> context,
                    OperationShape shape,
                    OperationObject operation,
                    String httpMethodName,
                    String path
            ) {
                return matchesApiType(context)
                        ? delegate.updateOperation(context, shape, operation, httpMethodName, path)
                        : operation;
            }

            @Override
            public OperationObject postProcessOperation(
                    Context<? extends Trait> context,
                    OperationShape shape,
                    OperationObject operation,
                    String httpMethodName,
                    String path
            ) {
                return matchesApiType(context)
                        ? delegate.postProcessOperation(context, shape, operation, httpMethodName, path)
                        : operation;
            }

            @Override
            public PathItem updatePathItem(Context<? extends Trait> context, String path, PathItem pathItem) {
                return matchesApiType(context)
                        ? delegate.updatePathItem(context, path, pathItem)
                        : pathItem;
            }

            @Override
            public ParameterObject updateParameter(
                    Context<? extends Trait> context,
                    OperationShape operation,
                    String httpMethodName,
                    String path,
                    ParameterObject parameterObject
            ) {
                return matchesApiType(context)
                        ? delegate.updateParameter(context, operation, httpMethodName, path, parameterObject)
                        : parameterObject;
            }

            @Override
            public RequestBodyObject updateRequestBody(
                    Context<? extends Trait> context,
                    OperationShape operation,
                    String httpMethodName,
                    String path,
                    RequestBodyObject requestBody
            ) {
                return matchesApiType(context)
                        ? delegate.updateRequestBody(context, operation, httpMethodName, path, requestBody)
                        : requestBody;
            }

            @Override
            public ResponseObject updateResponse(
                    Context<? extends Trait> context,
                    OperationShape operation,
                    String status,
                    String httpMethodName,
                    String path,
                    ResponseObject response
            ) {
                return matchesApiType(context)
                        ? delegate.updateResponse(context, operation, status, httpMethodName, path, response)
                        : response;
            }

            @Override
            public void before(Context<? extends Trait> context, OpenApi.Builder builder) {
                if (matchesApiType(context)) {
                    delegate.before(context, builder);
                }
            }

            @Override
            public SecurityScheme updateSecurityScheme(
                    Context<? extends Trait> context,
                    Trait authTrait,
                    SecurityScheme securityScheme
            ) {
                return matchesApiType(context)
                        ? delegate.updateSecurityScheme(context, authTrait, securityScheme)
                        : securityScheme;
            }

            @Override
            public Map<String, List<String>> updateSecurity(
                    Context<? extends Trait> context,
                    Shape shape,
                    SecuritySchemeConverter<? extends Trait> converter,
                    Map<String, List<String>> requirement
            ) {
                return matchesApiType(context)
                        ? delegate.updateSecurity(context, shape, converter, requirement)
                        : requirement;
            }

            @Override
            public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
                return matchesApiType(context)
                        ? delegate.after(context, openapi)
                        : openapi;
            }

            @Override
            public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
                return matchesApiType(context)
                        ? delegate.updateNode(context, openapi, node)
                        : node;
            }
        };
    }
}
