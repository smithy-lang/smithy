/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiPredicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.Node.NonNumericFloat;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;

/**
 * Validates the specific set of non-numeric values allowed for floats and doubles.
 */
final class NonNumericFloatValuesPlugin implements NodeValidatorPlugin {
    private static final Set<String> NON_NUMERIC_FLOAT_VALUES = NonNumericFloat.stringRepresentations();
    private static final ShapeTypeFilter SHAPE_TYPE_FILTER =
            new ShapeTypeFilter(EnumSet.of(ShapeType.FLOAT, ShapeType.DOUBLE), EnumSet.noneOf(ShapeType.class));

    @Override
    public BiPredicate<Model, Shape> shapeMatcher() {
        return SHAPE_TYPE_FILTER;
    }

    @Override
    public void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
        if (!value.isStringNode()) {
            return;
        }
        String nodeValue = value.expectStringNode().getValue();
        if (!NON_NUMERIC_FLOAT_VALUES.contains(nodeValue)) {
            emitter.accept(value,
                    String.format(
                            "Value for `%s` must either be numeric or one of the following strings: [\"%s\"], but was \"%s\"",
                            shape.getId(),
                            String.join("\", \"", NON_NUMERIC_FLOAT_VALUES),
                            nodeValue));
        }
    }
}
