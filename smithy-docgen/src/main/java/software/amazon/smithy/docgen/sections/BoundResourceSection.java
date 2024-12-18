/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.generators.ResourceGenerator;
import software.amazon.smithy.docgen.generators.ServiceGenerator;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for individual (sub)resources bound to a service or
 * resource.
 *
 * <p>Service documentation will include all resources transitively bound to the
 * service, while resource documentation will only include sub-resources directly
 * bound to the resource.
 *
 * @param context The context used to generate documentation.
 * @param container The service or resource the (sub)resource is bound to.
 * @param resource The (sub)resource being listed.
 *
 * @see BoundResourcesSection For the section containing all (sub)resources bound to
 * the service or resource.
 * @see ServiceGenerator for information
 * about other sections present on the service's documentation page.
 * @see ResourceGenerator for information
 * about other sections present on the documentation pages for resrouces.
 */
@SmithyUnstableApi
public record BoundResourceSection(
        DocGenerationContext context,
        EntityShape container,
        ResourceShape resource) implements CodeSection {}
