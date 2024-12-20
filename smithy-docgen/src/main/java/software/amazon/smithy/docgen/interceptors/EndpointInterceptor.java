/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds endpoint prefix information to operations based on the
 * <a href="https://smithy.io/2.0/spec/endpoint-traits.html#endpoint-trait">
 * endpoint</a> trait if the protocol supports it.
 */
@SmithyInternalApi
public final class EndpointInterceptor extends ProtocolTraitInterceptor<EndpointTrait> {
    @Override
    protected Class<EndpointTrait> getTraitClass() {
        return EndpointTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return EndpointTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, EndpointTrait trait) {
        writer.putContext("hasLabels", !trait.getHostPrefix().getLabels().isEmpty());
        writer.write("""
                $B $`
                ${?hasLabels}

                To resolve the endpoint prefix, replace any portions surrounded with braces with the \
                URI-escaped value of the corresponding member.
                ${/hasLabels}

                $L""", "Host prefix:", trait.getHostPrefix(), previousText);
    }
}
