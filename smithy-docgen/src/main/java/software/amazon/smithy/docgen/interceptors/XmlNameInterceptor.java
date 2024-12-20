/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a member's <a href="https://smithy.io/2.0/spec/protocol-traits.html#xmlname-trait">
 * xmlName</a> to the {@link ProtocolSection} if the protocol supports it.
 */
@SmithyInternalApi
public final class XmlNameInterceptor extends ProtocolTraitInterceptor<XmlNameTrait> {
    @Override
    protected Class<XmlNameTrait> getTraitClass() {
        return XmlNameTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return XmlNameTrait.ID;
    }

    @Override
    public boolean isIntercepted(ProtocolSection section) {
        // The xmlName trait uniquely doesn't inherit values from the target as a member.
        return super.isIntercepted(section) && section.shape().hasTrait(XmlNameTrait.class);
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, XmlNameTrait trait) {
        writer.putContext("xmlTagName", "XML tag name:");
        writer.write("""
                ${xmlTagName:B} $`

                $L""", trait.getValue(), previousText);
    }
}
