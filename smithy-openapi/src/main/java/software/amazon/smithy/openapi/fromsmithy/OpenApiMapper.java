/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Provides a plugin infrastructure used to hook into the Smithy to OpenAPI
 * conversion process and map over the result.
 *
 * <p>The methods of a plugin are invoked by {@link OpenApiConverter} during
 * the conversion of a model. There is no need to invoke these manually.
 * Implementations may choose to leverage configuration options of the
 * provided context to determine whether or not to enact the plugin.
 */
public interface OpenApiMapper {
    /**
     * Gets the sort order of the plugin from -128 to 127.
     *
     * <p>Plugins are applied according to this sort order. Lower values
     * are executed before higher values (for example, -128 comes before 0,
     * comes before 127). Plugins default to 0, which is the middle point
     * between the minimum and maximum order values.
     *
     * @return Returns the sort order, defaulting to 0.
     */
    default byte getOrder() {
        return 0;
    }

    /**
     * Sets default values on the OpenAPI configuration object.
     *
     * <p>Use this method <strong>and not {@link #before}</strong>
     * to add default settings. Adding default settings in before()
     * is possible, but might be too late in the process for those
     * configuration changes to take effect.
     *
     * @param model Model being converted.
     * @param config Configuration object to modify.
     */
    default void updateDefaultSettings(Model model, OpenApiConfig config) {}

    /**
     * Updates an operation before invoking the plugin system on the contents
     * of the operation (specifically, before {@link #updateParameter},
     * {@link #updateRequestBody}, {@link #updateResponse},
     * {@link #updateRequestBody}, and {@link #postProcessOperation}).
     *
     * @param context Conversion context.
     * @param shape Operation being converted.
     * @param operation OperationObject being built.
     * @param httpMethodName The HTTP method of the operation.
     * @param path The HTTP URI of the operation.
     * @return Returns the updated operation object.
     */
    default OperationObject updateOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        return operation;
    }

    /**
     * Updates an operation after invoking the plugin system on the contents
     * of the operation (specifically, after {@link #updateOperation},
     * {@link #updateParameter}, {@link #updateRequestBody},
     * {@link #updateResponse}, and {@link #updateRequestBody}).
     *
     * @param context Conversion context.
     * @param shape Operation being converted.
     * @param operation OperationObject being built.
     * @param httpMethodName The HTTP method of the operation.
     * @param path The HTTP URI of the operation.
     * @return Returns the updated operation object.
     */
    default OperationObject postProcessOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String httpMethodName,
            String path
    ) {
        return operation;
    }

    /**
     * Updates a path item.
     *
     * @param context Conversion context.
     * @param path Path of the PathItem.
     * @param pathItem Path item being converted.
     * @return Returns the updated path item.
     */
    default PathItem updatePathItem(Context<? extends Trait> context, String path, PathItem pathItem) {
        return pathItem;
    }

    /**
     * Updates a parameter.
     *
     * @param context Conversion context.
     * @param operation Smithy operation being converted.
     * @param httpMethodName The HTTP method that this parameter is bound to.
     * @param path The HTTP URI this parameter is bound to.
     * @param parameterObject Parameter being updated.
     * @return Returns the updated parameter.
     */
    default ParameterObject updateParameter(
            Context<? extends Trait> context,
            OperationShape operation,
            String httpMethodName,
            String path,
            ParameterObject parameterObject
    ) {
        return parameterObject;
    }

    /**
     * Updates the request body of an operation.
     *
     * @param context Conversion context.
     * @param operation Operation being converted.
     * @param httpMethodName The HTTP method that this request is bound to.
     * @param path The HTTP URI this request is bound to.
     * @param requestBody Request body being updated.
     * @return Returns the updated request body.
     */
    default RequestBodyObject updateRequestBody(
            Context<? extends Trait> context,
            OperationShape operation,
            String httpMethodName,
            String path,
            RequestBodyObject requestBody
    ) {
        return requestBody;
    }

    /**
     * Updates a response object.
     *
     * @param context Conversion context.
     * @param operation Operation shape being converted.
     * @param status HTTP status of this response.
     * @param httpMethodName The HTTP method that this response responds to.
     * @param path The HTTP URI this response responds to.
     * @param response Response object being updated.
     * @return Returns the updated response object.
     */
    default ResponseObject updateResponse(
            Context<? extends Trait> context,
            OperationShape operation,
            String status,
            String httpMethodName,
            String path,
            ResponseObject response
    ) {
        return response;
    }

    /**
     * Updates an OpenApi.Builder before converting the model.
     *
     * @param context Conversion context.
     * @param builder OpenAPI builder to modify.
     */
    default void before(Context<? extends Trait> context, OpenApi.Builder builder) {}

    /**
     * Updates a security scheme object.
     *
     * @param context Conversion context.
     * @param authTrait Smithy authentication scheme trait.
     * @param securityScheme Security scheme object to update.
     * @return Returns the updated security scheme object. Return null to remove the scheme.
     */
    default SecurityScheme updateSecurityScheme(
            Context<? extends Trait> context,
            Trait authTrait,
            SecurityScheme securityScheme
    ) {
        return securityScheme;
    }

    /**
     * Updates a security requirement map.
     *
     * <p>The provided requirement {@code Map} will never be null or empty,
     * but it may contain more than one key-value pair. A null or empty
     * return value will cause the security requirement to be omitted
     * from the converted shape.
     *
     * @param context Conversion context.
     * @param shape Shape that is getting a security requirement (a service or operation).
     * @param converter Security scheme converter.
     * @param requirement Security scheme requirement to update.
     * @return Returns the updated security requirement, a mapping of scheme to requirements.
     */
    default Map<String, List<String>> updateSecurity(
            Context<? extends Trait> context,
            Shape shape,
            SecuritySchemeConverter<? extends Trait> converter,
            Map<String, List<String>> requirement
    ) {
        return requirement;
    }

    /**
     * Updates an OpenApi object after it is built.
     *
     * @param context Conversion context.
     * @param openapi OpenAPI object to modify.
     * @return Returns the updated OpenApi object.
     */
    default OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
        return openapi;
    }

    /**
     * Modifies the Node/JSON representation of an OpenAPI object.
     *
     * @param context Conversion context.
     * @param openapi OpenAPI object being converted to a node.
     * @param node OpenAPI object node.
     * @return Returns the updated ObjectNode.
     */
    default ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
        return node;
    }

    /**
     * Creates an OpenApiMapper that is composed of multiple mappers.
     *
     * @param mappers Mappers to compose.
     * @return Returns the composed mapper.
     */
    static OpenApiMapper compose(List<OpenApiMapper> mappers) {
        List<OpenApiMapper> sorted = new ArrayList<>(mappers);
        sorted.sort(Comparator.comparingInt(OpenApiMapper::getOrder));

        return new OpenApiMapper() {
            @Override
            public void updateDefaultSettings(Model model, OpenApiConfig config) {
                for (OpenApiMapper plugin : sorted) {
                    plugin.updateDefaultSettings(model, config);
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
                for (OpenApiMapper plugin : sorted) {
                    if (operation == null) {
                        return null;
                    }
                    operation = plugin.updateOperation(context, shape, operation, httpMethodName, path);
                }
                return operation;
            }

            @Override
            public OperationObject postProcessOperation(
                    Context<? extends Trait> context,
                    OperationShape shape,
                    OperationObject operation,
                    String httpMethodName,
                    String path
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (operation == null) {
                        return null;
                    }
                    operation = plugin.postProcessOperation(context, shape, operation, httpMethodName, path);
                }
                return operation;
            }

            @Override
            public PathItem updatePathItem(Context<? extends Trait> context, String path, PathItem pathItem) {
                for (OpenApiMapper plugin : sorted) {
                    if (pathItem == null) {
                        return null;
                    }
                    pathItem = plugin.updatePathItem(context, path, pathItem);
                }
                return pathItem;
            }

            @Override
            public ParameterObject updateParameter(
                    Context<? extends Trait> context,
                    OperationShape operation,
                    String httpMethodName,
                    String path,
                    ParameterObject parameterObject
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (parameterObject == null) {
                        return null;
                    }
                    parameterObject = plugin.updateParameter(
                            context,
                            operation,
                            httpMethodName,
                            path,
                            parameterObject);
                }
                return parameterObject;
            }

            @Override
            public RequestBodyObject updateRequestBody(
                    Context<? extends Trait> context,
                    OperationShape shape,
                    String httpMethodName,
                    String path,
                    RequestBodyObject requestBody
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (requestBody == null) {
                        return null;
                    }
                    requestBody = plugin.updateRequestBody(context, shape, httpMethodName, path, requestBody);
                }
                return requestBody;
            }

            @Override
            public ResponseObject updateResponse(
                    Context<? extends Trait> context,
                    OperationShape shape,
                    String status,
                    String httpMethodName,
                    String path,
                    ResponseObject response
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (response == null) {
                        return null;
                    }
                    response = plugin.updateResponse(context, shape, status, httpMethodName, path, response);
                }
                return response;
            }

            @Override
            public SecurityScheme updateSecurityScheme(
                    Context<? extends Trait> context,
                    Trait authTrait,
                    SecurityScheme securityScheme
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (securityScheme == null) {
                        return null;
                    }
                    securityScheme = plugin.updateSecurityScheme(context, authTrait, securityScheme);
                }
                return securityScheme;
            }

            @Override
            public Map<String, List<String>> updateSecurity(
                    Context<? extends Trait> context,
                    Shape shape,
                    SecuritySchemeConverter<? extends Trait> converter,
                    Map<String, List<String>> requirement
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (requirement == null || requirement.isEmpty()) {
                        return null;
                    }
                    requirement = plugin.updateSecurity(context, shape, converter, requirement);
                }

                return requirement;
            }

            @Override
            public void before(Context<? extends Trait> context, OpenApi.Builder builder) {
                for (OpenApiMapper plugin : sorted) {
                    plugin.before(context, builder);
                }
            }

            @Override
            public OpenApi after(Context<? extends Trait> context, OpenApi openapi) {
                for (OpenApiMapper plugin : sorted) {
                    openapi = plugin.after(context, openapi);
                }
                return openapi;
            }

            @Override
            public ObjectNode updateNode(Context<? extends Trait> context, OpenApi openapi, ObjectNode node) {
                for (OpenApiMapper plugin : sorted) {
                    node = plugin.updateNode(context, openapi, node);
                }
                return node;
            }
        };
    }
}
