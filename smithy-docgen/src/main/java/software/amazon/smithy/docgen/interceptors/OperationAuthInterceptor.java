/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.List;
import software.amazon.smithy.docgen.DocgenUtils;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.synthetic.NoAuthTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a priority list of supported auth schemes for operations with optional auth or
 * operations which don't support all of a service's auth schemes.
 */
@SmithyInternalApi
public final class OperationAuthInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        if (!section.shape().isOperationShape()) {
            return false;
        }
        var index = ServiceIndex.of(section.context().model());
        var service = section.context().settings().service();

        // Only add the admonition if the service has auth in the first place.
        var serviceAuth = index.getAuthSchemes(service);
        if (serviceAuth.isEmpty()) {
            return false;
        }

        // Only add the admonition if the operations' effective auth schemes differs
        // from the total list of available auth schemes on the service.
        var operationAuth = index.getEffectiveAuthSchemes(service, section.shape(), AuthSchemeMode.NO_AUTH_AWARE);
        return !operationAuth.keySet().equals(serviceAuth.keySet());
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        writer.writeWithNoFormatting(previousText);
        writer.openAdmonition(NoticeType.IMPORTANT);

        var index = ServiceIndex.of(section.context().model());
        var service = section.context().settings().service();
        var operation = section.shape();

        var serviceAuth = DocgenUtils.getPrioritizedServiceAuth(section.context().model(), service);
        var operationAuth = List.copyOf(
                index.getEffectiveAuthSchemes(service, operation, AuthSchemeMode.MODELED).keySet());

        if (serviceAuth.equals(operationAuth)) {
            // If the total service auth and effective *modeled* operation auth are the same,
            // that means that the operation just has optional auth since isIntercepted would
            // return false otherwise. It would have been overly confusing to include this
            // case in the big text block below.
            writer.write("""
                    This operation may be optionally called without authentication.
                    """);
            writer.closeAdmonition();
            return;
        }

        var operationSchemes = operationAuth.stream()
                .map(id -> section.context().symbolProvider().toSymbol(section.context().model().expectShape(id)))
                .toList();

        writer.putContext("optional", supportsNoAuth(index, service, section.shape()));
        writer.putContext("schemes", operationSchemes);
        writer.putContext("multipleSchemes", operationSchemes.size() > 1);

        writer.write("""
                ${?schemes}This operation ${?optional}may optionally${/optional}${^optional}MUST${/optional} \
                be called with ${?multipleSchemes}one of the following priority-ordered auth schemes${/multipleSchemes}\
                ${^multipleSchemes}the following auth scheme${/multipleSchemes}: \
                ${#schemes}${value:R}${^key.last}, ${/key.last}${/schemes}.${/schemes}\
                ${^schemes}${?optional}This operation must be called without authentication.${/optional}${/schemes}
                """);
        writer.closeAdmonition();
    }

    private boolean supportsNoAuth(ServiceIndex index, ToShapeId service, ToShapeId operation) {
        return index.getEffectiveAuthSchemes(service, operation, AuthSchemeMode.NO_AUTH_AWARE)
                .containsKey(NoAuthTrait.ID);
    }
}
