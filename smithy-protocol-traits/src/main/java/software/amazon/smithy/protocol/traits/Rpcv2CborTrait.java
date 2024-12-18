/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class Rpcv2CborTrait extends AbstractTrait implements ToSmithyBuilder<Rpcv2CborTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.protocols#rpcv2Cbor");

    private static final String HTTP = "http";
    private static final String EVENT_STREAM_HTTP = "eventStreamHttp";

    private final List<String> http;
    private final List<String> eventStreamHttp;

    private Rpcv2CborTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        http = ListUtils.copyOf(builder.http);
        eventStreamHttp = ListUtils.copyOf(builder.eventStreamHttp);
    }

    /**
     * Creates a new {@code Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Updates the builder from a Node.
     *
     * @param node Node object that must be a valid {@code ObjectNode}.
     * @return Returns the updated builder.
     */
    public static Rpcv2CborTrait fromNode(Node node) {
        Builder builder = builder().sourceLocation(node);
        ObjectNode objectNode = node.expectObjectNode();
        objectNode.getArrayMember(HTTP)
                .map(values -> Node.loadArrayOfString(HTTP, values))
                .ifPresent(builder::http);
        objectNode.getArrayMember(EVENT_STREAM_HTTP)
                .map(values -> Node.loadArrayOfString(EVENT_STREAM_HTTP, values))
                .ifPresent(builder::eventStreamHttp);
        return builder.build();
    }

    /**
     * Gets the priority ordered list of supported HTTP protocol versions.
     *
     * @return Returns the supported HTTP protocol versions.
     */
    public List<String> getHttp() {
        return http;
    }

    /**
     * Gets the priority ordered list of supported HTTP protocol versions that are required when
     * using event streams.
     *
     * @return Returns the supported event stream HTTP protocol versions.
     */
    public List<String> getEventStreamHttp() {
        return eventStreamHttp;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder().sourceLocation(getSourceLocation());
        if (!getHttp().isEmpty()) {
            builder.withMember(HTTP, Node.fromStrings(getHttp()));
        }
        if (!getEventStreamHttp().isEmpty()) {
            builder.withMember(EVENT_STREAM_HTTP, Node.fromStrings(getEventStreamHttp()));
        }
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder().http(http).eventStreamHttp(eventStreamHttp);
    }

    /**
     * Builder for creating a {@code Rpcv2CborTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<Rpcv2CborTrait, Builder> {

        private final List<String> http = new ArrayList<>();
        private final List<String> eventStreamHttp = new ArrayList<>();

        @Override
        public Rpcv2CborTrait build() {
            return new Rpcv2CborTrait(this);
        }

        /**
         * Sets the list of supported HTTP protocols.
         *
         * @param http HTTP protocols to set and replace.
         * @return Returns the builder.
         */
        public Builder http(List<String> http) {
            this.http.clear();
            this.http.addAll(http);
            return this;
        }

        /**
         * Sets the list of supported event stream HTTP protocols.
         *
         * @param eventStreamHttp Event stream HTTP protocols to set and replace.
         * @return Returns the builder.
         */
        public Builder eventStreamHttp(List<String> eventStreamHttp) {
            this.eventStreamHttp.clear();
            this.eventStreamHttp.addAll(eventStreamHttp);
            return this;
        }
    }

    /**
     * Implements the {@code AbstractTrait.Provider}.
     */
    public static final class Provider extends AbstractTrait.Provider {

        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Rpcv2CborTrait result = fromNode(value);
            result.setNodeCache(value);
            return result;
        }
    }
}
