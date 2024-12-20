/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about query bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httpquery-trait">
 * httpQuery trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpQueryInterceptor extends ProtocolTraitInterceptor<HttpQueryTrait> {
    @Override
    protected Class<HttpQueryTrait> getTraitClass() {
        return HttpQueryTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpQueryTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpQueryTrait trait) {
        var target = section.context().model().expectShape(section.shape().asMemberShape().get().getTarget());
        writer.putContext("param", trait.getValue());
        writer.putContext("list", target.isListShape());
        writer.write("""
                This is bound to the HTTP query parameter ${param:`}.${?list} Each element in \
                the list is represented by its own key-value pair, each instance using the \
                same key.${/list}

                $L""", previousText);
    }
}
