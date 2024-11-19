/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.docgen.core.interceptors;

import software.amazon.smithy.docgen.core.sections.ProtocolSection;
import software.amazon.smithy.docgen.core.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpErrorTrait;

/**
 * Adds the http response code for errors from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httperror-trait">
 * httpError trait</a> if the protocol supports it.
 */
public final class HttpErrorInterceptor extends ProtocolTraitInterceptor<HttpErrorTrait> {
    @Override
    protected Class<HttpErrorTrait> getTraitClass() {
        return HttpErrorTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpErrorTrait.ID;
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpErrorTrait trait) {
        writer.putContext("code", trait.getCode());
        writer.write("""
                $B ${code:`}

                $L""", "HTTP Error Code:", previousText);
    }
}
