/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.sections;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.DocIntegration;
import software.amazon.smithy.docgen.DocSymbolProvider;
import software.amazon.smithy.docgen.interceptors.ApiKeyAuthInterceptor;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Contains the documentation for the auth schemes that the service supports.
 *
 * <p>By default, the auth schemes are documented in a definition list. The title
 * used for each auth scheme is the name that results from passing the auth trait's
 * shape to the {@link DocSymbolProvider}. The
 * name can be customized by decorating the provider with
 * {@link DocIntegration#decorateSymbolProvider}.
 *
 * <p>The body of each auth scheme's docs is treated like a typical shape section,
 * with a {@link ShapeSection}, {@link ShapeSubheadingSection},
 * {@link ShapeDetailsSection}, and documentation pulled from the shape. Details
 * based on the trait's values can be inserted via one of those sections, intercepting
 * when the shape's id matches the id of the auth trait's shape.
 *
 * @param context The context used to generate documentation.
 * @param service The service whose documentation is being generated.
 *
 * @see ShapeSection to override documentation for individual auth schemes.
 * @see ApiKeyAuthInterceptor for an
 *     example of adding details to an auth trait's docs based on its values.
 */
@SmithyUnstableApi
public record AuthSection(DocGenerationContext context, ServiceShape service) implements CodeSection {}
