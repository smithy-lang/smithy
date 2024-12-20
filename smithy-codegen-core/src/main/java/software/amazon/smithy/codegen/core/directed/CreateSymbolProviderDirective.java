/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to create a {@link SymbolProvider}.
 *
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#createSymbolProvider
 */
public final class CreateSymbolProviderDirective<S> extends Directive<S> {
    CreateSymbolProviderDirective(Model model, S settings, ServiceShape service) {
        super(model, settings, service);
    }
}
