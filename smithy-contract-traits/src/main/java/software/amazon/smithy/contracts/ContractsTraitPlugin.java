/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.EnumSet;
import java.util.Map;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.model.jmespath.node.NodeJmespathRuntime;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.node.NodeValidatorPlugin;

/**
 * Evaluates each {@code @contracts} expression of an operation against an operation call.
 *
 * <p>A contract constrains calls of an operation, so it is checked through the operation-call
 * hook rather than as an instance of any shape. The call is supplied by whichever validator
 * builds it (such as the examples validator, from each {@code @examples} call).
 */
public final class ContractsTraitPlugin implements NodeValidatorPlugin {

    private static final ShapeTypeFilter SHAPE_TYPE_FILTER = new ShapeTypeFilter(EnumSet.of(ShapeType.OPERATION));

    @Override
    public ShapeTypeFilter shapeTypeFilter() {
        return SHAPE_TYPE_FILTER;
    }

    @Override
    public void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
        // Contracts constrain calls, not instances, so there is nothing to do on the
        // shape-dispatched path. All work happens in applyForOperationCall.
    }

    @Override
    public void applyForOperationCall(OperationShape operation, ObjectNode call, Context context, Emitter emitter) {
        if (!operation.hasTrait(ContractsTrait.class)) {
            return;
        }
        Severity severity = context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
        for (Map.Entry<String, Condition> entry : operation.expectTrait(ContractsTrait.class)
                .getContracts()
                .entrySet()) {
            Evaluator<Node> evaluator = new Evaluator<>(call, NodeJmespathRuntime.INSTANCE);
            Node result = evaluator.visit(entry.getValue().getExpression());
            if (!result.expectBooleanNode().getValue()) {
                emitter.accept(call,
                        severity,
                        String.format(
                                "Call of `%s` must match the %s contract expression",
                                operation.getId(),
                                entry.getKey()),
                        entry.getKey());
            }
        }
    }
}
