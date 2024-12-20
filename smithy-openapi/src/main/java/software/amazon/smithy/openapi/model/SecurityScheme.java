/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class SecurityScheme extends Component implements ToSmithyBuilder<SecurityScheme> {
    private final String type;
    private final String description;
    private final String name;
    private final String in;
    private final String scheme;
    private final String bearerFormat;
    private final String openIdConnectUrl;
    private final ObjectNode flows;

    private SecurityScheme(Builder builder) {
        super(builder);
        type = SmithyBuilder.requiredState("type", builder.type);
        description = builder.description;
        name = builder.name;
        in = builder.in;
        scheme = builder.scheme;
        bearerFormat = builder.bearerFormat;
        openIdConnectUrl = builder.openIdConnectUrl;
        flows = builder.flows;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getType() {
        return type;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getIn() {
        return Optional.ofNullable(in);
    }

    public Optional<String> getScheme() {
        return Optional.ofNullable(scheme);
    }

    public Optional<String> getBearerFormat() {
        return Optional.ofNullable(bearerFormat);
    }

    public Optional<String> getOpenIdConnectUrl() {
        return Optional.ofNullable(openIdConnectUrl);
    }

    public Optional<ObjectNode> getFlows() {
        return Optional.ofNullable(flows);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .type(type)
                .description(description)
                .name(name)
                .in(in)
                .scheme(scheme)
                .bearerFormat(bearerFormat)
                .openIdConnectUrl(openIdConnectUrl)
                .flows(flows);
    }

    protected ObjectNode.Builder createNodeBuilder() {
        return Node.objectNodeBuilder()
                .withMember("type", type)
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("name", getName().map(Node::from))
                .withOptionalMember("in", getIn().map(Node::from))
                .withOptionalMember("scheme", getScheme().map(Node::from))
                .withOptionalMember("bearerFormat", getBearerFormat().map(Node::from))
                .withOptionalMember("openIdConnectUrl", getOpenIdConnectUrl().map(Node::from))
                .withOptionalMember("flows", getFlows());
    }

    public static final class Builder extends Component.Builder<Builder, SecurityScheme> {
        private String type;
        private String description;
        private String name;
        private String in;
        private String scheme;
        private String bearerFormat;
        private String openIdConnectUrl;
        private ObjectNode flows;

        @Override
        public SecurityScheme build() {
            return new SecurityScheme(this);
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder in(String in) {
            this.in = in;
            return this;
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder bearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }

        public Builder openIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        public Builder flows(ObjectNode flows) {
            this.flows = flows;
            return this;
        }
    }
}
