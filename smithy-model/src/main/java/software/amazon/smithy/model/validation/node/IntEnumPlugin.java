/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.Collection;
import java.util.EnumSet;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates NumberNodes against intEnum shapes' allowed enum values.
 */
public final class IntEnumPlugin extends FilteredPlugin<IntEnumShape, NumberNode> {

    public IntEnumPlugin() {
        super(EnumSet.of(ShapeType.INT_ENUM), IntEnumShape.class, NumberNode.class);
    }

    @Override
    protected void check(IntEnumShape shape, NumberNode node, Context context, Emitter emitter) {
        Collection<Integer> values = shape.getEnumValues().values();
        if (!values.contains(node.getValue().intValue())) {
            emitter.accept(node,
                    String.format(
                            "Integer value provided for `%s` must be one of the following values: %s, but found %s",
                            shape.getId(),
                            ValidationUtils.tickedList(values),
                            node.getValue()));
        }
    }
}
