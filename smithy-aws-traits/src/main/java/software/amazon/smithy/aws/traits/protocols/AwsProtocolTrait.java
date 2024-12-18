/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;

/**
 * Represents a configurable AWS protocol trait.
 *
 * <p>Subclasses are traits that allow the supported HTTP versions and
 * eventStream HTTP versions to be customized.
 */
public abstract class AwsProtocolTrait extends AbstractTrait {

    private static final String HTTP = "http";
    private static final String EVENT_STREAM_HTTP = "eventStreamHttp";

    private final List<String> http;
    private final List<String> eventStreamHttp;

    // package-private constructor (at least for now)
    AwsProtocolTrait(ShapeId id, Builder<?, ?> builder) {
        super(id, builder.getSourceLocation());
        http = builder.http.copy();
        eventStreamHttp = builder.eventStreamHttp.copy();
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
     * Gets the priority ordered list of supported HTTP protocol versions
     * that are required when using event streams.
     *
     * @return Returns the supported event stream HTTP protocol versions.
     */
    public List<String> getEventStreamHttp() {
        return eventStreamHttp;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.sourceLocation(getSourceLocation());

        if (!getHttp().isEmpty()) {
            builder.withMember(HTTP, Node.fromStrings(getHttp()));
        }

        if (!getEventStreamHttp().isEmpty()) {
            builder.withMember(EVENT_STREAM_HTTP, Node.fromStrings(getEventStreamHttp()));
        }

        return builder.build();
    }

    /**
     * Builder for creating a {@code AwsProtocolTrait}.
     *
     * @param <T> The type of trait being created.
     * @param <B> The concrete builder that creates {@code T}.
     */
    public abstract static class Builder<T extends Trait, B extends Builder> extends AbstractTraitBuilder<T, B> {

        private final BuilderRef<List<String>> http = BuilderRef.forList();
        private final BuilderRef<List<String>> eventStreamHttp = BuilderRef.forList();

        /**
         * Sets the list of supported HTTP protocols.
         *
         * @param http HTTP protocols to set and replace.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B http(List<String> http) {
            this.http.clear();
            this.http.get().addAll(http);
            return (B) this;
        }

        /**
         * Sets the list of supported event stream HTTP protocols.
         *
         * @param eventStreamHttp Event stream HTTP protocols to set and replace.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B eventStreamHttp(List<String> eventStreamHttp) {
            this.eventStreamHttp.clear();
            this.eventStreamHttp.get().addAll(eventStreamHttp);
            return (B) this;
        }

        /**
         * Updates the builder from a Node.
         *
         * @param node Node object that must be a valid {@code ObjectNode}.
         * @return Returns the updated builder.
         */
        @SuppressWarnings("unchecked")
        public B fromNode(Node node) {
            sourceLocation(node.getSourceLocation());
            ObjectNode objectNode = node.expectObjectNode();
            objectNode.getArrayMember(HTTP)
                    .map(values -> Node.loadArrayOfString(HTTP, values))
                    .ifPresent(this::http);
            objectNode.getArrayMember(EVENT_STREAM_HTTP)
                    .map(values -> Node.loadArrayOfString(EVENT_STREAM_HTTP, values))
                    .ifPresent(this::eventStreamHttp);
            return (B) this;
        }
    }
}
