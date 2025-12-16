/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.model.Model;

/**
 * A context object that can be used during code generation and is used by
 * {@link SmithyIntegration}.
 *
 * @param <S> The settings object used to configure the generator.
 * @param <W> The type of {@link SymbolWriter} used by the generator.
 * @param <I> The type of {@link SmithyIntegration}s used by the generator.
 */
public interface CodegenContext<S, W extends SymbolWriter<W, ?>, I extends SmithyIntegration<S, W, ?>> {
    /**
     * @return Gets the model being code generated.
     */
    Model model();

    /**
     * @return Gets code generation settings.
     */
    S settings();

    /**
     * @return Gets the SymbolProvider used for code generation.
     */
    SymbolProvider symbolProvider();

    /**
     * @return Gets the FileManifest being written to for code generation.
     */
    FileManifest fileManifest();

    /**
     * Gets the FileManifest used to create files in the projection's shared file
     * space.
     *
     * <p>All files written by a generator should either be written using this
     * manifest or the generator's isolated manifest ({@link #fileManifest()}).
     *
     * <p>Files written to this manifest may be read or modified by other Smithy build
     * plugins. Generators SHOULD NOT write files to this manifest unless they
     * specifically intend for them to be consumed by other plugins. Files that are not
     * intended to be shared should be written to the manifest from
     * {@link #fileManifest()}.
     *
     * @return Gets the optional FileManifest used to create files in the projection's
     *         shared file space.
     */
    default Optional<FileManifest> sharedFileManifest() {
        return Optional.empty();
    }

    /**
     * Get the WriterDelegator used for generating code.
     *
     * <p>Generates might need other delegators for specific purposes, and it's fine to
     * add more methods for those specific purposes. If an implementation uses a specific
     * subclass of a WriterDelegator, implementations can override this method to return
     * a more specific WriterDelegator type.
     *
     * @return Returns the writer delegator used by the generator.
     */
    WriterDelegator<W> writerDelegator();

    /**
     * @return Gets the SmithyIntegrations used for code generation.
     */
    List<I> integrations();
}
