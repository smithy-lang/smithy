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
                emitter.accept(node, String.format(
                        "String value provided for `%s` must be one of the following values: %s",
                        shape.getId(), ValidationUtils.tickedList(values)));
            }
        });
    }
}
