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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

final class TraitCodegenSymbolProvider extends ShapeVisitor.Default<Symbol> implements SymbolProvider {
    private static final String NODE_FROM = "Node.from($L)";
    private static final String TO_NODE = "$L.toNode()";
    private static final String LIST_INITIALIZER = "forList()";
    private static final String MAP_INITIALIZER = "forOrderedMap()";

    private final String packageName;
    private final String packagePath;
    private final Model model;

    TraitCodegenSymbolProvider(TraitCodegenSettings settings, Model model) {
        this.packageName = settings.packageName();
        this.packagePath = "./" + packageName.replace(".", "/");
        this.model = model;
    }

    @Override
    public Symbol blobShape(BlobShape shape) {
        return TraitCodegenUtils.fromClass(ByteBuffer.class);
    }

    @Override
    public Symbol booleanShape(BooleanShape shape) {
        return simpleShapeSymbolFrom(Boolean.class).toBuilder()
                .putProperty(SymbolProperties.MEMBER_MAPPER, "BooleanMember($1S, builder::$1L)")
                .build();
    }

    @Override
    public Symbol byteShape(ByteShape shape) {
        return TraitCodegenUtils.fromClass(Byte.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().byteValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.byteValue()))")
                .build();
    }

    @Override
    public Symbol shortShape(ShortShape shape) {
        return simpleShapeSymbolFrom(Short.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().shortValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.shortValue()))")
                .build();
    }

    @Override
    public Symbol integerShape(IntegerShape shape) {
        return simpleShapeSymbolFrom(Integer.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().intValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.intValue()))")
                .build();
    }

    @Override
    public Symbol intEnumShape(IntEnumShape shape) {
        return Symbol.builder()
                .name(TraitCodegenUtils.getDefaultName(shape))
                .putProperty(SymbolProperties.ENUM_VALUE_TYPE, TraitCodegenUtils.fromClass(int.class))
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().intValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.intValue()))")
                .namespace(packageName, ".")
                .declarationFile(packagePath + "/" + TraitCodegenUtils.getDefaultName(shape) + ".java")
                .build();
    }

    @Override
    public Symbol longShape(LongShape shape) {
        return simpleShapeSymbolFrom(Long.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().longValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.longValue()))")
                .build();
    }

    @Override
    public Symbol floatShape(FloatShape shape) {
        return simpleShapeSymbolFrom(Float.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().floatValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.floatValue()))")
                .build();
    }

    @Override
    public Symbol doubleShape(DoubleShape shape) {
        return simpleShapeSymbolFrom(Double.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().getValue().doubleValue()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.doubleValue()))")
                .build();
    }

    @Override
    public Symbol bigIntegerShape(BigIntegerShape shape) {
        return simpleShapeSymbolFrom(BigInteger.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        "$L.expectNumberNode().asBigDecimal().get().toBigInteger()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "Member($1S, n -> "
                        + "n.expectNumberNode().asBigDecimal().get().toBigInteger(), builder::$1L)")
                .build();
    }

    @Override
    public Symbol bigDecimalShape(BigDecimalShape shape) {
        return simpleShapeSymbolFrom(BigDecimal.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, "$L.expectNumberNode().asBigDecimal().get()")
                .putProperty(SymbolProperties.MEMBER_MAPPER, "Member($1S, n -> "
                        + "n.expectNumberNode().asBigDecimal().get(), builder::$1L)")
                .build();
    }

    @Override
    public Symbol listShape(ListShape shape) {
        Symbol.Builder builder = TraitCodegenUtils.fromClass(List.class).toBuilder()
                .addReference(toSymbol(shape.getMember()))
                .putProperty(SymbolProperties.BUILDER_REF_INITIALIZER, LIST_INITIALIZER);
        return builder.build();
    }

    @Override
    public Symbol mapShape(MapShape shape) {
        return TraitCodegenUtils.fromClass(Map.class).toBuilder()
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
                .name(TraitCodegenUtils.getDefaultName(shape))
                .putProperty(SymbolProperties.ENUM_VALUE_TYPE, TraitCodegenUtils.fromClass(String.class))
                .putProperty(SymbolProperties.TO_NODE_MAPPER, "Node.from($L.getValue())")
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        TraitCodegenUtils.getDefaultName(shape) + ".fromNode($L)")
                .namespace(packageName, ".")
                .declarationFile(packagePath + "/" + TraitCodegenUtils.getDefaultName(shape) + ".java")
                .build();
    }

    @Override
    public Symbol structureShape(StructureShape shape) {
        Symbol.Builder builder = Symbol.builder()
                .name(TraitCodegenUtils.getDefaultName(shape))
                .namespace(packageName, ".")
                .putProperty(SymbolProperties.TO_NODE_MAPPER, TO_NODE)
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        TraitCodegenUtils.getDefaultName(shape) + ".fromNode($L)")
                .declarationFile(packagePath + "/" + TraitCodegenUtils.getDefaultName(shape) + ".java");
        return builder.build();
    }

    @Override
    public Symbol documentShape(DocumentShape shape) {
        return simpleShapeSymbolFrom(ObjectNode.class);
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

    private static Symbol simpleShapeSymbolFrom(Class<?> clazz) {
        return TraitCodegenUtils.fromClass(clazz).toBuilder()
                .putProperty(SymbolProperties.TO_NODE_MAPPER, NODE_FROM)
                .putProperty(SymbolProperties.NODE_MAPPING_IMPORTS, TraitCodegenUtils.fromClass(Node.class))
                .build();
    }
}
