/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.writers;

import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A No-Op import container.
 */
@SmithyUnstableApi
public class DocImportContainer implements ImportContainer {
    @Override
    public void importSymbol(Symbol symbol, String s) {
        // no-op
    }
}
