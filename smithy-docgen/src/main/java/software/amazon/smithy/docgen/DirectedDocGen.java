/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.CreateContextDirective;
import software.amazon.smithy.codegen.core.directed.CreateSymbolProviderDirective;
import software.amazon.smithy.codegen.core.directed.DirectedCodegen;
import software.amazon.smithy.codegen.core.directed.GenerateEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateErrorDirective;
import software.amazon.smithy.codegen.core.directed.GenerateIntEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateOperationDirective;
import software.amazon.smithy.codegen.core.directed.GenerateResourceDirective;
import software.amazon.smithy.codegen.core.directed.GenerateServiceDirective;
import software.amazon.smithy.codegen.core.directed.GenerateStructureDirective;
import software.amazon.smithy.codegen.core.directed.GenerateUnionDirective;
import software.amazon.smithy.docgen.generators.MemberGenerator.MemberListingType;
import software.amazon.smithy.docgen.generators.OperationGenerator;
import software.amazon.smithy.docgen.generators.ResourceGenerator;
import software.amazon.smithy.docgen.generators.ServiceGenerator;
import software.amazon.smithy.docgen.generators.StructuredShapeGenerator;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * The main entry points for documentation generation.
 */
@SmithyUnstableApi
final class DirectedDocGen implements DirectedCodegen<DocGenerationContext, DocSettings, DocIntegration> {

    @Override
    public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<DocSettings> directive) {
        return new DocSymbolProvider(directive.model(), directive.settings());
    }

    @Override
    public DocGenerationContext createContext(CreateContextDirective<DocSettings, DocIntegration> directive) {
        return new DocGenerationContext(
                directive.model(),
                directive.settings(),
                directive.symbolProvider(),
                directive.fileManifest(),
                directive.integrations());
    }

    @Override
    public void generateService(GenerateServiceDirective<DocGenerationContext, DocSettings> directive) {
        new ServiceGenerator().accept(directive);
    }

    @Override
    public void generateStructure(GenerateStructureDirective<DocGenerationContext, DocSettings> directive) {
        // Input and output structures are documented alongside the relevant operations.
        if (directive.shape().hasTrait(InputTrait.class) || directive.shape().hasTrait(OutputTrait.class)) {
            return;
        }
        new StructuredShapeGenerator(directive.context()).accept(directive.shape(), MemberListingType.MEMBERS);
    }

    @Override
    public void generateOperation(GenerateOperationDirective<DocGenerationContext, DocSettings> directive) {
        new OperationGenerator().accept(directive);
    }

    @Override
    public void generateError(GenerateErrorDirective<DocGenerationContext, DocSettings> directive) {
        new StructuredShapeGenerator(directive.context()).accept(directive.shape(), MemberListingType.MEMBERS);
    }

    @Override
    public void generateUnion(GenerateUnionDirective<DocGenerationContext, DocSettings> directive) {
        new StructuredShapeGenerator(directive.context()).accept(directive.shape(), MemberListingType.OPTIONS);
    }

    @Override
    public void generateEnumShape(GenerateEnumDirective<DocGenerationContext, DocSettings> directive) {
        new StructuredShapeGenerator(directive.context()).accept(directive.shape(), MemberListingType.OPTIONS);
    }

    @Override
    public void generateIntEnumShape(GenerateIntEnumDirective<DocGenerationContext, DocSettings> directive) {
        var shape = directive.shape();
        var intEnum = shape.asIntEnumShape()
                .orElseThrow(() -> new ExpectationNotMetException(
                        "Expected an intEnum shape, but found " + shape,
                        shape));
        new StructuredShapeGenerator(directive.context()).accept(intEnum, MemberListingType.OPTIONS);
    }

    @Override
    public void generateResource(GenerateResourceDirective<DocGenerationContext, DocSettings> directive) {
        new ResourceGenerator().accept(directive.context(), directive.shape());
    }
}
