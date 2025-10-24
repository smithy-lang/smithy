/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An event sent over the event stream.
 */
public final class Event implements ToSmithyBuilder<Event> {
    private final EventType type;
    private final ObjectNode params;
    private final Map<String, EventHeaderValue<?>> headers;
    private final List<String> forbidHeaders;
    private final List<String> requireHeaders;
    private final String body;
    private final String bodyMediaType;
    private final String bytes;
    private final ObjectNode vendorParams;
    private final ShapeId vendorParamsShape;

    private Event(Builder builder) {
        this.type = SmithyBuilder.requiredState("type", builder.type);
        this.params = builder.params;
        this.headers = builder.headers.copy();
        this.forbidHeaders = builder.forbidHeaders.copy();
        this.requireHeaders = builder.requireHeaders.copy();
        this.body = builder.body;
        this.bodyMediaType = builder.bodyMediaType;
        this.bytes = builder.bytes;
        this.vendorParams = builder.vendorParams;
        this.vendorParamsShape = builder.vendorParamsShape;
    }

    /**
     * @return Returns the type of event.
     */
    public EventType getType() {
        return type;
    }

    /**
     * Gets the optional parameters used to generate the event.
     *
     * <p>If set, these parameters MUST be compatible with a modeled event.
     * If not set, this event represents an unmodeled event.
     *
     * @return Returns the optional parameters used to generate the event.
     */
    public Optional<ObjectNode> getParams() {
        return Optional.ofNullable(params);
    }

    /**
     * Gets a map of expected headers.
     *
     * <p>Headers that are not listed in this map are ignored unless they are
     * explicitly forbidden through {@link #forbidHeaders}.
     *
     * @return Returns a map of expected headers.
     */
    public Map<String, EventHeaderValue<?>> getHeaders() {
        return headers;
    }

    /**
     * @return Returns a list of headers field names that MUST NOT appear in
     * the serialized event.
     */
    public List<String> getForbidHeaders() {
        return forbidHeaders;
    }

    /**
     * Gets a list of header field names that MUST appear in the serialized event.
     *
     * <p>No assertion is made on the value of the headers.
     *
     * <p>Headers listed in {@link #headers} do not need to appear in this list.
     * @return Returns a list of required header keys.
     */
    public List<String> getRequireHeaders() {
        return requireHeaders;
    }

    /**
     * Gets the optional expected event body.
     *
     * <p>If no request body is defined, then no assertions are made about the
     * body of the event.
     *
     * @return Returns the optional expected event body.
     */
    public Optional<String> getBody() {
        return Optional.ofNullable(body);
    }

    /**
     * Gets the optional media type of the {@link #body}.
     *
     * <p>This is used to help test runners parse and validate the expected
     * data against generated data.
     *
     * @return Returns the optional media type of the event body.
     */
    public Optional<String> getBodyMediaType() {
        return Optional.ofNullable(bodyMediaType);
    }

    /**
     * Gets an optional binary representation of the entire event.
     *
     * <p> This is used to test deserialization. If set, implementations SHOULD
     * use this value to represent the binary value of received events rather
     * than constructing that binary value from the other properties of the
     * event.
     *
     * <p>This value SHOULD NOT be used to make assertions about serialized
     * events as such assertions likely would not be reliable. They would
     * suffer from the same problems of making {@link #body} assertions without a
     * {@link #bodyMediaType} where nonspecified ordering and optional whitespace
     * can cause semantically equivalent values to have different bytes. This
     * is made worse by headers having no defined order, and is likely made
     * even worse by common event framing features such as checksums.
     *
     * @return Returns an optional binary representation of the entire event.
     */
    public Optional<String> getBytes() {
        return Optional.ofNullable(bytes);
    }

    /**
     * Gets vendor-specific params used to influence the request.
     *
     * <p>For example, some vendors might utilize environment variables,
     * configuration files on disk, or other means to influence the
     * serialization formats used by clients or servers.
     *
     * <p>If {@link #vendorParamsShape} is set, this MUST be compatible with
     * that shape's definition.
     *
     * @return Returns a map of vendor-specific params used to influence the request.
     */
    public Optional<ObjectNode> getVendorParams() {
        return Optional.ofNullable(vendorParams);
    }

    /**
     * @return Returns an optional shape used to validate {@link #vendorParams}.
     */
    public Optional<ShapeId> getVendorParamsShape() {
        return Optional.ofNullable(vendorParamsShape);
    }

    @Override
    public SmithyBuilder<Event> toBuilder() {
        return new Builder()
                .type(type)
                .params(params)
                .headers(headers)
                .forbidHeaders(forbidHeaders)
                .requireHeaders(requireHeaders)
                .body(body)
                .bodyMediaType(bodyMediaType)
                .bytes(bytes)
                .vendorParams(vendorParams)
                .vendorParamsShape(vendorParamsShape);
    }

    /**
     * @return Returns a newly-created builder for {@link Event}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create {@link Event}.
     */
    public static final class Builder implements SmithyBuilder<Event> {
        private EventType type;
        private ObjectNode params;
        private final BuilderRef<Map<String, EventHeaderValue<?>>> headers = BuilderRef.forOrderedMap();
        private final BuilderRef<List<String>> forbidHeaders = BuilderRef.forList();
        private final BuilderRef<List<String>> requireHeaders = BuilderRef.forList();
        private String body;
        private String bodyMediaType;
        private String bytes;
        private ObjectNode vendorParams;
        private ShapeId vendorParamsShape;

        @Override
        public Event build() {
            return new Event(this);
        }

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder params(ObjectNode params) {
            this.params = params;
            return this;
        }

        public Builder headers(Map<String, EventHeaderValue<?>> headers) {
            this.headers.clear();
            this.headers.get().putAll(headers);
            return this;
        }

        public Builder forbidHeaders(List<String> forbidHeaders) {
            this.forbidHeaders.clear();
            this.forbidHeaders.get().addAll(forbidHeaders);
            return this;
        }

        public Builder requireHeaders(List<String> requireHeaders) {
            this.requireHeaders.clear();
            this.requireHeaders.get().addAll(requireHeaders);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder bodyMediaType(String bodyMediaType) {
            this.bodyMediaType = bodyMediaType;
            return this;
        }

        public Builder bytes(String bytes) {
            this.bytes = bytes;
            return this;
        }

        public Builder vendorParams(ObjectNode vendorParams) {
            this.vendorParams = vendorParams;
            return this;
        }

        public Builder vendorParamsShape(ShapeId vendorParamsShape) {
            this.vendorParamsShape = vendorParamsShape;
            return this;
        }
    }
}
