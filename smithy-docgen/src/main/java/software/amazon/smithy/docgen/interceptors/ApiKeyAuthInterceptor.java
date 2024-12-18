/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait.Location;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds additional context to the description of api key auth based on the customized values.
 */
@SmithyInternalApi
public final class ApiKeyAuthInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    private static final Pair<String, String> AUTH_HEADER_REF = Pair.of(
            "Authorization header",
            "https://datatracker.ietf.org/doc/html/rfc9110.html#section-11.4");

    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        return section.shape().getId().equals(HttpApiKeyAuthTrait.ID);
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        var service = section.context().model().expectShape(section.context().settings().service());
        var trait = service.expectTrait(HttpApiKeyAuthTrait.class);
        writer.putContext("name", trait.getName());
        writer.putContext("location", trait.getIn().equals(Location.HEADER) ? "header" : "query string");
        writer.putContext("scheme", trait.getScheme());
        writer.putContext("authHeader", AUTH_HEADER_REF);
        writer.write("""
                The API key must be bound to the ${location:L} using the key ${name:`}.${?scheme} \
                Additionally, the scheme used in the ${authHeader:R} must be ${scheme:`}.${/scheme}

                $L""", previousText);
    }
}
