/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.traits;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Resolves an indexes the context parameters in the model.
 */
@SmithyUnstableApi
public final class ContextIndex implements KnowledgeIndex {
    private final WeakReference<Model> model;

    public ContextIndex(Model model) {
        this.model = new WeakReference<>(model);
    }

    public static ContextIndex of(Model model) {
        return model.getKnowledge(ContextIndex.class, ContextIndex::new);
    }

    /**
     * Gets the mapping of context parameter names and corresponding {@link ClientContextParamDefinition} to be
     * generated on the service client's configuration.
     *
     * @param service The service shape.
     * @return The mapping of context parameter names to {@link ClientContextParamDefinition}.
     */
    public Optional<ClientContextParamsTrait> getClientContextParams(Shape service) {
        return service.getTrait(ClientContextParamsTrait.class);
    }

    /**
     * Gets the static context parameter names and their {@link StaticContextParamDefinition} to be set for the given
     * operation.
     *
     * @param operation The operation shape.
     * @return The mapping of context parameter names to the static {@link Node} value to be set.
     */
    public Optional<StaticContextParamsTrait> getStaticContextParams(Shape operation) {
        return operation.getTrait(StaticContextParamsTrait.class);
    }

    /**
     * Gets the mapping of {@link MemberShape} to {@link ContextParamTrait} for the operation.
     *
     * @param operation The operation shape.
     * @return The mapping of operation's {@link MemberShape} to {@link ContextParamTrait}.
     */
    public Map<MemberShape, ContextParamTrait> getContextParams(Shape operation) {
        if (!operation.isOperationShape()) {
            throw new IllegalArgumentException(operation.toShapeId() + " is not an operation shape");
        }

        OperationIndex operationIndex = OperationIndex.of(getModel());
        Map<MemberShape, ContextParamTrait> contextParams = new HashMap<>();
        for (MemberShape member : operationIndex.getInputMembers(operation).values()) {
            if (member.hasTrait(ContextParamTrait.ID)) {
                contextParams.put(member, member.expectTrait(ContextParamTrait.class));
            }
        }
        return MapUtils.copyOf(contextParams);
    }

    private Model getModel() {
        return Objects.requireNonNull(model.get(), "The dereferenced WeakReference<Model> is null");
    }
}
