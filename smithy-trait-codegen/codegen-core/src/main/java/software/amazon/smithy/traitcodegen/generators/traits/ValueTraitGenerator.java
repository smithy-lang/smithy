/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.generators.common.GetterGenerator;
import software.amazon.smithy.traitcodegen.generators.common.PropertiesGenerator;
import software.amazon.smithy.traitcodegen.generators.common.ToNodeGenerator;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

final class ValueTraitGenerator extends TraitGenerator {
    private static final String CLASS_TEMPLATE = "public final class $T extends AbstractTrait {";

    @Override
    protected void imports(TraitCodegenWriter writer) {
        writer.addImport(AbstractTrait.class);
    }

    @Override
    protected String getClassDefinition() {
        return CLASS_TEMPLATE;
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
        writeConstructor(writer, directive.traitSymbol(), directive.baseSymbol());
        writeConstructorWithSourceLocation(writer, directive.traitSymbol(), directive.baseSymbol());
        new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(), directive.model()).run();
        new GetterGenerator(writer, directive.symbolProvider(), directive.shape(), directive.model()).run();
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol traitSymbol, Symbol baseSymbol) {
        writer.addImport(FromSourceLocation.class);
        writer.openBlock("public $T($T value, FromSourceLocation sourceLocation) {", "}",
                traitSymbol, baseSymbol, () -> {
                    writer.write("super(ID, sourceLocation);");
                    writer.write("this.value = value;");
                });
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol traitSymbol, Symbol baseSymbol) {
        writer.addImport(SourceLocation.class);
        writer.openBlock("public $T($T value) {", "}",
                traitSymbol, baseSymbol, () -> {
                    writer.write("super(ID, SourceLocation.NONE);");
                    writer.write("this.value = value;");
                });
        writer.newLine();
    }
}
