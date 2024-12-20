/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.PaginatedTrait;

/**
 * Resolved and valid pagination information about an operation in a service.
 */
public final class PaginationInfo {

    private final ServiceShape service;
    private final OperationShape operation;
    private final StructureShape input;
    private final StructureShape output;
    private final PaginatedTrait paginatedTrait;
    private final MemberShape inputToken;
    private final List<MemberShape> outputToken;
    private final MemberShape pageSize;
    private final List<MemberShape> items;

    PaginationInfo(
            ServiceShape service,
            OperationShape operation,
            StructureShape input,
            StructureShape output,
            PaginatedTrait paginatedTrait,
            MemberShape inputToken,
            List<MemberShape> outputToken,
            MemberShape pageSize,
            List<MemberShape> items
    ) {
        this.service = service;
        this.operation = operation;
        this.input = input;
        this.output = output;
        this.paginatedTrait = paginatedTrait;
        this.inputToken = inputToken;
        this.outputToken = outputToken;
        this.pageSize = pageSize;
        this.items = items;
    }

    public ServiceShape getService() {
        return service;
    }

    public OperationShape getOperation() {
        return operation;
    }

    public StructureShape getInput() {
        return input;
    }

    public StructureShape getOutput() {
        return output;
    }

    /**
     * Gets the paginated trait of the operation merged with the service.
     *
     * @return Returns the resolved paginated trait.
     */
    public PaginatedTrait getPaginatedTrait() {
        return paginatedTrait.merge(service.getTrait(PaginatedTrait.class).orElse(null));
    }

    public MemberShape getInputTokenMember() {
        return inputToken;
    }

    /**
     * @return the last {@link MemberShape} of the output path.
     *
     * @deprecated See {@link PaginationInfo#getOutputTokenMemberPath} to retrieve the full path.
     */
    @Deprecated
    public MemberShape getOutputTokenMember() {
        return outputToken.get(outputToken.size() - 1);
    }

    /**
     * Get the resolved output path identifiers as a list of {@link MemberShape}.
     *
     * @return A list of {@link MemberShape}.
     */
    public List<MemberShape> getOutputTokenMemberPath() {
        return outputToken;
    }

    /**
     * @return the last {@link MemberShape} of the items path.
     *
     * @deprecated See {@link PaginationInfo#getItemsMemberPath} to retrieve the full path.
     */
    @Deprecated
    public Optional<MemberShape> getItemsMember() {
        int size = items.size();
        if (size == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(size - 1));
    }

    /**
     * Get the resolved items path identifiers as a list of {@link MemberShape}.
     *
     * @return A list of {@link MemberShape}.
     */
    public List<MemberShape> getItemsMemberPath() {
        return items;
    }

    public Optional<MemberShape> getPageSizeMember() {
        return Optional.ofNullable(pageSize);
    }
}
