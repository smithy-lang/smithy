/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about payload bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httppayload-trait">
 * httpPayload trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpPayloadInterceptor extends ProtocolTraitInterceptor<HttpPayloadTrait> {
    @Override
    protected Class<HttpPayloadTrait> getTraitClass() {
        return HttpPayloadTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpPayloadTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpPayloadTrait trait) {
        var target = section.context().model().expectShape(section.shape().asMemberShape().get().getTarget());
        writer.pushState();
        writer.putContext("requiresLength", target.hasTrait(RequiresLengthTrait.class));
        writer.write("""
                This is bound directly to the HTTP message body without wrapping.${?requiresLength} \
                Its size must be sent as the value of the $` header.${/requiresLength}

                $L""", "Content-Length", previousText);
        writer.popState();
    }
}
