/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.generators.common.GetterGenerator;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.MapUtils;

/**
 * Generates an Annotation Trait, a structure trait with no body.
 */
final class AnnotationTraitGenerator extends TraitGenerator {
    @Override
    protected Symbol getBaseClass() {
        return TraitCodegenUtils.fromClass(AnnotationTrait.class);
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writeConstructor(writer, directive.symbol());
        writeEmptyConstructor(writer, directive.symbol());
        writeConstructorWithSourceLocation(writer, directive.symbol());
        new GetterGenerator(writer, directive.symbolProvider(), directive.shape(), directive.model()).run();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImport(ObjectNode.class);
        writer.openBlock("public $T(ObjectNode node) {", "}", symbol,
                () -> writer.write("super(ID, node);"));
        writer.newLine();
    }

    private void writeEmptyConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImport(Node.class);
        writer.openBlock("public $T() {", "}", symbol,
                () -> writer.write("super(ID, Node.objectNode());"));
        writer.newLine();
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImports(SourceLocation.class, ObjectNode.class, MapUtils.class);
        writer.openBlock("public $T(SourceLocation sourceLocation) {", "}", symbol,
                () -> writer.write("this(new ObjectNode(MapUtils.of(), sourceLocation));"));
        writer.newLine();
    }
}
