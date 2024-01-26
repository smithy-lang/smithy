/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates a Java class representation of a Smithy {@code DocumentShape} trait.
 */
final class DocumentTraitGenerator extends TraitGenerator {
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
