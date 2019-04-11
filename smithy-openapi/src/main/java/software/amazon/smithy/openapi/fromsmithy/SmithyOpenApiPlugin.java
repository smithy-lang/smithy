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
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.openapi.model.SecurityScheme;

/**
 * Provides a plugin infrastructure used to hook into the Smithy to OpenAPI
 * conversion process.
 *
 * <p>The methods of a plugin are invoked by {@link OpenApiConverter} during
 * the conversion of a model. There is no need to invoke these manually.
 *
 * <p>All implementations of SmithyOpenApiPlugin are discovered using SPI
 * and applied to the model. Plugin implementations may choose to leverage
 * configuration options of the provided context to determine whether or
 * not to enact the plugin.
 */
public interface SmithyOpenApiPlugin {
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
     * @param pathItem Path item being converted.
     * @return Returns the updated path item.
     */
    default PathItem updatePathItem(Context context, PathItem pathItem) {
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
     * @param shape Operation shape being converted.
     * @param response Response object being updated.
     * @return Returns the updated response object.
     */
    default ResponseObject updateResponse(Context context, OperationShape shape, ResponseObject response) {
        return response;
    }

    /**
     * Updates a security scheme object.
     *
     * @param context Conversion context.
     * @param authName Smithy authentication scheme name.
     * @param securitySchemeName Name of the OpenAPI security scheme entry.
     * @param securityScheme Security scheme object to update.
     * @return Returns the updated security scheme object. Return null to remove the scheme.
     */
    default SecurityScheme updateSecurityScheme(
            Context context,
            String authName,
            String securitySchemeName,
            SecurityScheme securityScheme
    ) {
        return securityScheme;
    }

    /**
     * Updates an OpenApi.Builder before converting the model.
     *
     * @param context Conversion context.
     * @param builder OpenAPI builder to modify.
     */
    default void before(Context context, OpenApi.Builder builder) {}

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
     * Creates a ProtocolPlugin that is composed of multiple plugins.
     *
     * @param plugins Protocol plugins to compose.
     * @return Returns the composed plugin.
     */
    static SmithyOpenApiPlugin compose(List<SmithyOpenApiPlugin> plugins) {
        List<SmithyOpenApiPlugin> sorted = new ArrayList<>(plugins);
        sorted.sort(Comparator.comparingInt(SmithyOpenApiPlugin::getOrder));

        return new SmithyOpenApiPlugin() {
            @Override
            public OperationObject updateOperation(Context context, OperationShape shape, OperationObject operation) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    if (operation == null) {
                        return null;
                    }
                    operation = plugin.updateOperation(context, shape, operation);
                }
                return operation;
            }

            @Override
            public PathItem updatePathItem(Context context, PathItem pathItem) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    if (pathItem == null) {
                        return null;
                    }
                    pathItem = plugin.updatePathItem(context, pathItem);
                }
                return pathItem;
            }

            @Override
            public ParameterObject updateParameter(
                    Context context,
                    OperationShape operation,
                    ParameterObject parameterObject
            ) {
                for (SmithyOpenApiPlugin plugin : sorted) {
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
                for (SmithyOpenApiPlugin plugin : sorted) {
                    if (requestBody == null) {
                        return null;
                    }
                    requestBody = plugin.updateRequestBody(context, shape, requestBody);
                }
                return requestBody;
            }

            @Override
            public ResponseObject updateResponse(Context context, OperationShape shape, ResponseObject response) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    if (response == null) {
                        return null;
                    }
                    response = plugin.updateResponse(context, shape, response);
                }
                return response;
            }

            @Override
            public SecurityScheme updateSecurityScheme(
                    Context context,
                    String authName,
                    String securitySchemeName,
                    SecurityScheme securityScheme
            ) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    if (securityScheme == null) {
                        return null;
                    }
                    securityScheme = plugin.updateSecurityScheme(
                            context, authName, securitySchemeName, securityScheme);
                }
                return securityScheme;
            }

            @Override
            public void before(Context context, OpenApi.Builder builder) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    plugin.before(context, builder);
                }
            }

            @Override
            public OpenApi after(Context context, OpenApi openapi) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    openapi = plugin.after(context, openapi);
                }
                return openapi;
            }

            @Override
            public ObjectNode updateNode(Context context, OpenApi openapi, ObjectNode node) {
                for (SmithyOpenApiPlugin plugin : sorted) {
                    node = plugin.updateNode(context, openapi, node);
                }
                return node;
            }
        };
    }
}
