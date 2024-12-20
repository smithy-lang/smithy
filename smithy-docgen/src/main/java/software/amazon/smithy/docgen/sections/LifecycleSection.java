/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.ResourceGenerator;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for all resource lifecycle operations.
 *
 * @param context The context used to generate documentation.
 * @param resource The resource whose lifecycle operations are being documented.
 *
 * @see LifecycleSection For the section containing all the resource lifecycle
 * operations.
 * @see BoundOperationSection For individual operations bound to the resource's
 * {@code operations} or {@code collectionOperations} properties.
 * @see BoundOperationsSection For all operations bound to the resource's
 * {@code operations} or {@code collectionOperations} properties.
 * @see ResourceGenerator for information
 * about other sections present on the documentation pages for resrouces.
 */
@SmithyUnstableApi
public record LifecycleSection(DocGenerationContext context, ResourceShape resource) implements CodeSection {}
