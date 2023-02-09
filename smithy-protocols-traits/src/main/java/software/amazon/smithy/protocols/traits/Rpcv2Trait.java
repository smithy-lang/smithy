/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.protocols.traits;
 
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.utils.ListUtils;

public final class Rpcv2Trait extends AbstractTrait {

    private static final String HTTP = "http";
    private static final String EVENT_STREAM_HTTP = "eventStreamHttp";
    private static final String FORMAT = "format";

    private final List<String> http;
    private final List<String> eventStreamHttp;
    private final List<String> format;

    public static final ShapeId ID = ShapeId.from("smithy.protocols#Rpcv2");

    // Package-private constructor (at least for now)
    Rpcv2Trait(Builder builder) {
        super(ID, builder.getSourceLocation());
        http = ListUtils.copyOf(builder.http);
        eventStreamHttp = ListUtils.copyOf(builder.eventStreamHttp);
        format = ListUtils.copyOf(builder.format);
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

    /**
     * Gets the priority ordered list of supported wire formats.
     *
     * @return Returns the supported wire formats.
     */
    public List<String> getFormat() {
        return format;
    }

    /**
     * Turns the trait into a {@code Node}
     */
    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();

        if (!getHttp().isEmpty()) {
            builder.withMember(HTTP, Node.fromStrings(getHttp()));
        }

        if (!getEventStreamHttp().isEmpty()) {
            builder.withMember(EVENT_STREAM_HTTP, Node.fromStrings(getEventStreamHttp()));
        }

        if (!getFormat().isEmpty()) {
            builder.withMember(FORMAT, Node.fromStrings(getFormat()));
        }

        return builder.build();
    }

    /**
     * Builder for creating a {@code Rpcv2Trait}.
     */
    public static final class Builder extends AbstractTraitBuilder<Rpcv2Trait, Builder> {

        private final List<String> http = new ArrayList<>();
        private final List<String> eventStreamHttp = new ArrayList<>();
        private final List<String> format = new ArrayList<>();


        @Override
        public Rpcv2Trait build() {
            return new Rpcv2Trait(this);
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

        /**
         * Sets the list of supported wire formats.
         *
         * @param format Wire format to set and replace.
         * @return Returns the builder.
         */
        public Builder format(List<String> format) {
            this.format.clear();
            this.format.addAll(format);
            return this;
        }

        /**
         * Updates the builder from a Node.
         *
         * @param node Node object that must be a valid {@code ObjectNode}.
         * @return Returns the updated builder.
         */
        public Builder fromNode(Node node) {
            ObjectNode objectNode = node.expectObjectNode();
            objectNode.getArrayMember(HTTP)
                    .map(values -> Node.loadArrayOfString(HTTP, values))
                    .ifPresent(this::http);
            objectNode.getArrayMember(EVENT_STREAM_HTTP)
                    .map(values -> Node.loadArrayOfString(EVENT_STREAM_HTTP, values))
                    .ifPresent(this::eventStreamHttp);
            objectNode.getArrayMember(FORMAT)
                    .map(values -> Node.loadArrayOfString(FORMAT, values))
                    .ifPresent(this::format);
                
            return this;
        }
    }

    /**
     * Creates a new {@code Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Implements the {@code AbstractTrait.Provider}.
     */
    public static final class Provider extends AbstractTrait.Provider {
 
        public Provider() {
            super(ID);
        }
 
        /**
         * Creates a trait from a {@code Node} value.
         *
         * @param shapeId The shape ID.
         * @param value The Node value.
         * @return Returns the Rpcv2Trait. 
         */
        @Override
        public Rpcv2Trait createTrait(ShapeId shapeId, Node value) {
            return builder().sourceLocation(value).fromNode(value).build();
        }
    }
}
