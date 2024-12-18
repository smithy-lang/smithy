/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A record containing information about a doc format.
 *
 * <p>Use {@link DocIntegration#docFormats} to make new formats available.
 *
 * @param name The name of the format. This will be the string that will be set as the
 *             value of {@code format} in {@link DocSettings}.
 * @param extension The file extension to use by default for documentation files. This
 *                  will be set on the {@code Symbol}s automatically by
 *                  {@link DocSymbolProvider.FileExtensionDecorator}.
 * @param writerFactory A factory method for creating writers that write in this
 *                      format.
 */
@SmithyUnstableApi
public record DocFormat(String name, String extension, SymbolWriter.Factory<DocWriter> writerFactory) {}
