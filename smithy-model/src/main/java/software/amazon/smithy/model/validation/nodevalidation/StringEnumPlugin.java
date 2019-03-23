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
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates the enum trait on string shapes.
 */
public final class StringEnumPlugin extends FilteredPlugin<StringShape, StringNode> {
    public StringEnumPlugin() {
        super(StringShape.class, StringNode.class);
    }

    @Override
    protected List<String> check(StringShape shape, StringNode node, ShapeIndex index) {
        List<String> messages = new ArrayList<>();
        // Validate the enum trait.
        shape.getTrait(EnumTrait.class).ifPresent(trait -> {
            if (!trait.getValues().containsKey(node.getValue())) {
                messages.add(String.format(
                        "String value provided for `%s` must be one of the following values: %s",
                        shape.getId(), ValidationUtils.tickedList(trait.getValues().keySet())));
            }
        });
        return messages;
    }
}
