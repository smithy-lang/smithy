/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class CallbackObject extends Component implements ToSmithyBuilder<CallbackObject> {
    private final Map<String, PathItem> paths;

    private CallbackObject(Builder builder) {
        super(builder);
        paths = builder.paths.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, PathItem> getPaths() {
        return paths;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            builder.withMember(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder().extensions(getExtensions()).paths(paths);
    }

    public static final class Builder extends Component.Builder<Builder, CallbackObject> {
        private final BuilderRef<Map<String, PathItem>> paths = BuilderRef.forSortedMap();

        private Builder() {}

        @Override
        public CallbackObject build() {
            return new CallbackObject(this);
        }

        public Builder paths(Map<String, PathItem> paths) {
            this.paths.clear();
            paths.forEach(this::putPath);
            return this;
        }

        public Builder putPath(String expression, PathItem pathItem) {
            paths.get().put(expression, pathItem);
            return this;
        }
    }
}
