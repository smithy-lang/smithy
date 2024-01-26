/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.codegen.core.SymbolProvider;
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
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.GeneratorProvider;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Provides the {@link TraitGenerator} to use given a Smithy shape with a {@code @trait} trait definition trait.
 */
@SmithyInternalApi
public final class DefaultTraitGeneratorProvider extends ShapeVisitor.Default<TraitGenerator>
        implements GeneratorProvider {
    private final SymbolProvider symbolProvider;

    public DefaultTraitGeneratorProvider(SymbolProvider symbolProvider) {
        this.symbolProvider = symbolProvider;
    }

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
        // If the shape type resolves is a simple java string we can have the
        // resulting trait inherit from the StringTrait base class, simplifying the generated code.
        if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
          return new StringTraitGenerator();
        }
        return new ValueTraitGenerator();
    }

    @Override
    public TraitGenerator enumShape(EnumShape shape) {
        return new EnumTraitGenerator();
    }

    @Override
    public TraitGenerator listShape(ListShape shape) {
        // If the shape is a list shape with only string members we want it to inherit from
        // the StringListShape base class rather than use the default collection trait generator
        if (!shape.hasTrait(UniqueItemsTrait.class)
            && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getMember()))
        ) {
            return new StringListTraitGenerator();
        }
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
