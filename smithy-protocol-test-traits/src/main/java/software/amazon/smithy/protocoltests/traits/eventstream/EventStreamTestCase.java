/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.TestExpectation;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A single event stream test case.
 */
public final class EventStreamTestCase implements ToSmithyBuilder<EventStreamTestCase> {
    private final String id;
    private final ShapeId protocol;
    private final ObjectNode initialRequestParams;
    private final ObjectNode initialRequest;
    private final ShapeId initialRequestShape;
    private final ObjectNode initialResponseParams;
    private final ObjectNode initialResponse;
    private final ShapeId initialResponseShape;
    private final List<Event> events;
    private final TestExpectation expectation;
    private final ObjectNode vendorParams;
    private final ShapeId vendorParamsShape;
    private final String documentation;
    private final AppliesTo appliesTo;

    private EventStreamTestCase(Builder builder) {
        this.id = SmithyBuilder.requiredState("id", builder.id);
        this.protocol = SmithyBuilder.requiredState("protocol", builder.protocol);
        this.initialRequestParams = builder.initialRequestParams;
        this.initialRequest = builder.initialRequest;
        this.initialRequestShape = builder.initialRequestShape;
        this.initialResponseParams = builder.initialResponseParams;
        this.initialResponse = builder.initialResponse;
        this.initialResponseShape = builder.initialResponseShape;
        this.events = builder.events.copy();
        this.expectation = builder.expectation;
        this.vendorParams = builder.vendorParams;
        this.vendorParamsShape = builder.vendorParamsShape;
        this.documentation = builder.documentation;
        this.appliesTo = builder.appliesTo;
    }

    /**
     * Get the test case identifier.
     *
     * <p>This identifier can be used by protocol test implementations to filter out
     * unsupported test cases by ID, to generate test case names, etc. No two test cases
     * can share the same ID.
     *
     * @return Returns the test case identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Returns the protocol the test case applies to.
     */
    public ShapeId getProtocol() {
        return protocol;
    }

    /**
     * @return Returns the initial request's parameters as an ObjectNode.
     */
    public Optional<ObjectNode> getInitialRequestParams() {
        return Optional.ofNullable(initialRequestParams);
    }

    /**
     * @return Returns the initial request as an ObjectNode.
     */
    public Optional<ObjectNode> getInitialRequest() {
        return Optional.ofNullable(initialRequest);
    }

    /**
     * @return Returns a shape describing the structure of the initial request.
     */
    public Optional<ShapeId> getInitialRequestShape() {
        return Optional.ofNullable(initialRequestShape);
    }

    /**
     * @return Returns the initial response's parameters as an ObjectNode.
     */
    public Optional<ObjectNode> getInitialResponseParams() {
        return Optional.ofNullable(initialResponseParams);
    }

    /**
     * @return Returns the initial response as an ObjectNode.
     */
    public Optional<ObjectNode> getInitialResponse() {
        return Optional.ofNullable(initialResponse);
    }

    /**
     * @return Returns a shape describing the structure of the initial response.
     */
    public Optional<ShapeId> getInitialResponseShape() {
        return Optional.ofNullable(initialResponseShape);
    }

    /**
     * Gets the list of events under test.
     *
     * <p>Each event must be sent in the order presented. Implementations MAY send
     * events concurrently.
     *
     * @return Returns the list of events under test.
     */
    public List<Event> getEvents() {
        return events;
    }

    /**
     * @return Returns the expected result of the test case.
     */
    public TestExpectation getExpectation() {
        return expectation;
    }

    /**
     * Gets any additional vendor-specific parameters.
     *
     * <p>This could include credentials, endpoint configuration, or
     * anything else that may impact the protocol's serialization of events.
     *
     * @return Returns vendor-specific parameters as an ObjectNode.
     */
    public Optional<ObjectNode> getVendorParams() {
        return Optional.ofNullable(vendorParams);
    }

    /**
     * @return Returns a shape describing the structure of the vendor params.
     */
    public Optional<ShapeId> getVendorParamsShape() {
        return Optional.ofNullable(vendorParamsShape);
    }

    /**
     * @return Returns documentation to write out at the beginning of the test case.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * @return Returns what sort of implementation the test case applies to.
     */
    public Optional<AppliesTo> getAppliesTo() {
        return Optional.ofNullable(appliesTo);
    }

    /**
     * @return Returns the test case as a builder
     */
    @Override
    public Builder toBuilder() {
        return builder()
                .id(id)
                .protocol(protocol)
                .initialRequestParams(initialRequestParams)
                .initialRequest(initialRequest)
                .initialRequestShape(initialRequestShape)
                .initialResponseParams(initialResponseParams)
                .initialResponse(initialResponse)
                .initialResponseShape(initialResponseShape)
                .events(events)
                .expectation(expectation)
                .vendorParams(vendorParams)
                .vendorParamsShape(vendorParamsShape)
                .documentation(documentation)
                .appliesTo(appliesTo);
    }

    /**
     * Creates a builder for an EventStreamTestCase.
     *
     * @return Returns a newly-created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventStreamTestCase)) {
            return false;
        }
        EventStreamTestCase that = (EventStreamTestCase) o;
        return Objects.equals(id, that.id) && Objects.equals(protocol, that.protocol)
                && Objects.equals(initialRequestParams, that.initialRequestParams)
                && Objects.equals(initialRequest, that.initialRequest)
                && Objects.equals(initialRequestShape, that.initialRequestShape)
                && Objects.equals(initialResponseParams, that.initialResponseParams)
                && Objects.equals(initialResponse, that.initialResponse)
                && Objects.equals(initialResponseShape, that.initialResponseShape)
                && Objects.equals(events, that.events)
                && Objects.equals(expectation, that.expectation)
                && Objects.equals(vendorParams, that.vendorParams)
                && Objects.equals(vendorParamsShape, that.vendorParamsShape)
                && Objects.equals(documentation, that.documentation)
                && appliesTo == that.appliesTo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                protocol,
                initialRequestParams,
                initialRequest,
                initialRequestShape,
                initialResponseParams,
                initialResponse,
                initialResponseShape,
                events,
                expectation,
                vendorParams,
                vendorParamsShape,
                documentation,
                appliesTo);
    }

    /**
     * Builder used to create {@link EventStreamTestCase}.
     */
    public static final class Builder implements SmithyBuilder<EventStreamTestCase> {
        private String id;
        private ShapeId protocol;
        private ObjectNode initialRequestParams;
        private ObjectNode initialRequest;
        private ShapeId initialRequestShape;
        private ObjectNode initialResponseParams;
        private ObjectNode initialResponse;
        private ShapeId initialResponseShape;
        private final BuilderRef<List<Event>> events = BuilderRef.forList();
        private TestExpectation expectation = TestExpectation.success();
        private ObjectNode vendorParams;
        private ShapeId vendorParamsShape;
        private String documentation;
        private AppliesTo appliesTo;

        @Override
        public EventStreamTestCase build() {
            return new EventStreamTestCase(this);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder protocol(ShapeId protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder initialRequestParams(ObjectNode initialRequestParams) {
            this.initialRequestParams = initialRequestParams;
            return this;
        }

        public Builder initialRequest(ObjectNode initialRequest) {
            this.initialRequest = initialRequest;
            return this;
        }

        public Builder initialRequestShape(ShapeId initialRequestShape) {
            this.initialRequestShape = initialRequestShape;
            return this;
        }

        public Builder initialResponseParams(ObjectNode initialResponseParams) {
            this.initialResponseParams = initialResponseParams;
            return this;
        }

        public Builder initialResponse(ObjectNode initialResponse) {
            this.initialResponse = initialResponse;
            return this;
        }

        public Builder initialResponseShape(ShapeId initialResponseShape) {
            this.initialResponseShape = initialResponseShape;
            return this;
        }

        public Builder events(List<Event> events) {
            this.events.clear();
            this.events.get().addAll(events);
            return this;
        }

        public Builder event(Event event) {
            this.events.get().add(event);
            return this;
        }

        public Builder expectation(TestExpectation expectation) {
            this.expectation = expectation;
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

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder appliesTo(AppliesTo appliesTo) {
            this.appliesTo = appliesTo;
            return this;
        }
    }
}
