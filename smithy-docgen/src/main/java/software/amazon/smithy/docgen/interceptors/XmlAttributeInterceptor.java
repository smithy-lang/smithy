/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

// TODO: this needs to get added to operation input / output shapes too.
// It isn't right now because those shapes don't have their own doc pages.
/**
 * Notes that a member is an
 * <a href="https://smithy.io/2.0/spec/protocol-traits.html#xmlattribute-trait">
 * xml attribute</a> in the {@link ProtocolSection} if the protocol supports it.
 */
@SmithyInternalApi
public final class XmlAttributeInterceptor extends ProtocolTraitInterceptor<XmlAttributeTrait> {
    @Override
    protected Class<XmlAttributeTrait> getTraitClass() {
        return XmlAttributeTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return XmlAttributeTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, XmlAttributeTrait trait) {
        writer.writeWithNoFormatting(previousText + "\n");
        writer.openAdmonition(NoticeType.IMPORTANT);
        writer.write("This member represents an XML attribute rather than a nested tag.");
        writer.closeAdmonition();
    }
}
