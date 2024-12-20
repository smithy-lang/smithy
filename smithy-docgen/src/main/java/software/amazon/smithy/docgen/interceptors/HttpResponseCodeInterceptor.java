/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about response code bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httpresponsecode-trait">
 * httpResponseCode trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpResponseCodeInterceptor extends ProtocolTraitInterceptor<HttpResponseCodeTrait> {
    @Override
    protected Class<HttpResponseCodeTrait> getTraitClass() {
        return HttpResponseCodeTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpResponseCodeTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpResponseCodeTrait trait) {
        writer.write("""
                This value represents the HTTP response code for the operation invocation.

                $L""", previousText);
    }
}
