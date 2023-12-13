/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.List;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.codegen.core.directed.CreateContextDirective;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

public final class TraitCodegenContext implements CodegenContext<TraitCodegenSettings, TraitCodegenWriter,
        TraitCodegenIntegration> {
    private final Model model;
    private final TraitCodegenSettings settings;
    private final SymbolProvider baseSymbolProvider;
    private final SymbolProvider traitSymbolProvider;
    private final FileManifest fileManifest;
    private final List<TraitCodegenIntegration> integrations;
    private final WriterDelegator<TraitCodegenWriter> writerDelegator;


    private TraitCodegenContext(Model model,
                                TraitCodegenSettings settings,
                                SymbolProvider symbolProvider,
                                FileManifest fileManifest,
                                List<TraitCodegenIntegration> integrations
    ) {
        this.model = model;
        this.settings = settings;
        this.baseSymbolProvider = symbolProvider;
        this.traitSymbolProvider = SymbolProvider.cache(new TraitSymbolProvider(settings));
        this.fileManifest = fileManifest;
        this.integrations = integrations;
        this.writerDelegator = new WriterDelegator<>(fileManifest, shape -> {
            if (shape.hasTrait(TraitDefinition.class)) {
                return traitSymbolProvider.toSymbol(shape);
            } else {
                return baseSymbolProvider.toSymbol(shape);
            }
        }, (filename, namespace) -> new TraitCodegenWriter(filename, namespace, settings));
    }

    public static TraitCodegenContext fromDirective(
            CreateContextDirective<TraitCodegenSettings, TraitCodegenIntegration> directive
    ) {
        return new TraitCodegenContext(
                directive.model(),
                directive.settings(),
                directive.symbolProvider(),
                directive.fileManifest(),
                directive.integrations()
        );
    }

    @Override
    public Model model() {
        return model;
    }

    @Override
    public TraitCodegenSettings settings() {
        return settings;
    }

    @Override
    public SymbolProvider symbolProvider() {
        return baseSymbolProvider;
    }

    @Override
    public FileManifest fileManifest() {
        return fileManifest;
    }

    @Override
    public WriterDelegator<TraitCodegenWriter> writerDelegator() {
        return writerDelegator;
    }

    @Override
    public List<TraitCodegenIntegration> integrations() {
        return integrations;
    }

    public SymbolProvider traitSymbolProvider() {
        return traitSymbolProvider;
    }
}
