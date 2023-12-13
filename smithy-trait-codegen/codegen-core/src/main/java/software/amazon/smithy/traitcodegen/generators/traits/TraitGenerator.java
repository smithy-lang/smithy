/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

public abstract class TraitGenerator implements Consumer<GenerateTraitDirective> {
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
            imports(writer);
            writer.addImport(ShapeId.class);
            writer.pushState(new ClassSection(directive.shape()));
            writer.openBlock(getClassDefinition(), "}", directive.traitSymbol(), () -> {
                writer.write(TRAIT_ID_TEMPLATE, directive.shape().getId());
                writer.newLine();
                writeTraitBody(writer, directive);
                writeProvider(writer, directive);
            });
            writer.popState();
        });
        addSpiTraitProvider(directive.context(), directive.traitSymbol());
    }

    protected void writeProvider(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new ProviderGenerator(writer, directive.shape(), directive.traitSymbol(), directive.symbolProvider()).run();
    }

    protected abstract void imports(TraitCodegenWriter writer);

    protected abstract String getClassDefinition();

    protected abstract void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive);
}
