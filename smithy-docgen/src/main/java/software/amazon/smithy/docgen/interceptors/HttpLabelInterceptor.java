/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information about label bindings from the
 * <a href="https://smithy.io/2.0/spec/http-bindings.html#httplabel-trait">
 * httpLabel trait</a> if the protocol supports it.
 */
@SmithyInternalApi
public final class HttpLabelInterceptor extends ProtocolTraitInterceptor<HttpLabelTrait> {
    @Override
    protected Class<HttpLabelTrait> getTraitClass() {
        return HttpLabelTrait.class;
    }

    @Override
    protected ShapeId getTraitId() {
        return HttpLabelTrait.ID;
    }

    @Override
    public boolean isIntercepted(ProtocolSection section) {
        // It's possible to use this trait somewhere where it has no meaning, but we don't
        // want to document in those cases.
        var index = OperationIndex.of(section.context().model());
        return index.isInputStructure(section.shape().getId().withoutMember()) && super.isIntercepted(section);
    }

    @Override
    void write(DocWriter writer, String previousText, ProtocolSection section, HttpLabelTrait trait) {
        var index = OperationIndex.of(section.context().model());
        writer.putContext("greedy",
                index.getInputBindings(section.shape())
                        .stream()
                        .findFirst()
                        .map(operation -> operation.expectTrait(HttpTrait.class))
                        .flatMap(httpTrait -> httpTrait.getUri().getGreedyLabel())
                        .map(segment -> segment.getContent().equals(section.shape().getId().getName()))
                        .orElse(false));
        var segment = "{" + section.shape().getId().getName() + "}";
        writer.write("""
                This is bound to the path of the URI. Its value should be URI-escaped and \
                and inserted in place of the $` segment.\
                ${?greedy}
                 When escaping this value, do not escape any backslashes ($`).
                ${/greedy}

                $L""", segment, "/", previousText);
    }
}
