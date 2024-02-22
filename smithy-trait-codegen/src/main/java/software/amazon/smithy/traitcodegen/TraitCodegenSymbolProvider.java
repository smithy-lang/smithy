/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
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
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CaseUtils;
import software.amazon.smithy.utils.ListUtils;

final class TraitCodegenSymbolProvider extends ShapeVisitor.Default<Symbol> implements SymbolProvider {
    private static final String LIST_INITIALIZER = "forList()";
    private static final String MAP_INITIALIZER = "forOrderedMap()";
    private static final List<String> DELIMITERS = ListUtils.of("_", " ", "-");

    private final String packageNamespace;
    private final String smithyNamespace;
    private final Model model;

    TraitCodegenSymbolProvider(TraitCodegenSettings settings, Model model) {
        this.packageNamespace = settings.packageName();
        this.smithyNamespace = settings.smithyNamespace();
        this.model = model;
    }

    @Override
    public Symbol blobShape(BlobShape shape) {
        return TraitCodegenUtils.fromClass(ByteBuffer.class);
    }

    @Override
    public Symbol booleanShape(BooleanShape shape) {
        return simpleShapeSymbolFrom(Boolean.class).toBuilder()
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("BooleanMember($1S, builder::$1L)", s))
                .build();
    }

    @Override
    public Symbol byteShape(ByteShape shape) {
        return TraitCodegenUtils.fromClass(Byte.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().byteValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("NumberMember($1S, n -> builder.$1L(n.byteValue()))", s))
                .build();
    }

    @Override
    public Symbol shortShape(ShortShape shape) {
        return simpleShapeSymbolFrom(Short.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().shortValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER, "NumberMember($1S, n -> builder.$1L(n.shortValue()))")
                .build();
    }

    @Override
    public Symbol integerShape(IntegerShape shape) {
        return simpleShapeSymbolFrom(Integer.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().intValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("NumberMember($1S, n -> builder.$1L(n.intValue()))", s))
                .build();
    }

    @Override
    public Symbol intEnumShape(IntEnumShape shape) {
        return getJavaClassSymbolBuilder(shape)
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().intValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("NumberMember($1S, n -> builder.$1L(n.intValue()))", s))
                .build();
    }

    @Override
    public Symbol longShape(LongShape shape) {
        return simpleShapeSymbolFrom(Long.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().longValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("NumberMember($1S, n -> builder.$1L(n.longValue()))", s))
                .build();
    }

    @Override
    public Symbol floatShape(FloatShape shape) {
        return simpleShapeSymbolFrom(Float.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().floatValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("NumberMember($1S, n -> builder.$1L(n.floatValue()))", s))
                .build();
    }

    @Override
    public Symbol doubleShape(DoubleShape shape) {
        return simpleShapeSymbolFrom(Double.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().getValue().doubleValue()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("NumberMember($1S, n -> builder.$1L(n.doubleValue()))", s))
                .build();
    }

    @Override
    public Symbol bigIntegerShape(BigIntegerShape shape) {
        return simpleShapeSymbolFrom(BigInteger.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write(
                                "$L.expectNumberNode().asBigDecimal().get().toBigInteger()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("Member($1S, n -> "
                            + "n.expectNumberNode().asBigDecimal().get().toBigInteger(), builder::$1L)", s))
                .build();
    }

    @Override
    public Symbol bigDecimalShape(BigDecimalShape shape) {
        return simpleShapeSymbolFrom(BigDecimal.class).toBuilder()
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectNumberNode().asBigDecimal().get()", s))
                .putProperty(SymbolProperties.MEMBER_MAPPER,
                        (Mapper) (w, s) -> w.write("Member($1S, n -> "
                                + "n.expectNumberNode().asBigDecimal().get(), builder::$1L)", s))
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
                .putProperty(SymbolProperties.FROM_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$L.expectStringNode().getValue()", s))
                .build();
    }

    @Override
    public Symbol enumShape(EnumShape shape) {
        return getJavaClassSymbolBuilder(shape)
                .putProperty(SymbolProperties.TO_NODE_MAPPER,
                        (Mapper) (w, s) -> w.write("$T.from($L.getValue())", Node.class, s))
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, (Mapper) (w, s) -> fromNode(w, s, shape))
                .build();
    }

    @Override
    public Symbol structureShape(StructureShape shape) {
        return getJavaClassSymbolBuilder(shape)
                .putProperty(SymbolProperties.TO_NODE_MAPPER, (Mapper) (w, s) -> w.write("$L.toNode()", s))
                .putProperty(SymbolProperties.FROM_NODE_MAPPER, (Mapper) (w, s) -> fromNode(w, s, shape)).build();
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

    @Override
    public String toMemberName(MemberShape member) {
        Shape containerShape = model.expectShape(member.getContainer());
        // If the container is a Map list then we assign a simple "values" holder for the collection
        if (containerShape.isMapShape() || containerShape.isListShape()) {
            return "values";
        // Enum shapes should have upper snake case members
        } else if (containerShape.isEnumShape() || containerShape.isIntEnumShape()) {
            return CaseUtils.toSnakeCase(TraitCodegenUtils.MEMBER_ESCAPER.escape(member.getMemberName()))
                    .toUpperCase(Locale.ROOT);
        }

        if (DELIMITERS.stream().anyMatch(member.getMemberName()::contains)) {
            return TraitCodegenUtils.MEMBER_ESCAPER.escape(CaseUtils.toCamelCase(member.getMemberName()));
        } else {
            return TraitCodegenUtils.MEMBER_ESCAPER.escape(member.getMemberName());
        }
    }

    private static Symbol simpleShapeSymbolFrom(Class<?> clazz) {
        return TraitCodegenUtils.fromClass(clazz).toBuilder()
                .putProperty(SymbolProperties.TO_NODE_MAPPER, (Mapper) TraitCodegenSymbolProvider::nodeFrom)
                .build();
    }

    private static void nodeFrom(TraitCodegenWriter writer, String var) {
        writer.write("$T.from($L)", Node.class, var);
    }

    private static void fromNode(TraitCodegenWriter writer, String var, Shape shape) {
        writer.write("$L.fromNode($L)", TraitCodegenUtils.getDefaultName(shape), var);
    }

    private Symbol.Builder getJavaClassSymbolBuilder(Shape shape) {
        String name = TraitCodegenUtils.getDefaultName(shape);
        String namespace = TraitCodegenUtils.mapNamespace(smithyNamespace,
                shape.getId().getNamespace(), packageNamespace);
        return Symbol.builder().name(name)
                .namespace(namespace, ".")
                .declarationFile("./" + namespace.replace(".", "/") + "/"  + name + ".java");
    }
}
