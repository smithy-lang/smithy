/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates length trait on blob shapes and members that target blob shapes.
 */
@SmithyInternalApi
final class BlobLengthPlugin extends MemberAndShapeTraitPlugin<StringNode, LengthTrait> {

    private static final ShapeTypeFilter SHAPE_TYPE_FILTER = new ShapeTypeFilter(EnumSet.of(ShapeType.BLOB));

    public BlobLengthPlugin() {
        super(StringNode.class, LengthTrait.class);
    }

    @Override
    public ShapeTypeFilter shapeTypeFilter() {
        return SHAPE_TYPE_FILTER;
    }

    @Override
    protected void check(Shape shape, LengthTrait trait, StringNode node, Context context, Emitter emitter) {
        byte[] value = node.getValue().getBytes(StandardCharsets.UTF_8);

        if (context.hasFeature(NodeValidationVisitor.Feature.REQUIRE_BASE_64_BLOB_VALUES)) {
            try {
                value = Base64.getDecoder().decode(value);
            } catch (IllegalArgumentException e) {
                // Error will reported by the blobShape method in NodeValidationVisitor
                return;
            }
        }

        int size = value.length;

        trait.getMin().ifPresent(min -> {
            if (size < min) {
                emitter.accept(node,
                        getSeverity(context),
                        "Value provided for `" + shape.getId()
                                + "` must have at least " + min + " bytes, but the provided value only has " + size
                                + " bytes");
            }
        });

        trait.getMax().ifPresent(max -> {
            if (size > max) {
                emitter.accept(node,
                        getSeverity(context),
                        "Value provided for `" + shape.getId()
                                + "` must have no more than " + max + " bytes, but the provided value has " + size
                                + " bytes");
            }
        });
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
