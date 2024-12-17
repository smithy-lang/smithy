/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Abstract class used for most OpenAPI model components.
 *
 * <p>This class provides the ability to add arbitrary key-value
 * pairs to just about everything in the model. You'll need to
 * ensure that "x-" is added to each key to ensure compliance.
 * "x-" can be omitted if adding something that's built-in to
 * the OpenAPI spec but not directly supported in this package
 * (for example, "discriminator", "examples", etc).
 */
public abstract class Component implements ToNode {
    private Node node;
    private final Map<String, Node> extensions = new TreeMap<>();

    protected Component(Builder<?, ?> builder) {
        extensions.putAll(builder.getExtensions());
    }

    public final Optional<Node> getExtension(String name) {
        return Optional.ofNullable(extensions.get(name));
    }

    public final Map<String, Node> getExtensions() {
        return extensions;
    }

    @Override
    public final Node toNode() {
        if (node == null) {
            ObjectNode.Builder builder = createNodeBuilder();
            for (Map.Entry<String, Node> entry : extensions.entrySet()) {
                builder.withMember(entry.getKey(), entry.getValue().toNode());
            }
            node = builder.build();
        }

        return node;
    }

    protected abstract ObjectNode.Builder createNodeBuilder();

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || o.getClass() != getClass()) {
            return false;
        } else {
            return toNode().equals(((Component) o).toNode());
        }
    }

    @Override
    public final int hashCode() {
        return toNode().hashCode();
    }

    public abstract static class Builder<B extends Builder, C extends Component> implements SmithyBuilder<C> {
        private final Map<String, Node> extensions = new TreeMap<>();

        public Map<String, Node> getExtensions() {
            return extensions;
        }

        @SuppressWarnings("unchecked")
        public B extensions(Map<String, Node> extensions) {
            this.extensions.clear();
            this.extensions.putAll(extensions);
            return (B) this;
        }

        public B extensions(ObjectNode extensions) {
            return extensions(extensions.getStringMap());
        }

        @SuppressWarnings("unchecked")
        public B putExtension(String key, Node value) {
            extensions.put(key, value);
            return (B) this;
        }

        public B putExtension(String key, String value) {
            return putExtension(key, Node.from(value));
        }

        public B putExtension(String key, Boolean value) {
            return putExtension(key, Node.from(value));
        }

        public B putExtension(String key, Number value) {
            return putExtension(key, Node.from(value));
        }

        @SuppressWarnings("unchecked")
        public B removeExtension(String key) {
            extensions.remove(key);
            return (B) this;
        }
    }
}
