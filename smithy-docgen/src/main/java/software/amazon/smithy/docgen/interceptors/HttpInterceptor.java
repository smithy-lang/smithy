/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.pattern.SmithyPattern.Segment;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information to operations from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#http-trait">
 * http trait</a>.
 */
@SmithyInternalApi
public final class HttpInterceptor extends ProtocolTraitInterceptor<HttpTrait> {
    @Override
    protected Class<HttpTrait> getTraitClass() {
        return HttpTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpTrait trait) {
        writer.putContext("hasLabels", !trait.getUri().getLabels().isEmpty());
        writer.putContext("greedyLabel",
                trait.getUri().getGreedyLabel().map(Segment::getContent));
        writer.write("""
                $B $`

                $B $`
                ${?hasLabels}

                To resolve the path segment of the URI, replace any segments surrounded with
                braces with the URI-escaped value of the corresponding member.${?greedyLabel} \
                When escaping the value of the ${greedyLabel:`} segment, do not escape any \
                backslashes ($`).${/greedyLabel}
                ${/hasLabels}

                $L""", "HTTP Method:", trait.getMethod(), "URI:", trait.getUri(), "/", previousText);
    }
}
