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

package software.amazon.smithy.rulesengine.traits;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.util.StringUtils;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An endpoint test-case expectation.
 */
@SmithyUnstableApi
public final class ExpectedEndpoint implements FromSourceLocation, ToSmithyBuilder<ExpectedEndpoint> {
    private final SourceLocation sourceLocation;
    private final String url;
    private final Map<String, List<String>> headers;
    private final Map<String, Node> properties;

    public ExpectedEndpoint(Builder builder) {
        this.sourceLocation = builder.sourceLocation;
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.headers = builder.headers.copy();
        this.properties = builder.properties.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, Node> getProperties() {
        return properties;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public SmithyBuilder<ExpectedEndpoint> toBuilder() {
        return builder()
                .sourceLocation(sourceLocation)
                .url(url)
                .headers(headers)
                .properties(properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUrl(), getHeaders(), getProperties());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpectedEndpoint that = (ExpectedEndpoint) o;
        return getUrl().equals(that.getUrl()) && Objects.equals(getHeaders(), that.getHeaders())
               && Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("url: ").append(url).append("\n");
        if (!headers.isEmpty()) {
            headers.forEach(
                    (key, value) -> {
                        sb.append(StringUtils.indent(String.format("%s:%s", key, value), 2));
                    });
        }
        if (!properties.isEmpty()) {
            sb.append("properties:\n");
            properties.forEach((k, v) -> sb
                    .append(
                            StringUtils.indent(
                                    String.format("%s: %s", k, Node.prettyPrintJson(v)), 2)
                    ));
        }
        return sb.toString();
    }

    public static final class Builder implements SmithyBuilder<ExpectedEndpoint> {
        private final BuilderRef<Map<String, List<String>>> headers = BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, Node>> properties = BuilderRef.forOrderedMap();
        private SourceLocation sourceLocation = SourceLocation.none();
        private String url;

        private Builder() {
        }

        public Builder sourceLocation(FromSourceLocation fromSourceLocation) {
            this.sourceLocation = fromSourceLocation.getSourceLocation();
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers.clear();
            this.headers.get().putAll(headers);
            return this;
        }

        public Builder putHeader(String header, List<String> values) {
            this.headers.get().put(header, values);
            return this;
        }

        public Builder removeHeader(String header) {
            this.headers.get().remove(header);
            return this;
        }

        public Builder properties(Map<String, Node> properties) {
            this.properties.clear();
            this.properties.get().putAll(properties);
            return this;
        }

        public Builder putProperty(String property, Node value) {
            this.properties.get().put(property, value);
            return this;
        }

        public Builder removeProperty(String property) {
            this.properties.get().remove(property);
            return this;
        }

        @Override
        public ExpectedEndpoint build() {
            return new ExpectedEndpoint(this);
        }
    }
}
