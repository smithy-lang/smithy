/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Consumer that generates a trait class definition from a {@link GenerateTraitDirective}.
 *
 * <p>This base class will automatically generate a provider method and add that provider to the
 * {@code META-INF/services/software.amazon.smithy.model.traits.TraitService} service provider
 * file so the generated trait implementation will be discoverable by a {@code ServiceLoader}.
 */
class TraitGenerator implements Consumer<GenerateTraitDirective> {
    private static final String PROVIDER_FILE = "META-INF/services/software.amazon.smithy.model.traits.TraitService";

    @Override
    public void accept(GenerateTraitDirective directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            writer.pushState(new ClassSection(directive.shape()));
            // Add class definition context
            writer.putContext("baseClass", directive.shape().accept(new BaseClassVisitor(directive.symbolProvider())));
            writer.putContext("implementsToBuilder", directive.shape().accept(
                    new ImplementsSmithyBuilderVisitor(directive.symbolProvider())));
            writer.openBlock("public final class $2T extends $baseClass:T"
                            + "${?implementsToBuilder} implements $1T<$2T>${/implementsToBuilder} {", "}",
                    ToSmithyBuilder.class, directive.symbol(), () -> {
                // All traits include a static ID property
                writer.write("public static final $1T ID = $1T.from($2S);",
                        ShapeId.class, directive.shape().getId());
                writer.newLine();
                new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
                new ConstructorGenerator(writer, directive.symbol(), directive.shape(),
                        directive.symbolProvider()).run();
                // Abstract Traits need to define serde methods
                if (AbstractTrait.class.equals(
                        directive.shape().accept(new BaseClassVisitor(directive.symbolProvider())))
                ) {
                    new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(), directive.model()).run();
                }
                new FromNodeGenerator(writer, directive.symbol(), directive.shape(),
                        directive.symbolProvider(), directive.model()).run();
                new GetterGenerator(writer, directive.symbolProvider(), directive.model(), directive.shape()).run();
                directive.shape().accept(new NestedClassVisitor(writer, directive.symbolProvider(), directive.model()));
                if (directive.shape().accept(new ImplementsSmithyBuilderVisitor(directive.symbolProvider()))) {
                    new BuilderGenerator(writer, directive.symbol(), directive.symbolProvider(), directive.shape(),
                            directive.model()).run();
                }
                new ProviderGenerator(writer, directive.model(), directive.shape(),
                        directive.symbolProvider(), directive.symbol()).run();
            });
            writer.popState();
        });
        // Add the trait provider to the META-INF/services/TraitService file
        addSpiTraitProvider(directive.context(), directive.symbol());
    }

    /**
     * Write provider method to Java SPI to service file for {@link software.amazon.smithy.model.traits.TraitService}.
     *
     * @param context Codegen context
     * @param symbol  Symbol for trait class
     */
    private static void addSpiTraitProvider(TraitCodegenContext context, Symbol symbol) {
        context.writerDelegator().useFileWriter(PROVIDER_FILE,
                writer -> writer.writeInline("$L$$Provider", symbol.getFullName()));
    }

    /**
     * Returns the base class to use for a trait.
     */
    private static final class BaseClassVisitor extends ShapeVisitor.DataShapeVisitor<Class<?>> {
        private final SymbolProvider symbolProvider;

        private BaseClassVisitor(SymbolProvider symbolProvider) {
            this.symbolProvider = symbolProvider;
        }

        @Override
        public Class<?> booleanShape(BooleanShape shape) {
            throw new UnsupportedOperationException("Boolean traits not supported. Consider using an "
                    + " Annotation Trait.");
        }

        @Override
        public Class<?> listShape(ListShape shape) {
            // Do not create a property if the shape can inherit from the StringListTrait base class.
            if (!shape.hasTrait(UniqueItemsTrait.class)
                    && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getMember()))
            ) {
                return StringListTrait.class;
            }
            return AbstractTrait.class;
        }

        @Override
        public Class<?> mapShape(MapShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> byteShape(ByteShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> shortShape(ShortShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> integerShape(IntegerShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> longShape(LongShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> floatShape(FloatShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> documentShape(DocumentShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> doubleShape(DoubleShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> bigIntegerShape(BigIntegerShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> bigDecimalShape(BigDecimalShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> stringShape(StringShape shape) {
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                return StringTrait.class;
            }
            return AbstractTrait.class;
        }

        @Override
        public Class<?> enumShape(EnumShape shape) {
            return StringTrait.class;
        }

        @Override
        public Class<?> structureShape(StructureShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> timestampShape(TimestampShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Property generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Class<?> blobShape(BlobShape shape) {
            throw new UnsupportedOperationException("Property generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Class<?> memberShape(MemberShape shape) {
            throw new IllegalArgumentException("Property generator cannot visit member shapes. Attempted "
                    + "to visit " + shape);
        }
    }

    private static final class ImplementsSmithyBuilderVisitor extends ShapeVisitor.DataShapeVisitor<Boolean> {
        private final SymbolProvider symbolProvider;

        private ImplementsSmithyBuilderVisitor(SymbolProvider symbolProvider) {
            this.symbolProvider = symbolProvider;
        }

        @Override
        public Boolean listShape(ListShape shape) {
            if (!shape.hasTrait(UniqueItemsTrait.class)
                    && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getMember()))
            ) {
                return false;
            }
            return true;
        }

        @Override
        public Boolean mapShape(MapShape shape) {
            return true;
        }

        @Override
        public Boolean byteShape(ByteShape shape) {
            return false;
        }

        @Override
        public Boolean shortShape(ShortShape shape) {
            return false;
        }

        @Override
        public Boolean integerShape(IntegerShape shape) {
            return false;
        }

        @Override
        public Boolean intEnumShape(IntEnumShape shape) {
            return false;
        }

        @Override
        public Boolean longShape(LongShape shape) {
            return false;
        }

        @Override
        public Boolean floatShape(FloatShape shape) {
            return false;
        }

        @Override
        public Boolean documentShape(DocumentShape shape) {
            return false;
        }

        @Override
        public Boolean doubleShape(DoubleShape shape) {
            return false;
        }

        @Override
        public Boolean bigIntegerShape(BigIntegerShape shape) {
            return false;
        }

        @Override
        public Boolean bigDecimalShape(BigDecimalShape shape) {
            return false;
        }

        @Override
        public Boolean stringShape(StringShape shape) {
            return false;
        }

        @Override
        public Boolean enumShape(EnumShape shape) {
            return false;
        }

        @Override
        public Boolean structureShape(StructureShape shape) {
            return true;
        }

        @Override
        public Boolean timestampShape(TimestampShape shape) {
            return false;
        }

        @Override
        public Boolean booleanShape(BooleanShape shape) {
            throw new UnsupportedOperationException("Boolean traits not supported. Consider using an "
                    + " Annotation Trait.");
        }

        @Override
        public Boolean unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Property generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Boolean blobShape(BlobShape shape) {
            throw new UnsupportedOperationException("Property generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Boolean memberShape(MemberShape shape) {
            throw new IllegalArgumentException("Property generator cannot visit member shapes. Attempted "
                    + "to visit " + shape);
        }
    }

    private static final class NestedClassVisitor extends ShapeVisitor.Default<Void> {
        private final TraitCodegenWriter writer;
        private final SymbolProvider symbolProvider;
        private final Model model;

        private NestedClassVisitor(TraitCodegenWriter writer, SymbolProvider symbolProvider, Model model) {
            this.writer = writer;
            this.symbolProvider = symbolProvider;
            this.model = model;
        }

        @Override
        protected Void getDefault(Shape shape) {
            // Most classes have no nested classes
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            new EnumShapeGenerator.IntEnumShapeGenerator().writeEnum(shape, symbolProvider, writer, model, false);
            writer.newLine();
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            new EnumShapeGenerator.StringEnumShapeGenerator().writeEnum(shape, symbolProvider, writer, model, false);
            writer.newLine();
            return null;
        }
    }
}
