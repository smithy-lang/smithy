/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.knowledge.PaginatedIndex;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * This adds pagination information to operation docs.
 */
@SmithyInternalApi
public final class PaginationInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        return section.shape().isOperationShape() && section.shape().hasTrait(PaginatedTrait.class);
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        var paginatedIndex = PaginatedIndex.of(section.context().model());
        var service = section.context().settings().service();
        var paginationInfo = paginatedIndex.getPaginationInfo(service, section.shape()).get();
        var symbolProvider = section.context().symbolProvider();
        writer.putContext("size", paginationInfo.getPageSizeMember().map(symbolProvider::toSymbol));
        writer.putContext("inputToken",
                SymbolReference.builder()
                        .symbol(symbolProvider.toSymbol(paginationInfo.getInputTokenMember()))
                        .alias("input token")
                        .build());

        var outputTokenPath = paginationInfo.getOutputTokenMemberPath();
        var outputToken = outputTokenPath.get(outputTokenPath.size() - 1);
        writer.putContext("outputToken",
                SymbolReference.builder()
                        .symbol(symbolProvider.toSymbol(outputToken))
                        .alias("output token")
                        .build());

        writer.openAdmonition(NoticeType.IMPORTANT);
        writer.write("""
                This operation returns partial results in pages${?size}, whose maximum size may be
                configured with ${size:R}${/size}. Each request may return an ${outputToken:R} that \
                may be used as an ${inputToken:R} in subsequent requests to fetch the next page of results. \
                If the operation does not return an ${outputToken:R}, that means that there are \
                no more results. If the operation returns a repeated ${outputToken:R}, there MAY be \
                more results later.""");
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}
