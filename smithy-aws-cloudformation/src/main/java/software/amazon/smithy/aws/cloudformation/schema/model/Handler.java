/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Data class representing a CloudFormation Resource Schema's handler definition.
 *
 * @see <a href="https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-handlers">Resource Handler Definition</a>
 * @see <a href="https://github.com/aws-cloudformation/cloudformation-cli/blob/master/src/rpdk/core/data/schema/provider.definition.schema.v1.jsonL12">Resource Type Handler Definition JSON Schema</a>
 */
public final class Handler implements ToNode, ToSmithyBuilder<Handler> {
    public static final String CREATE = "create";
    public static final String READ = "read";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String LIST = "list";
    private static final Map<String, Integer> HANDLER_NAME_ORDERS = MapUtils.of(
            CREATE,
            0,
            READ,
            1,
            UPDATE,
            2,
            DELETE,
            3,
            LIST,
            4);

    private final Set<String> permissions;

    private Handler(Builder builder) {
        this.permissions = SetUtils.orderedCopyOf(builder.permissions);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(Handler.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .permissions(permissions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public static Integer getHandlerNameOrder(String name) {
        return HANDLER_NAME_ORDERS.getOrDefault(name, Integer.MAX_VALUE);
    }

    public static final class Builder implements SmithyBuilder<Handler> {
        private final Set<String> permissions = new TreeSet<>();

        private Builder() {}

        @Override
        public Handler build() {
            return new Handler(this);
        }

        public Builder permissions(Collection<String> permissions) {
            this.permissions.clear();
            this.permissions.addAll(permissions);
            return this;
        }

        public Builder addPermission(String permission) {
            this.permissions.add(permission);
            return this;
        }

        public Builder clearPermissions() {
            this.permissions.clear();
            return this;
        }
    }
}
