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

import java.util.Collection;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.validation.ValidationUtils;


/**
 * Validates NumberNodes against intEnum shapes' allowed enum values.
 */
final class IntEnumPlugin extends FilteredPlugin<IntEnumShape, NumberNode> {

    IntEnumPlugin() {
        super(IntEnumShape.class, NumberNode.class);
    }

    @Override
    protected void check(IntEnumShape shape, NumberNode node, Context context, Emitter emitter) {
        Collection<Integer> values = shape.getEnumValues().values();
        if (!values.contains(node.getValue().intValue())) {
            emitter.accept(node, String.format(
                    "Integer value provided for `%s` must be one of the following values: %s, but found %s",
                    shape.getId(), ValidationUtils.tickedList(values), node.getValue()));
        }
    }
}
