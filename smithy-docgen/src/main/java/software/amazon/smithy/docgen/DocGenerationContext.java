/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import java.util.LinkedHashSet;
import java.util.List;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.docgen.DocSymbolProvider.FileExtensionDecorator;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contextual information that is made available during most parts of documentation
 * generation.
 */
@SmithyUnstableApi
public final class DocGenerationContext implements CodegenContext<DocSettings, DocWriter, DocIntegration> {
    private final Model model;
    private final DocSettings docSettings;
    private final SymbolProvider symbolProvider;
    private final FileManifest fileManifest;
    private final WriterDelegator<DocWriter> writerDelegator;
    private final List<DocIntegration> docIntegrations;
    private final DocFormat docFormat;

    /**
     * Constructor.
     *
     * @param model The source model to generate for.
     * @param docSettings Settings to customize generation.
     * @param symbolProvider The symbol provider to use to turn shapes into symbols.
     * @param fileManifest The file manifest to write to.
     * @param docIntegrations A list of integrations to apply during generation.
     */
    public DocGenerationContext(
            Model model,
            DocSettings docSettings,
            SymbolProvider symbolProvider,
            FileManifest fileManifest,
            List<DocIntegration> docIntegrations
    ) {
        this.model = model;
        this.docSettings = docSettings;
        this.fileManifest = fileManifest;
        this.docIntegrations = docIntegrations;

        DocFormat resolvedFormat = null;
        var availableFormats = new LinkedHashSet<String>();
        for (var integration : docIntegrations) {
            for (var format : integration.docFormats(docSettings)) {
                if (format.name().equals(docSettings.format())) {
                    resolvedFormat = format;
                    symbolProvider = new FileExtensionDecorator(symbolProvider, resolvedFormat.extension());
                    break;
                }
                availableFormats.add(format.name());
            }
        }
        if (resolvedFormat == null) {
            throw new CodegenException(String.format(
                    "Unknown doc format `%s`. You may be missing a dependency. Currently available formats: [%s]",
                    docSettings.format(),
                    String.join(", ", availableFormats)));
        }

        this.docFormat = resolvedFormat;
        this.symbolProvider = symbolProvider;
        this.writerDelegator = new WriterDelegator<>(fileManifest, symbolProvider, resolvedFormat.writerFactory());
    }

    @Override
    public Model model() {
        return model;
    }

    @Override
    public DocSettings settings() {
        return docSettings;
    }

    @Override
    public SymbolProvider symbolProvider() {
        return symbolProvider;
    }

    @Override
    public FileManifest fileManifest() {
        return fileManifest;
    }

    @Override
    public WriterDelegator<DocWriter> writerDelegator() {
        return writerDelegator;
    }

    @Override
    public List<DocIntegration> integrations() {
        return docIntegrations;
    }

    /**
     * @return Returns the selected format that documentation should be generated in.
     */
    public DocFormat docFormat() {
        return this.docFormat;
    }
}
