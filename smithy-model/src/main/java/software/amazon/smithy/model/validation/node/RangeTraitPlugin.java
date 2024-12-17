/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.Node.NonNumericFloat;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;

/**
 * Validates the range trait on number shapes or members that target them.
 */
class RangeTraitPlugin implements NodeValidatorPlugin {
    private static final String MEMBER = "Member";
    private static final String TARGET = "Target";
    private static final String INVALID_RANGE = "InvalidRange";

    @Override
    public final void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (shape.hasTrait(RangeTrait.class)) {
            if (value.isNumberNode()) {
                check(shape, context, shape.expectTrait(RangeTrait.class), value.expectNumberNode(), emitter);
            } else if (value.isStringNode()) {
                checkNonNumeric(shape,
                        shape.expectTrait(RangeTrait.class),
                        value.expectStringNode(),
                        emitter,
                        context);
            }
        }
    }

    private void checkNonNumeric(Shape shape, RangeTrait trait, StringNode node, Emitter emitter, Context context) {
        NonNumericFloat.fromStringRepresentation(node.getValue()).ifPresent(value -> {
            if (value.equals(NonNumericFloat.NAN)) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "Value provided for `%s` must be a number because the `smithy.api#range` trait is applied, "
                                        + "but found \"%s\"",
                                shape.getId(),
                                node.getValue()));
            }

            if (trait.getMin().isPresent() && value.equals(NonNumericFloat.NEGATIVE_INFINITY)) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "Value provided for `%s` must be greater than or equal to %s, but found \"%s\"",
                                shape.getId(),
                                trait.getMin().get(),
                                node.getValue()),
                        shape.isMemberShape() ? MEMBER : TARGET,
                        INVALID_RANGE);
            }

            if (trait.getMax().isPresent() && value.equals(NonNumericFloat.POSITIVE_INFINITY)) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "Value provided for `%s` must be less than or equal to %s, but found \"%s\"",
                                shape.getId(),
                                trait.getMax().get(),
                                node.getValue()),
                        shape.isMemberShape() ? MEMBER : TARGET,
                        INVALID_RANGE);
            }
        });
    }

    protected void check(Shape shape, Context context, RangeTrait trait, NumberNode node, Emitter emitter) {
        trait.getMin().ifPresent(min -> {
            node.asBigDecimal().ifPresent(decimal -> {
                if (decimal.compareTo(min) < 0) {
                    emitter.accept(node,
                            getSeverity(node, context),
                            String.format(
                                    "Value provided for `%s` must be greater than or equal to %s, but found %s",
                                    shape.getId(),
                                    min,
                                    decimal),
                            shape.isMemberShape() ? MEMBER : TARGET,
                            INVALID_RANGE);
                }
            });
        });

        trait.getMax().ifPresent(max -> {
            node.asBigDecimal().ifPresent(decimal -> {
                if (decimal.compareTo(max) > 0) {
                    emitter.accept(node,
                            getSeverity(node, context),
                            String.format(
                                    "Value provided for `%s` must be less than or equal to %s, but found %s",
                                    shape.getId(),
                                    max,
                                    decimal),
                            shape.isMemberShape() ? MEMBER : TARGET,
                            INVALID_RANGE);
                }
            });
        });
    }

    private Severity getSeverity(NumberNode node, Context context) {
        boolean zeroValueWarning = context
                .hasFeature(NodeValidationVisitor.Feature.RANGE_TRAIT_ZERO_VALUE_WARNING);
        boolean rangeTraitWarning = context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS);
        return (zeroValueWarning && node.isZero()) || rangeTraitWarning ? Severity.WARNING : Severity.ERROR;
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
