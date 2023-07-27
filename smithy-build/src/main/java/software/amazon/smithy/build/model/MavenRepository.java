/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    public MavenRepository(Builder builder) {
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.httpCredentials = builder.httpCredentials;
        this.id = builder.id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MavenRepository fromNode(Node node) {
        Builder builder = builder();
        node.expectObjectNode()
                .warnIfAdditionalProperties(Arrays.asList("url", "httpCredentials"))
                .expectStringMember("url", builder::url)
                .getStringMember("httpCredentials", builder::httpCredentials)
                .getStringMember("id", builder::id);
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
        return
            Objects.equals(id, mavenRepo.id)
                && Objects.equals(url, mavenRepo.url)
                && Objects.equals(httpCredentials, mavenRepo.httpCredentials);
    }

    public static final class Builder implements SmithyBuilder<MavenRepository> {
        private String url;
        private String httpCredentials;
        private String id;

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

        public Builder httpCredentials(String httpCredentials) {
            this.httpCredentials = httpCredentials;
            if (httpCredentials != null) {
                int position = httpCredentials.indexOf(':');
                if (position < 1 || position == httpCredentials.length() - 1) {
                    throw new IllegalArgumentException("Invalid httpCredentials: expected in the format of user:pass");
                }
            }
            return this;
        }
    }
}
