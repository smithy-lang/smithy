/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about header bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httpprefixheaders-trait">
 * httpPrefixHeaders trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpPrefixHeadersInterceptor extends ProtocolTraitInterceptor<HttpPrefixHeadersTrait> {
    @Override
    protected Class<HttpPrefixHeadersTrait> getTraitClass() {
        return HttpPrefixHeadersTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpPrefixHeadersTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpPrefixHeadersTrait trait) {
        writer.putContext("prefix", trait.getValue());
        writer.write("""
                Each pair in this map represents an HTTP header${?prefix} with the prefix \
                ${prefix:`}${/prefix}.

                $L""", previousText);
    }
}
