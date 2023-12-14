/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.generators.common.ToNodeGenerator;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

final class DocumentTraitGenerator extends TraitGenerator {
    private static final String CLASS_TEMPLATE = "public final class $T extends AbstractTrait {";

    @Override
    protected void imports(TraitCodegenWriter writer) {
        writer.addImports(Node.class, AbstractTrait.class);
    }

    @Override
    protected String getClassDefinition() {
        return CLASS_TEMPLATE;
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writeConstructor(writer, directive.symbol());
        writer.newLine();
        new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(), directive.model()).run();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.openBlock("public $T(Node value) {", "}", symbol,
                () -> writer.write("super(ID, value);"));
    }
}
