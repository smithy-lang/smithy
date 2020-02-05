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

package software.amazon.smithy.aws.traits.protocols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Represents a configurable AWS protocol trait.
 *
 * <p>Subclasses are traits that allow the supported HTTP versions and
 * eventStream HTTP versions to be customized.
 */
public abstract class AwsProtocolTrait extends AbstractTrait {

    private static final String HTTP = "http";
    private static final String EVENT_STREAM_HTTP = "eventStreamHttp";

    private final List<Http> http;
    private final List<Http> eventStreamHttp;

    // package-private constructor (at least for now)
    AwsProtocolTrait(ShapeId id, Builder<?, ?> builder) {
        super(id, builder.getSourceLocation());
        http = ListUtils.copyOf(builder.http);
        eventStreamHttp = ListUtils.copyOf(builder.eventStreamHttp);
    }

    /**
     * Gets the priority ordered list of supported HTTP protocol versions.
     *
     * @return Returns the supported HTTP protocol versions.
     */
    public List<Http> getHttp() {
        return http;
    }

    /**
     * Gets the priority ordered list of supported HTTP protocol versions
     * that are required when using event streams.
     *
     * @return Returns the supported event stream HTTP protocol versions.
     */
    public List<Http> getEventStreamHttp() {
        return eventStreamHttp;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();

        if (!getHttp().isEmpty()) {
            builder.withMember(HTTP, getHttp().stream()
                    .map(Http::toString)
                    .map(Node::from)
                    .collect(ArrayNode.collect()));
        }

        if (!getEventStreamHttp().isEmpty()) {
            builder.withMember(EVENT_STREAM_HTTP, getEventStreamHttp().stream()
                    .map(Http::toString)
                    .map(Node::from)
                    .collect(ArrayNode.collect()));
        }

        return builder.build();
    }

    /**
     * HTTP protocol versions, specified based on ALPN protocol IDs.
     */
    public enum Http {
        HTTP_1_1("http/1.1"),
        H2("h2");

        private final String modelValue;

        Http(String modelValue) {
            this.modelValue = modelValue;
        }

        @Override
        public String toString() {
            return modelValue;
        }

        private static List<String> getAllModelValues() {
            List<String> values = new ArrayList<>();
            for (Http http : Http.values()) {
                values.add(http.toString());
            }
            return values;
        }

        private static Http fromString(String value) {
            for (Http http : Http.values()) {
                if (http.toString().equals(value)) {
                    return http;
                }
            }
            return null;
        }
    }

    /**
     * Builder for creating a {@code AwsProtocolTrait}.
     *
     * @param <T> The type of trait being created.
     * @param <B> The concrete builder that creates {@code T}.
     */
    public abstract static class Builder<T extends Trait, B extends Builder> extends AbstractTraitBuilder<T, B> {

        private final List<Http> http = new ArrayList<>();
        private final List<Http> eventStreamHttp = new ArrayList<>();

        /**
         * Sets the list of supported HTTP protocols.
         *
         * @param http HTTP protocols to set and replace.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B http(List<Http> http) {
            this.http.clear();
            this.http.addAll(http);
            return (B) this;
        }

        /**
         * Sets the list of supported event stream HTTP protocols.
         *
         * @param eventStreamHttp Event stream HTTP protocols to set and replace.
         * @return Returns the builder.
         */
        @SuppressWarnings("unchecked")
        public B eventStreamHttp(List<Http> eventStreamHttp) {
            this.eventStreamHttp.clear();
            this.eventStreamHttp.addAll(eventStreamHttp);
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
            ObjectNode objectNode = node.expectObjectNode();
            http(loadHttpList(HTTP, objectNode));
            eventStreamHttp(loadHttpList(EVENT_STREAM_HTTP, objectNode));
            return (B) this;
        }

        private List<Http> loadHttpList(String memberName, ObjectNode node) {
            return node.getArrayMember(memberName)
                    .map(array -> {
                        List<Http> result = new ArrayList<>(array.size());
                        for (StringNode value : array.getElementsAs(StringNode.class)) {
                            String oneOf = value.expectOneOf(Http.getAllModelValues());
                            result.add(Http.fromString(oneOf));
                        }
                        return result;
                    })
                    .orElse(Collections.emptyList());
        }
    }
}
