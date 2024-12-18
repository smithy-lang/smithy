/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about query bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httpquery-trait">
 * httpQuery trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpChecksumRequiredInterceptor extends ProtocolTraitInterceptor<HttpChecksumRequiredTrait> {
    private static final Pair<String, String> CONTENT_MD5 = Pair.of(
            "Content-MD5",
            "https://datatracker.ietf.org/doc/html/rfc1864.html");

    @Override
    protected Class<HttpChecksumRequiredTrait> getTraitClass() {
        return HttpChecksumRequiredTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpChecksumRequiredTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpChecksumRequiredTrait trait) {
        writer.writeWithNoFormatting(previousText + "\n");
        writer.openAdmonition(NoticeType.IMPORTANT);
        writer.write("This operation REQUIRES a checksum, such as $R.", CONTENT_MD5);
        writer.closeAdmonition();
    }
}
