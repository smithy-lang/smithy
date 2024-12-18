/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections.sphinx;

import java.util.Set;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Generates the {@code conf.py} file for sphinx.
 * @see <a href="https://www.sphinx-doc.org/en/master/usage/configuration.html">
 *     sphinx config docs</a>
 * @param context The context used to generate documentation.
 * @param extensions Extensions needed to generate documentation.
 */
@SmithyUnstableApi
public record ConfSection(DocGenerationContext context, Set<String> extensions) implements CodeSection {}
