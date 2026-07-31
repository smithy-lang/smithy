/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyInternalApi;

public final class UnionShapeGenerator implements Consumer<GenerateTraitDirective> {
    @Override
    public void accept(GenerateTraitDirective directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            writer.pushState(new ClassSection(directive.shape()))
                    .openBlock("public abstract class $T implements $T {",
                            "}",
                            directive.symbol(),
                            ToNode.class,
                            () -> {
                                new PropertiesGenerator(writer,
                                        directive.shape(),
                                        directive.symbolProvider()).run();
                                new ConstructorGenerator(writer,
                                        directive.symbol(),
                                        directive.shape(),
                                        directive.symbolProvider()).run();
                                new GetterGenerator(writer,
                                        directive.symbolProvider(),
                                        directive.model(),
                                        directive.shape()).run();
                                new ToNodeGenerator(writer,
                                        directive.shape(),
                                        directive.symbolProvider(),
                                        directive.model()).run();
                                new FromNodeGenerator(writer,
                                        directive.symbol(),
                                        directive.shape(),
                                        directive.symbolProvider(),
                                        directive.model()).run();
                                writeNestedClasses(directive.shape(),
                                        writer,
                                        directive.symbolProvider(),
                                        directive.model(),
                                        directive.context().settings());
                                new BuilderGenerator(writer,
                                        directive.symbol(),
                                        directive.symbolProvider(),
                                        directive.shape(),
                                        directive.model()).run();
                                writeEquals(writer, directive.symbol());
                                writeHashCode(writer);
                            })
                    .popState();
        });
    }

    /**
     * Writes nested classes for union shapes.
     *
     * @param shape union shape to generate nested classes for.
     * @param writer writer to write generated code to.
     * @param symbolProvider symbol provider.
     * @param model smithy model used for code generation.
     * @param settings trait codegen settings.
     */
    @SmithyInternalApi
    public void writeNestedClasses(
            Shape shape,
            TraitCodegenWriter writer,
            SymbolProvider symbolProvider,
            Model model,
            TraitCodegenSettings settings
    ) {
        writeEnumClass(writer, shape, symbolProvider);
        writeVariantClasses(writer, shape, symbolProvider, model, settings);
    }

    private void writeVariantClasses(
            TraitCodegenWriter writer,
            Shape unionShape,
            SymbolProvider symbolProvider,
            Model model,
            TraitCodegenSettings settings
    ) {
        writer.writeWithNoFormatting("abstract Node asNode();");
        writer.newLine();

        // The type-safe accessor, narrowed to the variant's value type by a covariant
        // return on each member.
        writer.writeWithNoFormatting("public abstract Object getContents();");
        writer.newLine();

        // The original accessor erases its type and is only retained for backwards
        // compatibility.
        if (!settings.excludeDeprecatedUnionGetters()) {
            writeDeprecatedGetValue(writer);
        }

        Symbol unionSymbol = symbolProvider.toSymbol(unionShape);
        boolean isTrait = unionShape.hasTrait(TraitDefinition.ID);
        for (MemberShape memberShape : unionShape.members()) {
            writer.pushState(new ClassSection(memberShape));
            String tag = memberShape.getMemberName();
            String variantClassName = symbolProvider.toMemberName(memberShape) + "Member";
            writer.addLocalDefinedName(variantClassName);
            Symbol memberSymbol = symbolProvider.toSymbol(memberShape);
            boolean isTargetUnitType = model.expectShape(memberShape.getTarget()).hasTrait(UnitTypeTrait.ID);
            writer.putContext("isTargetUnitType", isTargetUnitType);
            writer.putContext("isTrait", isTrait);
            writer.openBlock("public static final class $U extends $T {",
                    "}",
                    variantClassName,
                    unionSymbol,
                    () -> {
                        writeValueField(writer, memberSymbol);
                        writeConstructors(writer, memberSymbol, tag, isTrait, isTargetUnitType);
                        writer.newLine();

                        writeFromMethod(writer, memberShape, variantClassName, symbolProvider, model);
                        writer.newLine();

                        writeAsNodeMethod(writer, memberShape, symbolProvider, model);
                        writer.newLine();

                        writeGetContents(writer, memberSymbol, isTargetUnitType);
                    });
            writer.newLine();
            writer.popState();
        }
    }

    private void writeEnumClass(TraitCodegenWriter writer, Shape shape, SymbolProvider symbolProvider) {
        writer.addLocalDefinedName("Type");
        List<String> enumList = new ArrayList<>();
        writer.putContext("variants", enumList);
        for (MemberShape member : shape.members()) {
            enumList.add(symbolProvider.toMemberName(member));
        }
        // Generate enum class based on union tags.
        writer.openBlock("public enum Type {",
                "}",
                () -> {
                    // enum variants
                    writer.write("${#variants}" +
                            "${value:L}(\"${value:L}\")" + // write variant name
                            "${^key.last},\n${/key.last}${?key.last};\n${/key.last}" + // write comma or semi colon
                            "${/variants}");
                    writer.write("private final $T value;", String.class);
                    writer.newLine();
                    // constructor
                    writer.openBlock("Type($T value) {",
                            "}",
                            String.class,
                            () -> writer.write("this.value = value;"));
                    writer.newLine();
                    // getValue() method
                    writer.openBlock("public $T getValue() {",
                            "}",
                            String.class,
                            () -> writer.write("return this.value;"));
                });
        writer.newLine();
    }

    private void writeValueField(TraitCodegenWriter writer, Symbol memberSymbol) {
        // No value field for Unit type
        writer.writeInline("${^isTargetUnitType}private final $T value;\n\n${/isTargetUnitType}", memberSymbol);
    }

    private void writeConstructors(
            TraitCodegenWriter writer,
            Symbol memberSymbol,
            String tag,
            boolean isTrait,
            boolean isTargetUnitType
    ) {
        boolean needComma = isTrait && !isTargetUnitType;
        writer.putContext("needComma", needComma);

        if (isTrait) {
            writer.openBlock("private $U(${^isTargetUnitType}$T value${/isTargetUnitType}) {",
                    "}",
                    tag + "Member",
                    memberSymbol,
                    () -> writer.write("this(${^isTargetUnitType}value${/isTargetUnitType}" +
                            "${?needComma}, ${/needComma}${?isTrait}$T.NONE${/isTrait});", SourceLocation.class));
            writer.newLine();
        }
        writer.openBlock("private $U(${^isTargetUnitType}$T value${/isTargetUnitType}" +
                "${?needComma}, ${/needComma}${?isTrait}$T sourceLocation${/isTrait}) {",
                "}",
                tag + "Member",
                memberSymbol,
                FromSourceLocation.class,
                () -> {
                    writer.write("super(Type.$L${?isTrait}, sourceLocation${/isTrait});", tag);
                    writer.writeInline("${^isTargetUnitType}this.value = " +
                            "$T.requireNonNull(value);\n${/isTargetUnitType}", Objects.class);
                });
    }

    private void writeFromMethod(
            TraitCodegenWriter writer,
            MemberShape memberShape,
            String variantClassName,
            SymbolProvider provider,
            Model model
    ) {
        writer.openBlock("private static $U from($T node) {",
                "}",
                variantClassName,
                Node.class,
                () -> memberShape.accept(new VariantFromNodeGenerator(writer, variantClassName, provider, model)));
    }

    private void writeAsNodeMethod(
            TraitCodegenWriter writer,
            MemberShape memberShape,
            SymbolProvider provider,
            Model model
    ) {
        writer.override();
        writer.openBlock("$T asNode() {",
                "}",
                Node.class,
                () -> memberShape.accept(new VariantAsNodeGenerator(writer, model, provider)));
    }

    private void writeGetContents(
            TraitCodegenWriter writer,
            Symbol memberSymbol,
            boolean isTargetUnitType
    ) {
        writer.override();

        // A covariant return narrows the value to its concrete type so callers retain
        // it after an instanceof check. The unit variant has no value, so it falls
        // back to the base Object return type.
        if (isTargetUnitType) {
            writer.openBlock("public Object getContents() {",
                    "}",
                    () -> writer.writeWithNoFormatting("return null;"));
        } else {
            writer.openBlock("public $T getContents() {",
                    "}",
                    memberSymbol,
                    () -> writer.writeWithNoFormatting("return value;"));
        }
    }

    private void writeDeprecatedGetValue(TraitCodegenWriter writer) {
        writer.writeWithNoFormatting("/**");
        writer.writeWithNoFormatting(
                " * @deprecated this getter erases the value type, use {@link #getContents()} instead.");
        writer.writeWithNoFormatting(" */");
        writer.writeWithNoFormatting("@Deprecated");
        writer.writeWithNoFormatting("@SuppressWarnings(\"unchecked\")");
        writer.openBlock("public <T> T getValue() {",
                "}",
                () -> writer.writeWithNoFormatting("return (T) getContents();"));
        writer.newLine();
    }

    private void writeEquals(TraitCodegenWriter writer, Symbol symbol) {
        writer.override();
        writer.openBlock("public boolean equals(Object other) {",
                "}",
                () -> {
                    writer.disableNewlines();
                    writer.openBlock("if (other == this) {\n",
                            "}",
                            () -> writer.writeWithNoFormatting("return true;").newLine());
                    writer.openBlock(" else if (!(other instanceof $T)) {\n",
                            "}",
                            symbol,
                            () -> writer.writeWithNoFormatting("return false;").newLine());
                    writer.openBlock(" else {\n",
                            "}",
                            () -> {
                                writer.write("$1T b = ($1T) other;", symbol).newLine();
                                writer.writeWithNoFormatting(
                                        "return type == b.getType() && Objects.equals(getContents(), b.getContents());\n");
                            }).newLine();
                    writer.enableNewlines();
                });
        writer.newLine();
    }

    private void writeHashCode(TraitCodegenWriter writer) {
        writer.override();
        writer.openBlock("public int hashCode() {",
                "}",
                () -> writer.write("return $T.hash(type, getContents());", Objects.class));
    }

    private static final class VariantFromNodeGenerator extends ShapeVisitor.Default<Void> {

        private final TraitCodegenWriter writer;
        private final String variantClassName;
        private final Model model;
        private final SymbolProvider symbolProvider;

        VariantFromNodeGenerator(
                TraitCodegenWriter writer,
                String variantClassName,
                SymbolProvider symbolProvider,
                Model model
        ) {
            this.writer = writer;
            this.variantClassName = variantClassName;
            this.model = model;
            this.symbolProvider = symbolProvider;
        }

        @Override
        protected Void getDefault(Shape shape) {
            writer.write("return new $U(${^isTargetUnitType}$C${/isTargetUnitType}" +
                    "${?needComma}, ${/needComma}" +
                    "${?isTrait}node${/isTrait});",
                    variantClassName,
                    (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "node", 0, symbolProvider)));
            return null;
        }

        @Override
        public Void memberShape(MemberShape memberShape) {
            if (memberShape.hasTrait(IdRefTrait.ID) || memberShape.hasTrait(TimestampFormatTrait.ID)) {
                writer.write("return new $U($C);",
                        variantClassName,
                        (Runnable) () -> memberShape
                                .accept(new FromNodeMapperVisitor(writer, model, "node", 0, symbolProvider)));
                return null;
            }
            Shape target = model.expectShape(memberShape.getTarget());
            target.accept(this);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            shape.accept(new FromNodeMapperVisitor(writer, model, "node", 1, symbolProvider));
            writer.write("return new $U(value1${?isTrait}, node${/isTrait});", variantClassName);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            shape.accept(new FromNodeMapperVisitor(writer, model, "node", 1, symbolProvider));
            writer.write("return new $U(value1${?isTrait}, node${/isTrait});", variantClassName);
            return null;
        }
    }

    private static final class VariantAsNodeGenerator extends ShapeVisitor.Default<Void> {

        private final TraitCodegenWriter writer;
        private final Model model;
        private final SymbolProvider symbolProvider;

        VariantAsNodeGenerator(TraitCodegenWriter writer, Model model, SymbolProvider symbolProvider) {
            this.writer = writer;
            this.model = model;
            this.symbolProvider = symbolProvider;
        }

        @Override
        protected Void getDefault(Shape shape) {
            writer.write("return $C;",
                    (Runnable) () -> shape.accept(new ToNodeMapperVisitor(writer, model, "value", 1, symbolProvider)));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            if (shape.hasTrait(UnitTypeTrait.ID)) {
                writer.write("return $T.objectNode();", Node.class);
            } else {
                this.getDefault(shape);
            }
            return null;
        }

        @Override
        public Void memberShape(MemberShape memberShape) {
            if (memberShape.hasTrait(IdRefTrait.ID) || memberShape.hasTrait(TimestampFormatTrait.ID)) {
                this.getDefault(memberShape);
                return null;
            }
            Shape target = model.expectShape(memberShape.getTarget());
            target.accept(this);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            shape.accept(new ToNodeMapperVisitor(writer, model, "value", 1, symbolProvider));
            writer.writeWithNoFormatting("return builder1.build();");
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            shape.accept(new ToNodeMapperVisitor(writer, model, "value", 1, symbolProvider));
            writer.writeWithNoFormatting("return builder1.build();");
            return null;
        }
    }
}
