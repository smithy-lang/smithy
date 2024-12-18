/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.List;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates the enum trait on string shapes.
 */
final class StringEnumPlugin extends FilteredPlugin<StringShape, StringNode> {

    StringEnumPlugin() {
        super(StringShape.class, StringNode.class);
    }

    @Override
    protected void check(StringShape shape, StringNode node, Context context, Emitter emitter) {
        shape.getTrait(EnumTrait.class).ifPresent(trait -> {
            List<String> values = trait.getEnumDefinitionValues();
            if (!values.contains(node.getValue())) {
                emitter.accept(node,
                        String.format(
                                "String value provided for `%s` must be one of the following values: %s",
                                shape.getId(),
                                ValidationUtils.tickedList(values)));
            }
        });
    }
}
