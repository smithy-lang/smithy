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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.LengthTrait;

/**
 * Validates length trait on blob shapes and members that target blob shapes.
 */
public final class BlobLengthPlugin extends MemberAndShapeTraitPlugin<BlobShape, StringNode, LengthTrait> {
    public BlobLengthPlugin() {
        super(BlobShape.class, StringNode.class, LengthTrait.class);
    }

    @Override
    protected List<String> check(Shape shape, LengthTrait trait, StringNode node, ShapeIndex index) {
        String value = node.getValue();
        List<String> messages = new ArrayList<>();
        int size = value.getBytes(Charset.forName("UTF-8")).length;
        trait.getMin().ifPresent(min -> {
            if (size < min) {
                messages.add("Value provided for `" + shape.getId() + "` must have at least "
                             + min + " bytes, but the provided value only has " + size + " bytes");
            }
        });
        trait.getMax().ifPresent(max -> {
            if (value.getBytes(Charset.forName("UTF-8")).length > max) {
                messages.add("Value provided for `" + shape.getId() + "` must have no more than "
                             + max + " bytes, but the provided value has " + size + " bytes");
            }
        });
        return messages;
    }
}
