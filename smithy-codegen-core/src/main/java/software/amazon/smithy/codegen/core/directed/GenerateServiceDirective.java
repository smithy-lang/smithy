/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.knowledge.PaginationInfo;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TitleTrait;

/**
 * Directive used to generate a service.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateService
 */
public final class GenerateServiceDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<ServiceShape, C, S> {

    GenerateServiceDirective(C context, ServiceShape service) {
        super(context, service, service);
    }

    /**
     * Gets the {@link TitleTrait} value of the service if present, otherwise returns
     * the {@link Symbol#getName()} value of the service.
     *
     * @return title of service
     */
    public String serviceTitle() {
        return serviceTitle(symbol().getName());
    }

    /**
     * Attempts to get the title of service from the model, returning the fallback value
     * if the service does not have a {@link TitleTrait}.
     *
     * @param fallback string to return if service does not have a title
     * @return title of service
     */
    public String serviceTitle(String fallback) {
        return shape().getTrait(TitleTrait.class).map(TitleTrait::getValue).orElse(fallback);
    }

    /**
     * Get a map of operations that are paginated.
     *
     * <p>The returned map is a map of operation shape IDs to a {@link PaginationInfo}
     * object.
     *
     * @return Returns the paginated operations as a map.
     * @see PaginatedIndex
     */
    Map<ShapeId, PaginationInfo> paginatedOperations() {
        Map<ShapeId, PaginationInfo> result = new TreeMap<>();
        PaginatedIndex index = PaginatedIndex.of(model());
        for (OperationShape operation : operations()) {
            index.getPaginationInfo(service(), operation).ifPresent(i -> result.put(operation.getId(), i));
        }
        return result;
    }

    /**
     * Get a map of operations that use an event stream in their input.
     *
     * @return Returns a map of operation shape IDs to their event stream information.
     * @see EventStreamIndex
     */
    public Map<ShapeId, EventStreamInfo> inputEventStreamOperations() {
        Map<ShapeId, EventStreamInfo> result = new TreeMap<>();
        EventStreamIndex index = EventStreamIndex.of(model());
        for (OperationShape operation : operations()) {
            index.getInputInfo(operation).ifPresent(i -> result.put(operation.getId(), i));
        }
        return result;
    }

    /**
     * Get a map of operations that use an event stream in their output.
     *
     * @return Returns a map of operation shape IDs to their event stream information.
     * @see EventStreamIndex
     */
    public Map<ShapeId, EventStreamInfo> outputEventStreamOperations() {
        Map<ShapeId, EventStreamInfo> result = new TreeMap<>();
        EventStreamIndex index = EventStreamIndex.of(model());
        for (OperationShape operation : operations()) {
            index.getOutputInfo(operation).ifPresent(i -> result.put(operation.getId(), i));
        }
        return result;
    }
}
