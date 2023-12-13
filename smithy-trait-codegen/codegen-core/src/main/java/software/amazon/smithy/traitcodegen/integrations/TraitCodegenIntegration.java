/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations;

import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.TraitGeneratorProvider;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

public interface TraitCodegenIntegration extends SmithyIntegration<TraitCodegenSettings, TraitCodegenWriter,
        TraitCodegenContext> {
    /**
     * Use this method to override the implementation of a trait generator based on the shape or
     * base on meta-traits.
     */
    default TraitGeneratorProvider decorateGeneratorProvider(TraitCodegenContext context,
                                                             TraitGeneratorProvider provider) {
        return provider;
    }
}
