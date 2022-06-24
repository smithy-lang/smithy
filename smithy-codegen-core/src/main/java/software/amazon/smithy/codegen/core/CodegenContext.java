/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.codegen.core;

import java.util.List;
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
