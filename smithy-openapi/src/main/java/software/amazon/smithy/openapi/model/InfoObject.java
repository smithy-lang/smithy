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

public final class InfoObject extends Component implements ToSmithyBuilder<InfoObject> {
    private final String title;
    private final String version;
    private final String description;
    private final String termsOfService;
    private final ObjectNode license;
    private final ObjectNode contact;

    private InfoObject(Builder builder) {
        super(builder);
        title = SmithyBuilder.requiredState("title", builder.title);
        version = SmithyBuilder.requiredState("version", builder.version);
        description = builder.description;
        termsOfService = builder.termsOfService;
        license = builder.license;
        contact = builder.contact;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getTermsOfService() {
        return Optional.ofNullable(termsOfService);
    }

    public Optional<ObjectNode> getLicense() {
        return Optional.ofNullable(license);
    }

    public Optional<ObjectNode> getContact() {
        return Optional.ofNullable(contact);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .title(title)
                .version(version)
                .description(description)
                .termsOfService(termsOfService)
                .license(license)
                .contact(contact);
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        return Node.objectNodeBuilder()
                .withMember("title", getTitle())
                .withMember("version", getVersion())
                .withOptionalMember("termsOfService", getTermsOfService().map(Node::from))
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("license", getLicense())
                .withOptionalMember("contact", getContact());
    }

    public static final class Builder extends Component.Builder<Builder, InfoObject> {
        private String title;
        private String version;
        private String description;
        private String termsOfService;
        private ObjectNode license;
        private ObjectNode contact;

        private Builder() {}

        @Override
        public InfoObject build() {
            return new InfoObject(this);
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder termsOfService(String termsOfService) {
            this.termsOfService = termsOfService;
            return this;
        }

        public Builder license(ObjectNode license) {
            this.license = license;
            return this;
        }

        public Builder contact(ObjectNode contact) {
            this.contact = contact;
            return this;
        }
    }
}
