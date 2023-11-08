/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.iam.traits;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains information about a resource an IAM action can be authorized against.
 */
public final class ActionResource implements ToNode, ToSmithyBuilder<ActionResource> {
    private static final String CONDITION_KEYS = "conditionKeys";

    private final List<String> conditionKeys;

    private ActionResource(Builder builder) {
        this.conditionKeys = builder.conditionKeys.copy();
    }

    /**
     * Gets the condition keys used for authorizing against this resource.
     *
     * @return the condition keys.
     */
    public List<String> getConditionKeys() {
        return conditionKeys;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ActionResource fromNode(Node value) {
        Builder builder = builder();
        value.expectObjectNode()
                .warnIfAdditionalProperties(Collections.singletonList(CONDITION_KEYS))
                .getArrayMember(CONDITION_KEYS, StringNode::getValue, builder::conditionKeys);
        return builder.build();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        if (!conditionKeys.isEmpty()) {
            builder.withMember(CONDITION_KEYS, ArrayNode.fromStrings(conditionKeys));
        }
        return builder.build();
    }

    @Override
    public SmithyBuilder<ActionResource> toBuilder() {
        return builder().conditionKeys(conditionKeys);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActionResource that = (ActionResource) o;
        return Objects.equals(conditionKeys, that.conditionKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionKeys);
    }

    public static final class Builder implements SmithyBuilder<ActionResource> {
        private final BuilderRef<List<String>> conditionKeys = BuilderRef.forList();

        @Override
        public ActionResource build() {
            return new ActionResource(this);
        }

        public Builder conditionKeys(List<String> conditionKeys) {
            clearConditionKeys();
            this.conditionKeys.get().addAll(conditionKeys);
            return this;
        }

        public Builder clearConditionKeys() {
            conditionKeys.get().clear();
            return this;
        }

        public Builder addConditionKey(String conditionKey) {
            conditionKeys.get().add(conditionKey);
            return this;
        }

        public Builder removeConditionKey(String conditionKey) {
            conditionKeys.get().remove(conditionKey);
            return this;
        }
    }
}
