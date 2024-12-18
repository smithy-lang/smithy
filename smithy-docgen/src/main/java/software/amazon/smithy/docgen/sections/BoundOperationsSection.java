/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import java.util.List;
import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.ResourceGenerator;
import software.amazon.smithy.docgen.generators.ServiceGenerator;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for all operations bound to a service or resource.
 *
 * <p>Service documentation will include all operations transitively bound to the
 * service, while resource documentation will only include operations directly
 * bound to the resource through {@code operations} or {@code collectionOperations}.
 *
 * @param context The context used to generate documentation.
 * @param container The service or resource the operations are bound to.
 * @param operations The operations being listed.
 *
 * @see BoundOperationSection For a section that only contains individual operations.
 * @see LifecycleOperationSection For operations bound to a resource's lifecycle
 * operations.
 * @see ServiceGenerator for information
 * about other sections present on the service's documentation page.
 * @see ResourceGenerator for information
 * about other sections present on the documentation pages for resrouces.
 */
@SmithyUnstableApi
public record BoundOperationsSection(
        DocGenerationContext context,
        EntityShape container,
        List<OperationShape> operations) implements CodeSection {}
