/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.node;

import java.math.BigDecimal;
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

    @Override
    public final void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (shape.hasTrait(RangeTrait.class)) {
            if (value.isNumberNode()) {
                check(shape, context, shape.expectTrait(RangeTrait.class), value.expectNumberNode(), emitter);
            } else if (value.isStringNode()) {
                checkNonNumeric(shape, shape.expectTrait(RangeTrait.class), value.expectStringNode(), emitter,
                        context);
            }
        }
    }

    private void checkNonNumeric(Shape shape, RangeTrait trait, StringNode node, Emitter emitter, Context context) {
        NonNumericFloat.fromStringRepresentation(node.getValue()).ifPresent(value -> {
            if (value.equals(NonNumericFloat.NAN)) {
                emitter.accept(node, getSeverity(context), String.format(
                        "Value provided for `%s` must be a number because the `smithy.api#range` trait is applied, "
                                + "but found \"%s\"",
                        shape.getId(), node.getValue()));
            }

            if (trait.getMin().isPresent() && value.equals(NonNumericFloat.NEGATIVE_INFINITY)) {
                emitter.accept(node, getSeverity(context), String.format(
                        "Value provided for `%s` must be greater than or equal to %s, but found \"%s\"",
                        shape.getId(), trait.getMin().get(), node.getValue()),
                        shape.isMemberShape() ? MEMBER : TARGET);
            }

            if (trait.getMax().isPresent() && value.equals(NonNumericFloat.POSITIVE_INFINITY)) {
                emitter.accept(node, getSeverity(context), String.format(
                        "Value provided for `%s` must be less than or equal to %s, but found \"%s\"",
                        shape.getId(), trait.getMax().get(), node.getValue()),
                        shape.isMemberShape() ? MEMBER : TARGET);
            }
        });
    }

    protected void check(Shape shape, Context context, RangeTrait trait, NumberNode node, Emitter emitter) {
        Number number = node.getValue();
        BigDecimal decimal = number instanceof BigDecimal
                ? (BigDecimal) number
                : new BigDecimal(number.toString());

        trait.getMin().ifPresent(min -> {
            if (decimal.compareTo(new BigDecimal(min.toString())) < 0) {
                emitter.accept(node, getSeverity(node, context), String.format(
                        "Value provided for `%s` must be greater than or equal to %s, but found %s",
                        shape.getId(), min, number),
                        shape.isMemberShape() ? MEMBER : TARGET);
            }
        });

        trait.getMax().ifPresent(max -> {
            if (decimal.compareTo(new BigDecimal(max.toString())) > 0) {
                emitter.accept(node, getSeverity(node, context), String.format(
                        "Value provided for `%s` must be less than or equal to %s, but found %s",
                        shape.getId(), max, number),
                        shape.isMemberShape() ? MEMBER : TARGET);
            }
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
                ? Severity.WARNING : Severity.ERROR;
    }
}
