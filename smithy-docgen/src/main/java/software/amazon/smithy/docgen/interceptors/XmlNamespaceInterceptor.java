/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Notes that a member needs an
 * <a href="https://smithy.io/2.0/spec/protocol-traits.html#xmlnamespace-trait">
 * xml namespace</a> in the {@link ProtocolSection} if the protocol supports it.
 */
@SmithyInternalApi
public final class XmlNamespaceInterceptor extends ProtocolTraitInterceptor<XmlNamespaceTrait> {
    @Override
    protected Class<XmlNamespaceTrait> getTraitClass() {
        return XmlNamespaceTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return XmlNamespaceTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, XmlNamespaceTrait trait) {
        writer.writeWithNoFormatting(previousText + "\n");
        var namespace = "xmlns";
        if (trait.getPrefix().isPresent()) {
            namespace += ":" + trait.getPrefix().get();
        }
        namespace += "=\"" + trait.getUri() + "\"";
        writer.openAdmonition(NoticeType.IMPORTANT);
        writer.write("""
                This tag must contain the following XML namespace $`
                """, namespace);
        writer.closeAdmonition();
    }
}
