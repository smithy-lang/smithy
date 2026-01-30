/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.BiPredicate;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.jmespath.node.NodeJmespathRuntime;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.node.MemberAndShapeTraitPlugin;

/**
 * Validates that shapes with the conditions trait only contain values
 * that satisfy each condition expression.
 */
public final class ConditionsTraitPlugin extends MemberAndShapeTraitPlugin<Node, ConditionsTrait> {

    private static final ShapeTypeFilter SHAPE_TYPE_FILTER = new ShapeTypeFilter(EnumSet.allOf(ShapeType.class));

    public ConditionsTraitPlugin() {
        super(Node.class, ConditionsTrait.class);
    }

    @Override
    public BiPredicate<Model, Shape> shapeMatcher() {
        return SHAPE_TYPE_FILTER;
    }

    @Override
    protected void check(Shape shape, ConditionsTrait trait, Node value, Context context, Emitter emitter) {
        for (Map.Entry<String, Condition> entry : trait.getConditions().entrySet()) {
            checkCondition(shape, entry.getKey(), entry.getValue(), value, context, emitter);
        }
    }

    private void checkCondition(
            Shape shape,
            String conditionName,
            Condition condition,
            Node value,
            Context context,
            Emitter emitter
    ) {
        Evaluator<Node> evaluator = new Evaluator<>(value, NodeJmespathRuntime.INSTANCE);
        Node result = evaluator.visit(condition.getExpression());
        if (!result.expectBooleanNode().getValue()) {
            emitter.accept(value,
                    getSeverity(context),
                    String.format(
                            "Value provided for `%s` must match the %s condition expression",
                            shape.getId(),
                            conditionName),
                    conditionName);
        }
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
