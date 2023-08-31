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

import java.nio.charset.StandardCharsets;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates length trait on blob shapes and members that target blob shapes.
 */
@SmithyInternalApi
final class BlobLengthPlugin extends MemberAndShapeTraitPlugin<BlobShape, StringNode, LengthTrait> {

    BlobLengthPlugin() {
        super(BlobShape.class, StringNode.class, LengthTrait.class);
    }

    @Override
    protected void check(Shape shape, LengthTrait trait, StringNode node, Context context, Emitter emitter) {
        String value = node.getValue();
        int size = value.getBytes(StandardCharsets.UTF_8).length;

        trait.getMin().ifPresent(min -> {
            if (size < min) {
                emitter.accept(node, getSeverity(context), "Value provided for `" + shape.getId()
                            + "` must have at least " + min + " bytes, but the provided value only has " + size
                            + " bytes");
            }
        });

        trait.getMax().ifPresent(max -> {
            if (value.getBytes(StandardCharsets.UTF_8).length > max) {
                emitter.accept(node, getSeverity(context), "Value provided for `" + shape.getId()
                            + "` must have no more than " + max + " bytes, but the provided value has " + size
                            + " bytes");
            }
        });
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING : Severity.ERROR;
    }
}
