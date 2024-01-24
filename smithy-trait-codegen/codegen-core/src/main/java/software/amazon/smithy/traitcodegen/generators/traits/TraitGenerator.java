/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Consumer that generates a trait class definition from a {@link GenerateTraitDirective}.
 */
public abstract class TraitGenerator implements Consumer<GenerateTraitDirective> {
    private static final String CLASS_TEMPLATE = "public final class $1T extends $baseClass:T"
            + "${?implementsToBuilder} implements ToSmithyBuilder<$1T>${/implementsToBuilder} {";
    private static final String PROVIDER_FILE = "META-INF/services/software.amazon.smithy.model.traits.TraitService";
    private static final String TRAIT_ID_TEMPLATE = "public static final ShapeId ID = ShapeId.from($S);";

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
            if (implementsToSmithyBuilder()) {
                writer.addImport(ToSmithyBuilder.class);
                writer.putContext("implementsToBuilder", true);
            }
            writer.addImport(ShapeId.class);
            writer.pushState(new ClassSection(directive.shape()));
            writer.openBlock(CLASS_TEMPLATE, "}", directive.symbol(), () -> {
                // All traits include a static ID property
                writer.write(TRAIT_ID_TEMPLATE, directive.shape().getId());
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
     * <p>
     * Defaults to {@link software.amazon.smithy.model.traits.AbstractTrait}.
     * Override this method to have the trait inherit from another base class.
     */
    protected Symbol getBaseClass() {
        return TraitCodegenUtils.fromClass(AbstractTrait.class);
    }

    /**
     * Whether the class implements {@code ToSmithyBuilder}.
     * <p>
     * Defaults to false. Override this method to return true indicate that a trait does
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
