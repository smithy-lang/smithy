/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.CreateContextDirective;
import software.amazon.smithy.codegen.core.directed.CreateSymbolProviderDirective;
import software.amazon.smithy.codegen.core.directed.DirectedCodegen;
import software.amazon.smithy.codegen.core.directed.GenerateEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateErrorDirective;
import software.amazon.smithy.codegen.core.directed.GenerateIntEnumDirective;
import software.amazon.smithy.codegen.core.directed.GenerateServiceDirective;
import software.amazon.smithy.codegen.core.directed.GenerateStructureDirective;
import software.amazon.smithy.codegen.core.directed.GenerateUnionDirective;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.generators.base.EnumGenerator;
import software.amazon.smithy.traitcodegen.generators.base.IntEnumGenerator;
import software.amazon.smithy.traitcodegen.generators.base.StructureGenerator;
import software.amazon.smithy.traitcodegen.generators.traits.DefaultTraitGeneratorProvider;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * The main entry points for trait code generation.
 */
@SmithyUnstableApi
final class TraitCodegenDirectedCodegen
        implements DirectedCodegen<TraitCodegenContext, TraitCodegenSettings, TraitCodegenIntegration> {

    @Override
    public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<TraitCodegenSettings> directive) {
        return TraitCodegenSymbolProvider.fromDirective(directive);
    }

    @Override
    public TraitCodegenContext createContext(
            CreateContextDirective<TraitCodegenSettings, TraitCodegenIntegration> directive
    ) {
        return TraitCodegenContext.fromDirective(directive);
    }

    @Override
    public void generateService(
            GenerateServiceDirective<TraitCodegenContext, TraitCodegenSettings> directive
    ) {
        TraitGeneratorProvider provider = new DefaultTraitGeneratorProvider();
        for (TraitCodegenIntegration integration : directive.context().integrations()) {
            provider = integration.decorateGeneratorProvider(directive.context(), provider);
        }
        // Execute generators for all trait shapes connected to synthetic service
        for (Shape shape : directive.connectedShapes().values()) {
            if (shape.hasTrait(TraitDefinition.class)) {
                provider.getGenerator(shape).accept(new GenerateTraitDirective(directive.context(), shape));
            }
        }
    }

    @Override
    public void generateError(GenerateErrorDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
        // Do nothing on error generation
    }

    @Override
    public void generateStructure(GenerateStructureDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
        if (!directive.shape().hasTrait(TraitDefinition.class)) {
            new StructureGenerator().accept(directive);
        }
    }

    @Override
    public void generateUnion(GenerateUnionDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
        throw new UnsupportedOperationException("trait codegen does not support generation of union traits");
    }

    @Override
    public void generateEnumShape(GenerateEnumDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
        if (!directive.shape().hasTrait(TraitDefinition.class)) {
            new EnumGenerator().accept(directive);
        }
    }

    @Override
    public void generateIntEnumShape(GenerateIntEnumDirective<TraitCodegenContext, TraitCodegenSettings> directive) {
        if (!directive.shape().hasTrait(TraitDefinition.class)) {
            new IntEnumGenerator().accept(directive);
        }
    }
}
