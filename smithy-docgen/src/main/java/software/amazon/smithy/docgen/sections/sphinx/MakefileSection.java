/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections.sphinx;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Generates a Makefile that wraps sphinx-build with default arguments.
 * @param context The context used to generate documentation.
 */
@SmithyUnstableApi
public record MakefileSection(DocGenerationContext context) implements CodeSection {}
