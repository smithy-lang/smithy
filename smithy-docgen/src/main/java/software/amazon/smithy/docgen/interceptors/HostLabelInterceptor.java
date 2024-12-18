/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Documents usage for members targeted with the
 * <a href="https://smithy.io/2.0/spec/endpoint-traits.html#hostlabel-trait">
 * hostLabel</a> trait if the protocol supports it..
 */
@SmithyInternalApi
public final class HostLabelInterceptor extends ProtocolTraitInterceptor<HostLabelTrait> {
    @Override
    protected Class<HostLabelTrait> getTraitClass() {
        return HostLabelTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HostLabelTrait.ID;
    }

    @Override
    public boolean isIntercepted(ProtocolSection section) {
        // It's possible to use this trait somewhere where it has no meaning, but we don't
        // want to document in those cases.
        var index = OperationIndex.of(section.context().model());
        return index.isInputStructure(section.shape().getId().withoutMember()) && super.isIntercepted(section);
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HostLabelTrait trait) {
        var segment = "{" + section.shape().getId().getName() + "}";
        writer.write("""
                This is additionally bound to the host prefix. Its value should be URI-escaped and \
                and inserted in place of the $` segment. It must also be serialized to its normal \
                binding location.

                $L""", segment, previousText);
    }
}
