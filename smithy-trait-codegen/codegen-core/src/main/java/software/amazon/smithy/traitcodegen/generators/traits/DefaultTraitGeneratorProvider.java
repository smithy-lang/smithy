/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.traitcodegen.TraitGeneratorProvider;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Provides the {@link TraitGenerator} to use given a Smithy shape with a {@code @trait} trait definition trait.
 * <p>
 * This class can be decorated with the
 * {@link software.amazon.smithy.traitcodegen.TraitCodegenIntegration#decorateGeneratorProvider}
 * method to provide a different trait generator implementation. Trait generator decoration can be used to handle
 * special cases of trait generation such as including additional functionality or using a new base class for the trait.
 */
@SmithyInternalApi
public final class DefaultTraitGeneratorProvider extends ShapeVisitor.Default<TraitGenerator>
        implements TraitGeneratorProvider {
    @Override
    public TraitGenerator getGenerator(Shape shape) {
        return shape.accept(this);
    }

    @Override
    protected TraitGenerator getDefault(Shape shape) {
        throw new UnsupportedOperationException("Trait code generation does not support shapes of type: "
                + shape.getType());
    }

    @Override
    public TraitGenerator booleanShape(BooleanShape shape) {
        throw new UnsupportedOperationException("Boolean shapes not supported for trait code generation. "
                + "Consider using an Annotation trait instead");
    }

    @Override
    public TraitGenerator intEnumShape(IntEnumShape shape) {
        return new IntEnumTraitGenerator();
    }

    @Override
    public TraitGenerator stringShape(StringShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator enumShape(EnumShape shape) {
        return new EnumTraitGenerator();
    }

    @Override
    public TraitGenerator listShape(ListShape shape) {
        return new CollectionTraitGenerator();
    }

    @Override
    public TraitGenerator byteShape(ByteShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator shortShape(ShortShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator integerShape(IntegerShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator longShape(LongShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator floatShape(FloatShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator documentShape(DocumentShape shape) {
        return new DocumentTraitGenerator();
    }

    @Override
    public TraitGenerator doubleShape(DoubleShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator bigIntegerShape(BigIntegerShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator bigDecimalShape(BigDecimalShape shape) {
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator mapShape(MapShape shape) {
        return new CollectionTraitGenerator();
    }

    @Override
    public TraitGenerator structureShape(StructureShape shape) {
        // Annotation (empty structure) traits inherit from a different base class than other
        // structure traits, so they need a custom generator.
        return new CollectionTraitGenerator();
    }
}
