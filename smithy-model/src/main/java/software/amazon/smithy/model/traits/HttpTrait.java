/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
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
            value.expectObjectNode()
                    .expectStringMember("uri", s -> builder.uri(UriPattern.parse(s)))
                    .expectStringMember("method", builder::method)
                    .getNumberMember("code", n -> builder.code(n.intValue()));
            HttpTrait result = builder.build();
            result.setNodeCache(value);
            return result;
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
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("method", Node.from(method))
                .withMember("uri", Node.from(uri.toString()))
                .withMember("code", Node.from(code))
                .build();
    }

    /**
     * @return Returns a builder used to create an Http trait.
     */
    public static HttpTrait.Builder builder() {
        return new HttpTrait.Builder();
    }

    @Override
    public HttpTrait.Builder toBuilder() {
        return new Builder().sourceLocation(getSourceLocation()).method(method).uri(uri).code(code);
    }

    // Avoid inconsequential equality differences based on defaulting code to 200.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HttpTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            HttpTrait trait = (HttpTrait) other;
            return method.equals(trait.method)
                    && uri.equals(trait.uri)
                    && code == trait.code;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), method, uri, code);
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
