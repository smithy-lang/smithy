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

package software.amazon.smithy.openapi.model;

import java.util.Optional;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public final class XmlObject extends Component implements ToSmithyBuilder<XmlObject> {
    private final String name;
    private final String namespace;
    private final String prefix;
    private final boolean attribute;
    private final boolean wrapped;

    private XmlObject(Builder builder) {
        super(builder);
        name = builder.name;
        namespace = builder.namespace;
        prefix = builder.prefix;
        attribute = builder.attribute;
        wrapped = builder.wrapped;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getNamespace() {
        return Optional.ofNullable(namespace);
    }

    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix);
    }

    public boolean isAttribute() {
        return attribute;
    }

    public boolean isWrapped() {
        return wrapped;
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember("name", getName().map(Node::from))
                .withOptionalMember("namespace", getNamespace().map(Node::from))
                .withOptionalMember("prefix", getPrefix().map(Node::from));

        if (isWrapped()) {
            builder.withMember("wrapped", Node.from(true));
        }

        if (isAttribute()) {
            builder.withMember("attribute", Node.from(true));
        }

        return builder;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .name(name)
                .namespace(namespace)
                .prefix(prefix)
                .attribute(attribute)
                .wrapped(wrapped);
    }

    public static final class Builder extends Component.Builder<Builder, XmlObject> {
        private String name;
        private String namespace;
        private String prefix;
        private boolean attribute;
        private boolean wrapped;

        private Builder() {}

        @Override
        public XmlObject build() {
            return new XmlObject(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder attribute(boolean attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder wrapped(boolean wrapped) {
            this.wrapped = wrapped;
            return this;
        }
    }
}
