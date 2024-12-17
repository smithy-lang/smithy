/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class MavenRepository implements ToSmithyBuilder<MavenRepository> {

    private final String url;
    private final String httpCredentials;
    private final String id;
    private final String proxyHost;
    private final String proxyCredentials;

    public MavenRepository(Builder builder) {
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.httpCredentials = builder.httpCredentials;
        this.id = builder.id;
        this.proxyHost = builder.proxyHost;
        this.proxyCredentials = builder.proxyCredentials;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MavenRepository fromNode(Node node) {
        Builder builder = builder();
        node.expectObjectNode()
                .warnIfAdditionalProperties(
                        Arrays.asList("url", "httpCredentials", "id", "proxyHost", "proxyCredentials"))
                .expectStringMember("url", builder::url)
                .getStringMember("httpCredentials", builder::httpCredentials)
                .getStringMember("id", builder::id)
                .getStringMember("proxyHost", builder::proxyHost)
                .getStringMember("proxyCredentials", builder::proxyCredentials);
        return builder.build();
    }

    public String getUrl() {
        return url;
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getHttpCredentials() {
        return Optional.ofNullable(httpCredentials);
    }

    public Optional<String> getProxyCredentials() {
        return Optional.ofNullable(proxyCredentials);
    }

    public Optional<String> getProxyHost() {
        return Optional.ofNullable(proxyHost);
    }

    @Override
    public Builder toBuilder() {
        return builder().id(id).url(url).httpCredentials(httpCredentials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, httpCredentials);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof MavenRepository)) {
            return false;
        }
        MavenRepository mavenRepo = (MavenRepository) o;
        return Objects.equals(id, mavenRepo.id)
                && Objects.equals(url, mavenRepo.url)
                && Objects.equals(httpCredentials, mavenRepo.httpCredentials);
    }

    public static final class Builder implements SmithyBuilder<MavenRepository> {
        private String url;
        private String httpCredentials;
        private String id;
        private String proxyHost;
        private String proxyCredentials;

        private Builder() {}

        @Override
        public MavenRepository build() {
            return new MavenRepository(this);
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder proxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public Builder proxyCredentials(String proxyCredentials) {
            this.proxyCredentials = proxyCredentials;
            validateColonSeparatedValue(proxyCredentials,
                    "Invalid proxyCredentials: expected in the format of user:pass");
            return this;
        }

        public Builder httpCredentials(String httpCredentials) {
            this.httpCredentials = httpCredentials;
            validateColonSeparatedValue(httpCredentials,
                    "Invalid httpCredentials: expected in the format of user:pass");
            return this;
        }

        private void validateColonSeparatedValue(String value, String errorMessage) {
            if (value != null) {
                int position = value.indexOf(':');
                if (position < 1 || position == value.length() - 1) {
                    throw new IllegalArgumentException(errorMessage);
                }
            }
        }
    }
}
