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

import java.util.List;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates the pattern trait on string shapes or members that target them.
 */
public final class PatternTraitPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, PatternTrait> {
    public PatternTraitPlugin() {
        super(StringShape.class, StringNode.class, PatternTrait.class);
    }

    @Override
    protected List<String> check(Shape shape, PatternTrait trait, StringNode node, ShapeIndex index) {
        if (!trait.getPattern().matcher(node.getValue()).find()) {
            return ListUtils.of(String.format(
                    "String value provided for `%s` must match regular expression: %s",
                    shape.getId(), trait.getPattern().pattern()));
        }

        return ListUtils.of();
    }
}
