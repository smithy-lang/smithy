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

import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.model.Model;

/**
 * A context object that can be used during code generation and is used by
 * {@link SmithyIntegration}.
 *
 * @param <S> The settings object used to configure the generator.
 */
public interface CodegenContext<S> {
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
}
