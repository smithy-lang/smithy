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

package software.amazon.smithy.model.validation.nodevalidation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.RangeTrait;

/**
 * Validates the range trait on number shapes or members that target them.
 */
public final class RangeTraitPlugin extends MemberAndShapeTraitPlugin<NumberShape, NumberNode, RangeTrait> {
    public RangeTraitPlugin() {
        super(NumberShape.class, NumberNode.class, RangeTrait.class);
    }

    @Override
    protected List<String> check(Shape shape, RangeTrait trait, NumberNode node, ShapeIndex index) {
        List<String> messages = new ArrayList<>();
        Number number = node.getValue();
        BigDecimal decimal = new BigDecimal(number.toString());
        trait.getMin().ifPresent(min -> {
            if (decimal.compareTo(new BigDecimal(min.toString())) < 0) {
                messages.add(String.format(
                        "Value provided for `%s` must be greater than or equal to %s, but found %s",
                        shape.getId(), min.toString(), number));
            }
        });
        trait.getMax().ifPresent(max -> {
            if (decimal.compareTo(new BigDecimal(max.toString())) > 0) {
                messages.add(String.format(
                        "Value provided for `%s` must be less than or equal to %s, but found %s",
                        shape.getId(), max.toString(), number));
            }
        });
        return messages;
    }
}
