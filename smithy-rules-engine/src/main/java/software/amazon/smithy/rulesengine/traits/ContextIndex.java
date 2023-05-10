/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.traits;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.Pair;
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
        OperationShape operationShape = operation.asOperationShape()
                .orElseThrow(() -> new IllegalArgumentException(operation.toShapeId()
                                                                + " is not an operation shape"));

        LinkedHashMap<MemberShape, ContextParamTrait> out = new LinkedHashMap<>();

        getModel().expectShape(operationShape.getInputShape())
                .members().stream()
                .map(memberShape -> Pair.of(memberShape, memberShape.getTrait(ContextParamTrait.class)))
                .filter(pair -> pair.right.isPresent())
                .forEach(pair -> out.put(pair.left, pair.right.get()));
        return out;
    }

    private Model getModel() {
        return Objects.requireNonNull(model.get(), "The dereferenced WeakReference<Model> is null");
    }
}
