/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about query bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httpqueryparams-trait">
 * httpQueryParams trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpQueryParamsInterceptor extends ProtocolTraitInterceptor<HttpQueryParamsTrait> {
    @Override
    protected Class<HttpQueryParamsTrait> getTraitClass() {
        return HttpQueryParamsTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpQueryParamsTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpQueryParamsTrait trait) {
        var memberTarget = section.shape().asMemberShape().get().getTarget();
        var map = section.context().model().expectShape(memberTarget, MapShape.class);
        var valueTarget = section.context().model().expectShape(map.getValue().getTarget());
        writer.putContext("list", valueTarget.isListShape());
        writer.write("""
                Each pair in this map represents an HTTP query parameter.${?list} Each element in \
                the value lists is represented by its own key-value pair, each instance using the \
                same key.${/list}

                $L""", previousText);
    }
}
