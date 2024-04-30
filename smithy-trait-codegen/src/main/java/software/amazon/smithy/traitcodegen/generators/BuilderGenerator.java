/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Iterator;
import java.util.Optional;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Generates a static builder for a Java class.
 *
 * <p>In addition to the static builder class, this generator will create
 * {@code builder()} and {@code toBuilder()} methods for the target class.
 */
final class BuilderGenerator implements Runnable {

    private final TraitCodegenWriter writer;
    private final Symbol symbol;
    private final SymbolProvider symbolProvider;
    private final Shape baseShape;
    private final Model model;

    BuilderGenerator(TraitCodegenWriter writer, Symbol symbol, SymbolProvider symbolProvider, Shape baseShape,
                            Model model) {
        this.writer = writer;
        this.symbol = symbol;
        this.symbolProvider = symbolProvider;
        this.baseShape = baseShape;
        this.model = model;
    }

    @Override
    public void run() {
        // Only create builder methods for aggregate types.
        if (baseShape.getType().getCategory().equals(ShapeType.Category.AGGREGATE)) {
            writeToBuilderMethod();
            writeBuilderMethod();
            writeBuilderClass();
        }
    }

    private void writeBuilderClass() {
        writer.writeDocString(writer.format("Builder for {@link $T}.", symbol));
        writer.writeInline("public static final class Builder $C", (Runnable) this::writeBuilderInterface);
        writer.indent();
        baseShape.accept(new BuilderPropertyGenerator());
        writer.newLine();
        writer.writeWithNoFormatting("private Builder() {}").newLine();
        baseShape.accept(new BuilderSetterGenerator());
        writer.override();
        writer.openBlock("public $T build() {", "}", symbol,
                () -> writer.write("return new $C;", (Runnable) this::writeBuilderReturn));
        writer.dedent().write("}");
        writer.newLine();
    }

    private void writeBuilderInterface() {
        if (baseShape.hasTrait(TraitDefinition.class)) {
            if (TraitCodegenUtils.isJavaStringList(baseShape, symbolProvider)) {
                writer.write("extends $T.Builder<$T, Builder> {", StringListTrait.class, symbol);
            } else {
                writer.write("extends $T<$T, Builder> {", AbstractTraitBuilder.class, symbol);
            }
        } else {
            writer.write("implements $T<$T> {", SmithyBuilder.class, symbol);
        }
    }

    private void writeBuilderReturn() {
        // String list traits need a custom builder return
        if (TraitCodegenUtils.isJavaStringList(baseShape, symbolProvider)) {
            writer.write("$T(getValues(), getSourceLocation())", symbol);
        } else {
            writer.write("$T(this)", symbol);
        }
    }


    private void writeToBuilderMethod() {
        writer.writeDocString(writer.format("Creates a builder used to build a {@link $T}.", symbol));
        writer.openBlock("public $T<$T> toBuilder() {", "}",
                SmithyBuilder.class, symbol, () -> {
            writer.writeInlineWithNoFormatting("return builder()");
            writer.indent();
            if (baseShape.hasTrait(TraitDefinition.class)) {
                writer.writeInlineWithNoFormatting(".sourceLocation(getSourceLocation())");
            }
            if (baseShape.members().isEmpty()) {
                writer.writeInlineWithNoFormatting(";");
            }
            writer.newLine();

            // Set all builder properties for any members in the shape
            if (baseShape.isListShape()) {
                writer.writeWithNoFormatting(".values(getValues());");
            } else {
                Iterator<MemberShape> memberIterator = baseShape.members().iterator();
                while (memberIterator.hasNext()) {
                    MemberShape member = memberIterator.next();
                    writer.writeInline(".$1L($1L)", symbolProvider.toMemberName(member));
                    if (memberIterator.hasNext()) {
                        writer.writeInlineWithNoFormatting("\n");
                    } else {
                        writer.writeInlineWithNoFormatting(";\n");
                    }
                }
            }
            writer.dedent();
        });
        writer.newLine();
    }

    private void writeBuilderMethod() {
        writer.openBlock("public static Builder builder() {", "}",
                () -> writer.write("return new Builder();")).newLine();
    }

    private final class BuilderPropertyGenerator extends ShapeVisitor.Default<Void> {

        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("Does not support shape of type: " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            // String list shapes do not need value properties
            if (TraitCodegenUtils.isJavaStringList(shape, symbolProvider)) {
                return null;
            }
            writeValuesProperty(shape);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writeValuesProperty(shape);
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            shape.members().forEach(this::writeProperty);
            return null;
        }

        private void writeProperty(MemberShape shape) {
            Optional<String> builderRefOptional =
                    symbolProvider.toSymbol(shape).getProperty(SymbolProperties.BUILDER_REF_INITIALIZER);
            if (builderRefOptional.isPresent()) {
                writer.write("private final $1T<$2B> $3L = $1T.$4L;", BuilderRef.class,
                        symbolProvider.toSymbol(shape),
                        symbolProvider.toMemberName(shape),
                        builderRefOptional.orElseThrow(RuntimeException::new));
            } else {
                writer.write("private $B $L;", symbolProvider.toSymbol(shape),
                        symbolProvider.toMemberName(shape));
            }
        }

        private void writeValuesProperty(Shape shape) {
            Symbol collectionSymbol = symbolProvider.toSymbol(shape);
            writer.write("private final $1T<$2B> $3L = $1T.$4L;", BuilderRef.class,
                    collectionSymbol, "values",
                    collectionSymbol.expectProperty(SymbolProperties.BUILDER_REF_INITIALIZER));
        }
    }

    private final class BuilderSetterGenerator extends ShapeVisitor.Default<Void> {
        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("Does not support shape of type: " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            // String list shapes do not need setters
            if (TraitCodegenUtils.isJavaStringList(shape, symbolProvider)) {
                return null;
            }
            shape.accept(new SetterVisitor("values"));
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            shape.accept(new SetterVisitor("values"));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            shape.members().forEach(
                    memberShape -> memberShape.accept(new SetterVisitor(symbolProvider.toMemberName(memberShape))));
            return null;
        }
    }

    private final class SetterVisitor extends ShapeVisitor.Default<Void> {
        private final String memberName;

        private SetterVisitor(String memberName) {
            this.memberName = memberName;
        }

        @Override
        protected Void getDefault(Shape shape) {
            writer.openBlock("public Builder $1L($2B $1L) {", "}",
                    memberName, symbolProvider.toSymbol(shape), () -> {
                        writer.write("this.$1L = $1L;", memberName);
                        writer.writeWithNoFormatting("return this;");
                    }).newLine();
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.openBlock("public Builder $1L($2B $1L) {", "}",
                    memberName, symbolProvider.toSymbol(shape), () -> {
                        writer.write("clear$U();", memberName);
                        writer.write("this.$1L.get().addAll($1L);", memberName);
                        writer.writeWithNoFormatting("return this;");
                    }).newLine();

            // Clear all
            writer.openBlock("public Builder clear$U() {", "}", memberName, () -> {
                        writer.write("$L.get().clear();", memberName);
                        writer.writeWithNoFormatting("return this;");
                    }).newLine();

            // Set one
            writer.openBlock("public Builder add$U($T value) {", "}",
                    memberName, symbolProvider.toSymbol(shape.getMember()), () -> {
                        writer.write("$L.get().add(value);", memberName);
                        writer.write("return this;");
                    }).newLine();

            // Remove one
            writer.openBlock("public Builder remove$U($T value) {", "}",
                    memberName, symbolProvider.toSymbol(shape.getMember()),
                    () -> {
                        writer.write("$L.get().remove(value);", memberName);
                        writer.write("return this;");
                    }).newLine();
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            // Set all
            writer.openBlock("public Builder $1L($2B $1L) {", "}",
                    memberName, symbolProvider.toSymbol(shape), () -> {
                        writer.write("clear$U();", memberName);
                        writer.write("this.$1L.get().putAll($1L);", memberName);
                        writer.write("return this;");
                    });
            writer.newLine();

            // Clear all
            writer.openBlock("public Builder clear$U() {", "}", memberName, () -> {
                writer.write("this.$L.get().clear();", memberName);
                writer.write("return this;");
            }).newLine();

            // Set one
            MemberShape keyShape = shape.getKey();
            MemberShape valueShape = shape.getValue();
            writer.openBlock("public Builder put$U($T key, $T value) {", "}",
                    memberName, symbolProvider.toSymbol(keyShape), symbolProvider.toSymbol(valueShape), () -> {
                        writer.write("this.$L.get().put(key, value);", memberName);
                        writer.write("return this;");
                    }).newLine();

            // Remove one
            writer.openBlock("public Builder remove$U($T $L) {", "}",
                    memberName, symbolProvider.toSymbol(keyShape), memberName, () -> {
                        writer.write("this.$1L.get().remove($1L);", memberName);
                        writer.write("return this;");
                    }).newLine();
            return null;
        }

        @Override
        public Void memberShape(MemberShape shape) {
            return model.expectShape(shape.getTarget()).accept(this);
        }
    }
}
