/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.UnionShape;

/**
 * This class provides a simplified visitor interface for visiting trait shapes.
 *
 * <p>When handling trait shapes, all number shapes are treat the same, so we can handle
 * all numberShape branches with a single common method to reduce duplication.
 * The following shapes are not supported when visiting traits:
 * <ul>
 *     <li>MemberShapes</li>
 *     <li>UnionShapes</li>
 *     <li>BlobShapes</li>
 * </ul>
 *
 * @param <R> Return type
 */
abstract class TraitVisitor<R> extends ShapeVisitor.DataShapeVisitor<R> {

    @Override
    public R booleanShape(BooleanShape shape) {
        throw new UnsupportedOperationException("Boolean traits not supported. Consider using an "
                + " Annotation Trait.");
    }

    @Override
    public R byteShape(ByteShape shape) {
        return numberShape(shape);
    }

    @Override
    public R shortShape(ShortShape shape) {
        return numberShape(shape);
    }

    @Override
    public R integerShape(IntegerShape shape) {
        return numberShape(shape);
    }

    @Override
    public R longShape(LongShape shape) {
        return numberShape(shape);
    }

    @Override
    public R floatShape(FloatShape shape) {
        return numberShape(shape);
    }

    @Override
    public R doubleShape(DoubleShape shape) {
        return numberShape(shape);
    }

    @Override
    public R bigIntegerShape(BigIntegerShape shape) {
        return numberShape(shape);
    }

    @Override
    public R bigDecimalShape(BigDecimalShape shape) {
        return numberShape(shape);
    }

    @Override
    public R unionShape(UnionShape shape) {
        throw new UnsupportedOperationException("Property generator does not support shape "
                + shape + " of type " + shape.getType());
    }

    @Override
    public R blobShape(BlobShape shape) {
        throw new UnsupportedOperationException("Property generator does not support shape "
                + shape + " of type " + shape.getType());
    }

    @Override
    public R memberShape(MemberShape shape) {
        throw new IllegalArgumentException("Property generator cannot visit member shapes. Attempted "
                + "to visit " + shape);
    }

    protected abstract R numberShape(NumberShape shape);
}
