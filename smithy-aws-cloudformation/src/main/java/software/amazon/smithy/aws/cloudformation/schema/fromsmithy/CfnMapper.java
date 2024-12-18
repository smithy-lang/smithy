/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Provides a plugin infrastructure to hook into the Smithy CloudFormation
 * Resource Schema generation process and map over the result.
 *
 * <p>The methods of a plugin are invoked by {@link CfnConverter} during
 * Resource Schema generation. There is no need to invoke these manually.
 * Implementations may choose to leverage configuration options of the
 * provided context to determine whether or not to enact the plugin.
 */
public interface CfnMapper {
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
     * Updates an ResourceSchema.Builder before converting the model.
     *
     * @param context Conversion context.
     * @param builder ResourceSchema builder to modify.
     */
    default void before(Context context, ResourceSchema.Builder builder) {}

    /**
     * Updates an ResourceSchema.Builder after converting the model.
     *
     * @param context Conversion context.
     * @param resourceSchema ResourceSchema to modify.
     * @return Returns the updated ResourceSchema object.
     */
    default ResourceSchema after(Context context, ResourceSchema resourceSchema) {
        return resourceSchema;
    }

    /**
     * Modifies the Node/JSON representation of a ResourceSchema object.
     *
     * @param context Conversion context.
     * @param resourceSchema ResourceSchema being converted to a node.
     * @param node ResourceSchema object node.
     * @return Returns the updated ObjectNode.
     */
    default ObjectNode updateNode(Context context, ResourceSchema resourceSchema, ObjectNode node) {
        return node;
    }
}
