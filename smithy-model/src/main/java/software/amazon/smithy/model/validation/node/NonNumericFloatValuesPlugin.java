/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.Node.NonNumericFloat;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.Severity;

/**
 * Validates the specific set of non-numeric values allowed for floats and doubles.
 */
final class NonNumericFloatValuesPlugin implements NodeValidatorPlugin  {
    private static final Set<String> NON_NUMERIC_FLOAT_VALUES = NonNumericFloat.stringRepresentations();

    @Override
    public void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (!(shape.isFloatShape() || shape.isDoubleShape()) || !value.isStringNode()) {
            return;
        }
        String nodeValue = value.expectStringNode().getValue();
        if (!NON_NUMERIC_FLOAT_VALUES.contains(nodeValue)) {
            emitter.accept(value, Severity.ERROR, String.format(
                    "Value for `%s` must either be numeric or one of the following strings: [\"%s\"], but was \"%s\"",
                    shape.getId(), String.join("\", \"", NON_NUMERIC_FLOAT_VALUES), nodeValue
            ));
        }
    }
}
