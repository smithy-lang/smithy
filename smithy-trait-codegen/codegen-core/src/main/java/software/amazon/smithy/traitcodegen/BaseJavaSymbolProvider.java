/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.CreateSymbolProviderDirective;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
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
import software.amazon.smithy.traitcodegen.utils.SymbolUtil;

final class BaseJavaSymbolProvider extends ShapeVisitor.Default<Symbol> implements SymbolProvider {
    private static final String NODE_FROM = "Node.from($L)";
    private static final String TO_NODE = "$L.toNode()";
    private static final String LIST_INITIALIZER = "forList()";
    private static final String MAP_INITIALIZER = "forOrderedMap()";

    private final String packageName;
    private final String packagePath;
    private final Model model;

    private BaseJavaSymbolProvider(TraitCodegenSettings settings, Model model) {
        this.packageName = settings.packageName();
        this.packagePath = "./" + packageName.replace(".", "/");
        this.model = model;
    }

    public static SymbolProvider fromDirective(CreateSymbolProviderDirective<TraitCodegenSettings> directive) {
        return new BaseJavaSymbolProvider(directive.settings(), directive.model());
    }

    public static Symbol simpleShapeSymbolFrom(Class<?> clazz) {
        return SymbolUtil.fromClass(clazz).toBuilder()
                .putProperty(SymbolProperties.TO_NODE_MAPPER, NODE_FROM)
                .putProperty(SymbolProperties.NODE_MAPPING_IMPORTS, SymbolUtil.fromClass(Node.class))
                .build();
    }

    // TODO: ToNode?
    @Override
    public Symbol blobShape(BlobShape shape) {
        return SymbolUtil.fromClass(ByteBuffer.class);
    }

    @Override
    public Symbol booleanShape(BooleanShape shape) {
        return simpleShapeSymbolFrom(Boolean.class);
    }

    // TODO: ToNode?
    @Override
    public Symbol byteShape(ByteShape shape) {
        return SymbolUtil.fromClass(Byte.class);
    }

    @Override
    public Symbol shortShape(ShortShape shape) {
        return simpleShapeSymbolFrom(Short.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().shortValue()")
                .putProperty(SymbolProperties.VALUE_GETTER, "shortValue()")
                .build();
    }

    @Override
    public Symbol integerShape(IntegerShape shape) {
        return simpleShapeSymbolFrom(Integer.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().intValue()")
                .putProperty(SymbolProperties.VALUE_GETTER, "intValue()")
                .build();
    }

    @Override
    public Symbol intEnumShape(IntEnumShape shape) {
        return Symbol.builder()
                .name(SymbolUtil.getDefaultName(shape))
                .putProperty(SymbolProperties.ENUM_VALUE_TYPE, SymbolUtil.fromClass(int.class))
                .putProperty(SymbolProperties.TO_NODE_MAPPER, NODE_FROM)
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, SymbolUtil.getDefaultName(shape) + ".fromNode($L)")
                .namespace(packageName, ".")
                .declarationFile(packagePath + "/" + SymbolUtil.getDefaultName(shape) + ".java")
                .build();
    }

    @Override
    public Symbol longShape(LongShape shape) {
        return simpleShapeSymbolFrom(Long.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().longValue()")
                .putProperty(SymbolProperties.VALUE_GETTER, "longValue()")
                .build();
    }

    @Override
    public Symbol floatShape(FloatShape shape) {
        return simpleShapeSymbolFrom(Float.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().floatValue()")
                .putProperty(SymbolProperties.VALUE_GETTER, "floatValue()")
                .build();
    }

    @Override
    public Symbol doubleShape(DoubleShape shape) {
        return simpleShapeSymbolFrom(Double.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().doubleValue()")
                .putProperty(SymbolProperties.VALUE_GETTER, "doubleValue()")
                .build();
    }

    @Override
    public Symbol bigIntegerShape(BigIntegerShape shape) {
        return simpleShapeSymbolFrom(BigInteger.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().intValue()")
                .putProperty(SymbolProperties.VALUE_GETTER, "intValue()")
                .build();
    }

    @Override
    public Symbol bigDecimalShape(BigDecimalShape shape) {
        return simpleShapeSymbolFrom(BigDecimal.class);
    }

    @Override
    public Symbol listShape(ListShape shape) {
        Symbol.Builder builder = SymbolUtil.fromClass(List.class).toBuilder()
                .addReference(toSymbol(shape.getMember()))
                .putProperty(SymbolProperties.BUILDER_REF_INITIALIZER, LIST_INITIALIZER);
        return builder.build();
    }

    @Override
    public Symbol mapShape(MapShape shape) {
        return SymbolUtil.fromClass(Map.class).toBuilder()
                .addReference(shape.getKey().accept(this))
                .addReference(shape.getValue().accept(this))
                .putProperty(SymbolProperties.BUILDER_REF_INITIALIZER, MAP_INITIALIZER)
                .build();
    }

    @Override
    public Symbol stringShape(StringShape shape) {
        return simpleShapeSymbolFrom(String.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectStringNode().getValue()")
                .build();
    }

    @Override
    public Symbol enumShape(EnumShape shape) {
        return Symbol.builder()
                .name(SymbolUtil.getDefaultName(shape))
                .putProperty(SymbolProperties.ENUM_VALUE_TYPE, SymbolUtil.fromClass(String.class))
                .putProperty(SymbolProperties.TO_NODE_MAPPER, "Node.from($L.getValue())")
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, SymbolUtil.getDefaultName(shape) + ".fromNode($L)")
                .namespace(packageName, ".")
                .declarationFile(packagePath + "/" + SymbolUtil.getDefaultName(shape) + ".java")
                .build();
    }

    @Override
    public Symbol structureShape(StructureShape shape) {
        Symbol.Builder builder = Symbol.builder()
                .name(SymbolUtil.getDefaultName(shape))
                .namespace(packageName, ".")
                .putProperty(SymbolProperties.TO_NODE_MAPPER, TO_NODE)
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, SymbolUtil.getDefaultName(shape) + ".fromNode($L)")
                .declarationFile(packagePath + "/" + SymbolUtil.getDefaultName(shape) + ".java");
        return builder.build();
    }

    @Override
    public Symbol memberShape(MemberShape shape) {
        return toSymbol(model.expectShape(shape.getTarget()));
    }

    @Override
    protected Symbol getDefault(Shape shape) {
        return null;
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        return shape.accept(this);
    }
}
