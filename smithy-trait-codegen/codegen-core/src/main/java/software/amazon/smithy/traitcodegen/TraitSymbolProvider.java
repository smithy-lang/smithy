/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.utils.SymbolUtil;


final class TraitSymbolProvider extends ShapeVisitor.Default<Symbol> implements SymbolProvider {
    private final String packageName;
    private final String packagePath;

    TraitSymbolProvider(TraitCodegenSettings settings) {
        this.packageName = settings.packageName();
        this.packagePath = "./" + packageName.replace(".", "/");
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        if (shape.hasTrait(TraitDefinition.class)) {
            return shape.accept(this);
        }
        throw new UnsupportedOperationException("Expected shape to have trait definition trait but was not found.");
    }


    @Override
    protected Symbol getDefault(Shape shape) {
        throw new UnsupportedOperationException("Shape type " + shape.getType() + " is not supported by Trait Codegen");
    }

    @Override
    public Symbol listShape(ListShape shape) {
        return getSymbolBuilder(shape).build();
    }

    @Override
    public Symbol byteShape(ByteShape shape) {
        return getSymbolBuilder(shape)
                .putProperty(SymbolProperties.VALUE_GETTER, "byteValue()")
                .build();
    }

    @Override
    public Symbol shortShape(ShortShape shape) {
        return getSymbolBuilder(shape)
                .putProperty(SymbolProperties.VALUE_GETTER, "shortValue()")
                .build();
    }

    @Override
    public Symbol integerShape(IntegerShape shape) {
        return getSymbolBuilder(shape)
                .putProperty(SymbolProperties.VALUE_GETTER, "intValue()")
                .build();
    }

    @Override
    public Symbol floatShape(FloatShape shape) {
        return getSymbolBuilder(shape)
                .putProperty(SymbolProperties.VALUE_GETTER, "floatValue()")
                .build();
    }

    @Override
    public Symbol documentShape(DocumentShape shape) {
        return getSymbolBuilder(shape).build();
    }

    @Override
    public Symbol doubleShape(DoubleShape shape) {
        return getSymbolBuilder(shape)
                .putProperty(SymbolProperties.VALUE_GETTER, "doubleValue()")
                .build();
    }

    @Override
    public Symbol bigDecimalShape(BigDecimalShape shape) {
        return getSymbolBuilder(shape).build();
    }

    @Override
    public Symbol mapShape(MapShape shape) {
        return getSymbolBuilder(shape).build();
    }

    @Override
    public Symbol longShape(LongShape shape) {
        return getSymbolBuilder(shape)
                .putProperty(SymbolProperties.VALUE_GETTER, "longValue()")
                .build();
    }

    @Override
    public Symbol stringShape(StringShape shape) {
        return getSymbolBuilder(shape).build();
    }

    @Override
    public Symbol structureShape(StructureShape shape) {
        return getSymbolBuilder(shape).build();
    }

    @Override
    public Symbol unionShape(UnionShape shape) {
        throw new UnsupportedOperationException("Cannot generate Union shape traits");
    }

    @Override
    public Symbol memberShape(MemberShape shape) {
        throw new UnsupportedOperationException("Cannot generate trait for member shape.");
    }

    @Override
    public Symbol timestampShape(TimestampShape shape) {
        throw new UnsupportedOperationException("Cannot generate trait for timestamp at this time");
    }

    private Symbol.Builder getSymbolBuilder(Shape shape) {
        return Symbol.builder()
                .name(SymbolUtil.getDefaultTraitName(shape))
                .namespace(packageName, ".")
                .declarationFile(packagePath + "/" + SymbolUtil.getDefaultTraitName(shape) + ".java");
    }
}
