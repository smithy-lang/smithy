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

package software.amazon.smithy.model.traits;

import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the HTTP request and response code bindings of an operation.
 */
public final class HttpTrait extends AbstractTrait implements ToSmithyBuilder<HttpTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#http");

    private final String method;
    private final UriPattern uri;
    private final int code;

    private HttpTrait(HttpTrait.Builder builder) {
        super(ID, builder.sourceLocation);
        method = Objects.requireNonNull(builder.method, "method not set");
        uri = Objects.requireNonNull(builder.uri, "uri not set");
        code = builder.code;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
                super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            HttpTrait.Builder builder = builder().sourceLocation(value);
            ObjectNode members = value.expectObjectNode();
            builder.uri(UriPattern.parse(members.expectStringMember("uri").getValue()));
            builder.method(members.expectStringMember("method").getValue());
            builder.code(members.getNumberMember("code")
                                 .map(NumberNode::getValue)
                                 .map(Number::intValue)
                                 .orElse(200));
            return builder.build();
        }
    }

    public UriPattern getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    public int getCode() {
        return code;
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withMember("method", Node.from(method))
                .withMember("uri", Node.from(uri.toString()))
                .withMember("code", Node.from(code));
    }

    /**
     * @return Returns a builder used to create an Http trait.
     */
    public static HttpTrait.Builder builder() {
        return new HttpTrait.Builder();
    }

    @Override
    public HttpTrait.Builder toBuilder() {
        return new Builder().method(method).uri(uri).code(code);
    }

    /**
     * Builder used to create an Http trait.
     */
    public static final class Builder extends AbstractTraitBuilder<HttpTrait, HttpTrait.Builder> {
        private String method;
        private UriPattern uri;
        private int code = 200;

        public HttpTrait.Builder uri(UriPattern uri) {
            this.uri = uri;
            return this;
        }

        public HttpTrait.Builder method(String method) {
            this.method = method;
            return this;
        }

        public HttpTrait.Builder code(int code) {
            this.code = code;
            return this;
        }

        @Override
        public HttpTrait build() {
            return new HttpTrait(this);
        }
    }
}
