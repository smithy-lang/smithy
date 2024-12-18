/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections.sphinx;

import java.nio.file.Path;
import java.util.Set;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains index files for sphinx.
 *
 * <p>These indexes are necessary to build up the left-side navigation bar.
 *
 * @param context The context used to generate documentation.
 * @param directory The directory the index covers.
 * @param sourceFiles The sphinx source files contained in the directory that need to
 *                    be present in generated toctrees.
 */
@SmithyUnstableApi
public record IndexSection(
        DocGenerationContext context,
        Path directory,
        Set<Path> sourceFiles) implements CodeSection {}
