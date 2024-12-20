/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.Node.NonNumericFloat;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Validates the specific set of non-numeric values allowed for floats and doubles.
 */
final class NonNumericFloatValuesPlugin implements NodeValidatorPlugin {
    private static final Set<String> NON_NUMERIC_FLOAT_VALUES = NonNumericFloat.stringRepresentations();

    @Override
    public void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (!(shape.isFloatShape() || shape.isDoubleShape()) || !value.isStringNode()) {
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
