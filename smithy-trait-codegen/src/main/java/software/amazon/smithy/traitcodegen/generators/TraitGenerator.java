/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Consumer that generates a trait class definition from a {@link GenerateTraitDirective}.
 *
 * <p>This base class can be extended to generate a trait class by overriding
 * the {@link #writeTraitBody(TraitCodegenWriter, GenerateTraitDirective)} method.
 * This base class will automatically generate a provider method and add that provider to the
 * {@code META-INF/services/software.amazon.smithy.model.traits.TraitService} service provider
 * file so the generated trait implementation will be discoverable by a {@code ServiceLoader}.
 */
abstract class TraitGenerator implements Consumer<GenerateTraitDirective> {
    private static final String PROVIDER_FILE = "META-INF/services/software.amazon.smithy.model.traits.TraitService";

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

    @Override
    public void accept(GenerateTraitDirective directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            // Add class definition context
            writer.putContext("baseClass", getBaseClass());
            writer.putContext("implementsToBuilder", implementsToSmithyBuilder());

            writer.pushState(new ClassSection(directive.shape()));
            writer.openBlock("public final class $2T extends $baseClass:T"
                            + "${?implementsToBuilder} implements $1T<$2T>${/implementsToBuilder} {", "}",
                    ToSmithyBuilder.class, directive.symbol(), () -> {
                // All traits include a static ID property
                writer.write("public static final $1T ID = $1T.from($2S);",
                        ShapeId.class, directive.shape().getId());
                writer.newLine();
                writeTraitBody(writer, directive);
                // Include the provider class
                writeProvider(writer, directive);
            });
            writer.popState();
        });
        // Add the trait provider to the META-INF/services/TraitService file
        addSpiTraitProvider(directive.context(), directive.symbol());
    }

    protected void writeProvider(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new ProviderGenerator(writer, directive.shape(), directive.symbol(), directive.symbolProvider()).run();
    }

    /**
     * Returns base class that the trait is a child of.
     *
     * <p>Defaults to {@link software.amazon.smithy.model.traits.AbstractTrait}.
     * Override this method to have the trait inherit from another base class.
     */
    protected Class<?> getBaseClass() {
        return AbstractTrait.class;
    }

    /**
     * Whether the class implements {@code ToSmithyBuilder}.
     *
     * <p>Defaults to false. Override this method to return true indicate that a trait does
     * implement ToSmithyBuilder.
     *
     * @return flag indicating if trait implements {@code ToSmithyBuilder}
     */
    protected boolean implementsToSmithyBuilder() {
        return false;
    }

    /**
     * Writes the body of the trait class.
     *
     * @param writer writer to use for writing trait class body.
     * @param directive directive to use for codegen context and shape information.
     */
    protected abstract void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive);
}
