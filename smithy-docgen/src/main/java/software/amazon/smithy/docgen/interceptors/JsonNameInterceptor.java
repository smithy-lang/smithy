/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a member's <a href="https://smithy.io/2.0/spec/protocol-traits.html#jsonname-trait">
 * jsonName</a> to the {@link ProtocolSection} if the protocol supports it.
 */
@SmithyInternalApi
public final class JsonNameInterceptor extends ProtocolTraitInterceptor<JsonNameTrait> {

    @Override
    protected Class<JsonNameTrait> getTraitClass() {
        return JsonNameTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return JsonNameTrait.ID;
    }

    @Override
    public void write(DocWriter writer, String previousText, ProtocolSection section, JsonNameTrait trait) {
        writer.putContext("jsonKeyName", "JSON key name:");
        writer.write("""
                ${jsonKeyName:B} $`

                $L""", trait.getValue(), previousText);
    }
}
