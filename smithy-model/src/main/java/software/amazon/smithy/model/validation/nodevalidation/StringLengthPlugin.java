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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.LengthTrait;

/**
 * Validates the length trait on string shapes or members that target them.
 */
public final class StringLengthPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, LengthTrait> {
    public StringLengthPlugin() {
        super(StringShape.class, StringNode.class, LengthTrait.class);
    }

    @Override
    protected List<String> check(Shape shape, LengthTrait trait, StringNode node, ShapeIndex index) {
        List<String> messages = new ArrayList<>();
        trait.getMin().ifPresent(min -> {
            if (node.getValue().length() < min) {
                messages.add(String.format(
                        "String value provided for `%s` must be >= %d characters, but the provided value is "
                        + "only %d characters.", shape.getId(), min, node.getValue().length()));
            }
        });
        trait.getMax().ifPresent(max -> {
            if (node.getValue().length() > max) {
                messages.add(String.format(
                        "String value provided for `%s` must be <= %d characters, but the provided value is "
                        + "%d characters.", shape.getId(), max, node.getValue().length()));
            }
        });
        return messages;
    }
}
