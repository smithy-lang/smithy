/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.docgen.core.sections;

import software.amazon.smithy.docgen.core.DocGenerationContext;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for individual operations bound to a service or resource.
 *
 * <p>Service documentation will include all operations transitively bound to the
 * service, while resource documentation will only include operations directly
 * bound to the resource through {@code operations} or {@code collectionOperations}.
 *
 * @param context The context used to generate documentation.
 * @param container The service or resource the operation is bound to.
 * @param operation The operation being listed.
 *
 * @see BoundOperationsSection For the section containing all operations bound to the
 * service or resource.
 * @see LifecycleOperationSection For operations bound to a resource's lifecycle
 * operations.
 * @see software.amazon.smithy.docgen.core.generators.ServiceGenerator for information
 * about other sections present on the service's documentation page.
 * @see software.amazon.smithy.docgen.core.generators.ResourceGenerator for information
 * about other sections present on the documentation pages for resrouces.
 */
@SmithyUnstableApi
public record BoundOperationSection(
        DocGenerationContext context,
        EntityShape container,
        OperationShape operation
) implements CodeSection {
}
