/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class CallbackObject extends Component implements ToSmithyBuilder<CallbackObject> {
    private final Map<String, PathItem> paths;

    private CallbackObject(Builder builder) {
        super(builder);
        paths = Collections.unmodifiableMap(new TreeMap<>(builder.paths));
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
        private Map<String, PathItem> paths = new TreeMap<>();

        private Builder() {}

        @Override
        public CallbackObject build() {
            return new CallbackObject(this);
        }

        public Builder paths(Map<String, PathItem> paths) {
            this.paths.clear();
            this.paths.putAll(paths);
            return this;
        }

        public Builder putPath(String expression, PathItem pathItem) {
            paths.put(expression, pathItem);
            return this;
        }
    }
}
