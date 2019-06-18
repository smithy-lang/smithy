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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
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
     * Updates an operation.
     *
     * @param context Conversion context.
     * @param shape Operation being converted.
     * @param operation OperationObject being built.
     * @return Returns the updated operation object.
     */
    default OperationObject updateOperation(Context context, OperationShape shape, OperationObject operation) {
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
    default PathItem updatePathItem(Context context, String path, PathItem pathItem) {
        return pathItem;
    }

    /**
     * Updates a parameter.
     *
     * @param context Conversion context.
     * @param operation Smithy operation being converted.
     * @param parameterObject Parameter being updated.
     * @return Returns the updated parameter.
     */
    default ParameterObject updateParameter(
            Context context,
            OperationShape operation,
            ParameterObject parameterObject
    ) {
        return parameterObject;
    }

    /**
     * Updates the request body of an operation.
     *
     * @param context Conversion context.
     * @param shape Operation being converted.
     * @param requestBody Request body being updated.
     * @return Returns the updated request body.
     */
    default RequestBodyObject updateRequestBody(
            Context context,
            OperationShape shape,
            RequestBodyObject requestBody
    ) {
        return requestBody;
    }

    /**
     * Updates a response object.
     *
     * @param context Conversion context.
     * @param status HTTP status of this response.
     * @param shape Operation shape being converted.
     * @param response Response object being updated.
     * @return Returns the updated response object.
     */
    default ResponseObject updateResponse(
            Context context,
            String status,
            OperationShape shape,
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
    default void before(Context context, OpenApi.Builder builder) {}

    /**
     * Updates a security scheme object.
     *
     * @param context Conversion context.
     * @param authName Smithy authentication scheme name.
     * @param securityScheme Security scheme object to update.
     * @return Returns the updated security scheme object. Return null to remove the scheme.
     */
    default SecurityScheme updateSecurityScheme(Context context, String authName, SecurityScheme securityScheme) {
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
            Context context,
            Shape shape,
            SecuritySchemeConverter converter,
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
    default OpenApi after(Context context, OpenApi openapi) {
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
    default ObjectNode updateNode(Context context, OpenApi openapi, ObjectNode node) {
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
            public OperationObject updateOperation(Context context, OperationShape shape, OperationObject operation) {
                for (OpenApiMapper plugin : sorted) {
                    if (operation == null) {
                        return null;
                    }
                    operation = plugin.updateOperation(context, shape, operation);
                }
                return operation;
            }

            @Override
            public PathItem updatePathItem(Context context, String path, PathItem pathItem) {
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
                    Context context,
                    OperationShape operation,
                    ParameterObject parameterObject
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (parameterObject == null) {
                        return null;
                    }
                    parameterObject = plugin.updateParameter(context, operation, parameterObject);
                }
                return parameterObject;
            }

            @Override
            public RequestBodyObject updateRequestBody(
                    Context context,
                    OperationShape shape,
                    RequestBodyObject requestBody
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (requestBody == null) {
                        return null;
                    }
                    requestBody = plugin.updateRequestBody(context, shape, requestBody);
                }
                return requestBody;
            }

            @Override
            public ResponseObject updateResponse(
                    Context context,
                    String status,
                    OperationShape shape,
                    ResponseObject response
            ) {
                for (OpenApiMapper plugin : sorted) {
                    if (response == null) {
                        return null;
                    }
                    response = plugin.updateResponse(context, status, shape, response);
                }
                return response;
            }

            @Override
            public SecurityScheme updateSecurityScheme(Context context, String name, SecurityScheme securityScheme) {
                for (OpenApiMapper plugin : sorted) {
                    if (securityScheme == null) {
                        return null;
                    }
                    securityScheme = plugin.updateSecurityScheme(context, name, securityScheme);
                }
                return securityScheme;
            }

            @Override
            public Map<String, List<String>> updateSecurity(
                    Context context,
                    Shape shape,
                    SecuritySchemeConverter converter,
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
            public void before(Context context, OpenApi.Builder builder) {
                for (OpenApiMapper plugin : sorted) {
                    plugin.before(context, builder);
                }
            }

            @Override
            public OpenApi after(Context context, OpenApi openapi) {
                for (OpenApiMapper plugin : sorted) {
                    openapi = plugin.after(context, openapi);
                }
                return openapi;
            }

            @Override
            public ObjectNode updateNode(Context context, OpenApi openapi, ObjectNode node) {
                for (OpenApiMapper plugin : sorted) {
                    node = plugin.updateNode(context, openapi, node);
                }
                return node;
            }
        };
    }
}
