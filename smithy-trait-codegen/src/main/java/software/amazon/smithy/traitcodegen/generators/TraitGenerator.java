/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.StringTrait;
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
            // Only collection types implement ToSmithyBuilder
            boolean isAggregateType = directive.shape().getType().getCategory().equals(ShapeType.Category.AGGREGATE);
            writer.putContext("isAggregateType", isAggregateType);
            writer.openBlock("public final class $2T extends $baseClass:T"
                    + "${?isAggregateType} implements $1T<$2T>${/isAggregateType} {",
                    "}",
                    ToSmithyBuilder.class,
                    directive.symbol(),
                    () -> {
                        // All traits include a static ID property
                        writer.write("public static final $1T ID = $1T.from($2S);",
                                ShapeId.class,
                                directive.shape().getId());
                        writer.newLine();
                        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
                        new ConstructorGenerator(writer,
                                directive.symbol(),
                                directive.shape(),
                                directive.symbolProvider()).run();
                        // Abstract Traits need to define serde methods
                        if (AbstractTrait.class.equals(
                                directive.shape().accept(new BaseClassVisitor(directive.symbolProvider())))) {
                            new ToNodeGenerator(writer,
                                    directive.shape(),
                                    directive.symbolProvider(),
                                    directive.model()).run();
                        }
                        new FromNodeGenerator(writer,
                                directive.symbol(),
                                directive.shape(),
                                directive.symbolProvider(),
                                directive.model()).run();
                        new GetterGenerator(writer, directive.symbolProvider(), directive.model(), directive.shape())
                                .run();
                        directive.shape()
                                .accept(new NestedClassVisitor(writer, directive.symbolProvider(), directive.model()));
                        new BuilderGenerator(writer,
                                directive.symbol(),
                                directive.symbolProvider(),
                                directive.shape(),
                                directive.model()).run();
                        new ProviderGenerator(writer,
                                directive.model(),
                                directive.shape(),
                                directive.symbolProvider(),
                                directive.symbol()).run();
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
        context.writerDelegator()
                .useFileWriter(PROVIDER_FILE,
                        writer -> writer.writeInline("$L$$Provider", symbol.getFullName()));
    }

    /**
     * Returns the base class to use for a trait.
     */
    private static final class BaseClassVisitor extends TraitVisitor<Class<?>> {
        private final SymbolProvider symbolProvider;

        private BaseClassVisitor(SymbolProvider symbolProvider) {
            this.symbolProvider = symbolProvider;
        }

        @Override
        public Class<?> listShape(ListShape shape) {
            // Do not create a property if the shape can inherit from the StringListTrait base class.
            if (TraitCodegenUtils.isJavaStringList(shape, symbolProvider)) {
                return StringListTrait.class;
            }
            return AbstractTrait.class;
        }

        @Override
        public Class<?> mapShape(MapShape shape) {
            return AbstractTrait.class;
        }

        @Override
        public Class<?> documentShape(DocumentShape shape) {
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
        protected Class<?> numberShape(NumberShape shape) {
            return AbstractTrait.class;
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
