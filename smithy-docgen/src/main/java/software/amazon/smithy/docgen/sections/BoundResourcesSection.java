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
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for all (sub)resources bound to a service or resource.
 *
 * <p>Service documentation will include all resources transitively bound to the
 * service, while resource documentation will only include sub-resources directly
 * bound to the resource.
 *
 * @param context The context used to generate documentation.
 * @param container The service or resource the (sub)resources are bound to.
 * @param resources The (sub)resources being listed.
 *
 * @see BoundResourceSection For the section containing individual (sub)resources bound
 * to the service or resource.
 * @see ServiceGenerator for information
 * about other sections present on the service's documentation page.
 * @see ResourceGenerator for information
 * about other sections present on the documentation pages for resrouces.
 */
@SmithyUnstableApi
public record BoundResourcesSection(
        DocGenerationContext context,
        EntityShape container,
        List<ResourceShape> resources) implements CodeSection {}
