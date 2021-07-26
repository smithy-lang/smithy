/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a test case for malformed HTTP requests.
 */
@SmithyUnstableApi
public final class HttpMalformedRequestTestCase implements Tagged, ToSmithyBuilder<HttpMalformedRequestTestCase> {

    private static final String ID = "id";
    private static final String PROTOCOL = "protocol";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";

    private final String documentation;
    private final String id;
    private final ShapeId protocol;
    private final HttpMalformedRequestDefinition request;
    private final HttpMalformedResponseDefinition response;
    private final List<String> tags;

    private HttpMalformedRequestTestCase(Builder builder) {
        documentation = builder.documentation;
        id = SmithyBuilder.requiredState(ID, builder.id);
        protocol = SmithyBuilder.requiredState(PROTOCOL, builder.protocol);
        request = SmithyBuilder.requiredState(REQUEST, builder.request);
        response = SmithyBuilder.requiredState(RESPONSE, builder.response);
        tags = ListUtils.copyOf(builder.tags);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public String getId() {
        return id;
    }

    public ShapeId getProtocol() {
        return protocol;
    }

    public HttpMalformedRequestDefinition getRequest() {
        return request;
    }

    public HttpMalformedResponseDefinition getResponse() {
        return response;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .id(getId())
                .protocol(getProtocol())
                .request(getRequest())
                .response(getResponse())
                .tags(getTags());
        getDocumentation().ifPresent(builder::documentation);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a HttpMalformedRequestTestCase.
     */
    public static final class Builder implements SmithyBuilder<HttpMalformedRequestTestCase> {

        private String documentation;
        private String id;
        private ShapeId protocol;
        private HttpMalformedRequestDefinition request;
        private HttpMalformedResponseDefinition response;
        private final List<String> tags = new ArrayList<>();

        private Builder() {}

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder protocol(ToShapeId protocol) {
            this.protocol = protocol.toShapeId();
            return this;
        }

        public Builder request(HttpMalformedRequestDefinition request) {
            this.request = request;
            return this;
        }

        public Builder response(HttpMalformedResponseDefinition response) {
            this.response = response;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        @Override
        public HttpMalformedRequestTestCase build() {
            return new HttpMalformedRequestTestCase(this);
        }
    }
}
