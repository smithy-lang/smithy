/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds a member's <a href="https://smithy.io/2.0/spec/protocol-traits.html#timestampformat-trait">
 * timestamp format</a> to the {@link ProtocolSection} if the protocol supports it.
 */
@SmithyInternalApi
public final class TimestampFormatInterceptor extends ProtocolTraitInterceptor<TimestampFormatTrait> {
    private static final Pair<String, String> DATE_TIME_REF = Pair.of(
            "RFC3339 date-time",
            "https://datatracker.ietf.org/doc/html/rfc3339.html#section-5.6");
    private static final Pair<String, String> HTTP_DATE_REF = Pair.of(
            "RFC7231 IMF-fixdate",
            "https://tools.ietf.org/html/rfc7231.html#section-7.1.1.1");

    @Override
    protected Class<TimestampFormatTrait> getTraitClass() {
        return TimestampFormatTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return TimestampFormatTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, TimestampFormatTrait trait) {
        writer.writeInline("$B ", "TimestampFormat:");
        switch (trait.getFormat()) {
            case DATE_TIME -> writer.write("$R", DATE_TIME_REF);
            case HTTP_DATE -> writer.write("$R", HTTP_DATE_REF);
            case EPOCH_SECONDS -> writer.write("epoch seconds");
            default -> {
                return;
            }
        }
        writer.writeWithNoFormatting("\n" + previousText);
    }
}
