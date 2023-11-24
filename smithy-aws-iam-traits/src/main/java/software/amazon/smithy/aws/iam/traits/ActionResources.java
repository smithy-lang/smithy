/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.iam.traits;

import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains information about the resources an IAM action can be authorized against.
 */
public final class ActionResources implements ToNode, ToSmithyBuilder<ActionResources> {
    private static final String REQUIRED = "required";
    private static final String OPTIONAL = "optional";

    private final Map<String, ActionResource> required;
    private final Map<String, ActionResource> optional;

    private ActionResources(Builder builder) {
        required = builder.required.copy();
        optional = builder.optional.copy();
    }

    /**
     * Gets the resources that will always be authorized against for
     * functionality of the IAM action.
     *
     * @return the required resources.
     */
    public Map<String, ActionResource> getRequired() {
        return required;
    }

    /**
     * Gets the resources that will be authorized against based on
     * optional behavior of the IAM action.
     *
     * @return the optional resources.
     */
    public Map<String, ActionResource> getOptional() {
        return optional;
    }

    private static Builder builder() {
        return new Builder();
    }

    public static ActionResources fromNode(Node value) {
        Builder builder = builder();
        ObjectNode node = value.expectObjectNode()
                .warnIfAdditionalProperties(ListUtils.of(REQUIRED, OPTIONAL));
        if (node.containsMember(REQUIRED)) {
            for (Map.Entry<String, Node> entry : node.expectObjectMember(REQUIRED).getStringMap().entrySet()) {
                builder.putRequired(entry.getKey(), ActionResource.fromNode(entry.getValue()));
            }
        }
        if (node.containsMember(OPTIONAL)) {
            for (Map.Entry<String, Node> entry : node.expectObjectMember(OPTIONAL).getStringMap().entrySet()) {
                builder.putOptional(entry.getKey(), ActionResource.fromNode(entry.getValue()));
            }
        }
        return builder.build();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        if (!required.isEmpty()) {
            ObjectNode.Builder requiredBuilder = Node.objectNodeBuilder();
            for (Map.Entry<String, ActionResource> requiredEntry : required.entrySet()) {
                requiredBuilder.withMember(requiredEntry.getKey(), requiredEntry.getValue().toNode());
            }
        }
        if (!optional.isEmpty()) {
            ObjectNode.Builder optionalBuilder = Node.objectNodeBuilder();
            for (Map.Entry<String, ActionResource> optionalEntry : optional.entrySet()) {
                optionalBuilder.withMember(optionalEntry.getKey(), optionalEntry.getValue().toNode());
            }
        }
        return builder.build();
    }

    @Override
    public SmithyBuilder<ActionResources> toBuilder() {
        return builder().required(required).optional(optional);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActionResources that = (ActionResources) o;
        return Objects.equals(required, that.required)
                && Objects.equals(optional, that.optional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(required, optional);
    }

    public static final class Builder implements SmithyBuilder<ActionResources> {
        private final BuilderRef<Map<String, ActionResource>> required = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, ActionResource>> optional = BuilderRef.forOrderedMap();

        @Override
        public ActionResources build() {
            return new ActionResources(this);
        }

        public Builder clearRequired() {
            required.get().clear();
            return this;
        }

        public Builder required(Map<String, ActionResource> required) {
            clearRequired();
            this.required.get().putAll(required);
            return this;
        }

        public Builder putRequired(String resourceName, ActionResource actionResource) {
            required.get().put(resourceName, actionResource);
            return this;
        }

        public Builder removeRequired(String resourceName) {
            required.get().remove(resourceName);
            return this;
        }

        public Builder clearOptional() {
            optional.get().clear();
            return this;
        }

        public Builder optional(Map<String, ActionResource> optional) {
            clearOptional();
            this.optional.get().putAll(optional);
            return this;
        }

        public Builder putOptional(String resourceName, ActionResource actionResource) {
            optional.get().put(resourceName, actionResource);
            return this;
        }

        public Builder removeOptional(String resourceName) {
            optional.get().remove(resourceName);
            return this;
        }
    }
}
