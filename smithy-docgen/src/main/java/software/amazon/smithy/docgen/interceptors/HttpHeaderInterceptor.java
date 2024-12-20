/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about header bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httpheader-trait">
 * httpHeader trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpHeaderInterceptor extends ProtocolTraitInterceptor<HttpHeaderTrait> {
    @Override
    protected Class<HttpHeaderTrait> getTraitClass() {
        return HttpHeaderTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpHeaderTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpHeaderTrait trait) {
        var target = section.context().model().expectShape(section.shape().asMemberShape().get().getTarget());
        writer.putContext("key", trait.getValue());
        writer.putContext("list", target.isListShape());
        writer.write("""
                This is bound to the HTTP header ${param:`}.${?list} Each element in \
                the list should be sent as its own header using the same key for each \
                value. The list may instead be concatenated with commas separating each \
                value.${/list}

                $L""", previousText);
    }
}
