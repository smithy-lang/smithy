/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.validation.validators.PaginatedTraitValidator;
import software.amazon.smithy.utils.ListUtils;

/**
 * Index of operation shapes to paginated trait information.
 *
 * <p>This index makes it easy to slice up paginated operations and
 * get the resolved members. This index performs some basic validation
 * of the paginated trait like ensuring that the operation has input
 * and output, and that the members defined in the paginated trait can
 * be found in the input or output of the operation. Additional
 * validation is performed in the {@link PaginatedTraitValidator}
 * (which makes use of this index).
 */
public final class PaginatedIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, PaginationInfo>> paginationInfo = new HashMap<>();

    public PaginatedIndex(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex opIndex = OperationIndex.of(model);

        for (ServiceShape service : model.getServiceShapes()) {
            PaginatedTrait serviceTrait = service.getTrait(PaginatedTrait.class).orElse(null);
            Map<ShapeId, PaginationInfo> mappings = new HashMap<>();
            for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                if (operation.hasTrait(PaginatedTrait.class)) {
                    PaginatedTrait merged = operation.expectTrait(PaginatedTrait.class).merge(serviceTrait);
                    create(model, service, opIndex, operation, merged).ifPresent(info -> {
                        mappings.put(info.getOperation().getId(), info);
                    });
                }
            }
            paginationInfo.put(service.getId(), Collections.unmodifiableMap(mappings));
        }
    }

    public static PaginatedIndex of(Model model) {
        return model.getKnowledge(PaginatedIndex.class, PaginatedIndex::new);
    }

    private Optional<PaginationInfo> create(
            Model model,
            ServiceShape service,
            OperationIndex opIndex,
            OperationShape operation,
            PaginatedTrait trait
    ) {
        StructureShape input = opIndex.expectInputShape(operation.getId());
        StructureShape output = opIndex.expectOutputShape(operation.getId());

        MemberShape inputToken = trait.getInputToken().flatMap(input::getMember).orElse(null);
        List<MemberShape> outputTokenPath = trait.getOutputToken()
                .map(path -> PaginatedTrait.resolveFullPath(path, model, output))
                .orElse(ListUtils.of());

        if (inputToken == null || outputTokenPath.isEmpty()) {
            return Optional.empty();
        }

        MemberShape pageSizeMember = trait.getPageSize().flatMap(input::getMember).orElse(null);
        List<MemberShape> itemsMemberPath = trait.getItems()
                .map(path -> PaginatedTrait.resolveFullPath(path, model, output))
                .orElse(ListUtils.of());

        return Optional.of(new PaginationInfo(
                service,
                operation,
                input,
                output,
                trait,
                inputToken,
                outputTokenPath,
                pageSizeMember,
                itemsMemberPath));
    }

    public Optional<PaginationInfo> getPaginationInfo(ToShapeId service, ToShapeId operation) {
        return Optional.ofNullable(paginationInfo.get(service.toShapeId()))
                .flatMap(mappings -> Optional.ofNullable(mappings.get(operation.toShapeId())));
    }
}
