/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.core.integrations;

import java.util.List;
import software.amazon.smithy.docgen.core.DocFormat;
import software.amazon.smithy.docgen.core.DocGenerationContext;
import software.amazon.smithy.docgen.core.DocIntegration;
import software.amazon.smithy.docgen.core.DocSettings;
import software.amazon.smithy.docgen.core.interceptors.ApiKeyAuthInterceptor;
import software.amazon.smithy.docgen.core.interceptors.DefaultValueInterceptor;
import software.amazon.smithy.docgen.core.interceptors.DeprecatedInterceptor;
import software.amazon.smithy.docgen.core.interceptors.EndpointInterceptor;
import software.amazon.smithy.docgen.core.interceptors.ErrorFaultInterceptor;
import software.amazon.smithy.docgen.core.interceptors.ExternalDocsInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HostLabelInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpChecksumRequiredInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpErrorInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpHeaderInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpLabelInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpPayloadInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpPrefixHeadersInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpQueryInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpQueryParamsInterceptor;
import software.amazon.smithy.docgen.core.interceptors.HttpResponseCodeInterceptor;
import software.amazon.smithy.docgen.core.interceptors.IdempotencyInterceptor;
import software.amazon.smithy.docgen.core.interceptors.InternalInterceptor;
import software.amazon.smithy.docgen.core.interceptors.JsonNameInterceptor;
import software.amazon.smithy.docgen.core.interceptors.LengthInterceptor;
import software.amazon.smithy.docgen.core.interceptors.MediaTypeInterceptor;
import software.amazon.smithy.docgen.core.interceptors.NoReplaceBindingInterceptor;
import software.amazon.smithy.docgen.core.interceptors.NoReplaceOperationInterceptor;
import software.amazon.smithy.docgen.core.interceptors.NullabilityInterceptor;
import software.amazon.smithy.docgen.core.interceptors.OperationAuthInterceptor;
import software.amazon.smithy.docgen.core.interceptors.PaginationInterceptor;
import software.amazon.smithy.docgen.core.interceptors.PatternInterceptor;
import software.amazon.smithy.docgen.core.interceptors.RangeInterceptor;
import software.amazon.smithy.docgen.core.interceptors.RecommendedInterceptor;
import software.amazon.smithy.docgen.core.interceptors.ReferencesInterceptor;
import software.amazon.smithy.docgen.core.interceptors.RequestCompressionInterceptor;
import software.amazon.smithy.docgen.core.interceptors.RetryableInterceptor;
import software.amazon.smithy.docgen.core.interceptors.SensitiveInterceptor;
import software.amazon.smithy.docgen.core.interceptors.SinceInterceptor;
import software.amazon.smithy.docgen.core.interceptors.SparseInterceptor;
import software.amazon.smithy.docgen.core.interceptors.StreamingInterceptor;
import software.amazon.smithy.docgen.core.interceptors.TimestampFormatInterceptor;
import software.amazon.smithy.docgen.core.interceptors.UniqueItemsInterceptor;
import software.amazon.smithy.docgen.core.interceptors.UnstableInterceptor;
import software.amazon.smithy.docgen.core.interceptors.XmlAttributeInterceptor;
import software.amazon.smithy.docgen.core.interceptors.XmlFlattenedInterceptor;
import software.amazon.smithy.docgen.core.interceptors.XmlNameInterceptor;
import software.amazon.smithy.docgen.core.interceptors.XmlNamespaceInterceptor;
import software.amazon.smithy.docgen.core.writers.DocWriter;
import software.amazon.smithy.docgen.core.writers.MarkdownWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies the built-in {@link DocFormat}s and base {@code CodeSection}s.
 *
 * <p>This integration runs in high priority to ensure that other integrations can see
 * and react to changes it makes. To have an integration reliably run
 * before this, override {@link DocIntegration#runBefore} with the output of
 * {@link BuiltinsIntegration#name} in the list. Similarly, to guarantee an integration
 * is run after this, override {@link DocIntegration#runAfter} with the same argument.
 */
@SmithyInternalApi
public class BuiltinsIntegration implements DocIntegration {

    @Override
    public byte priority() {
        // Add the builtins at a highest priority so that they almost always are run
        // first. Using runBefore it is still possible to ensure an integration is run
        // before this.
        return 127;
    }

    @Override
    public List<DocFormat> docFormats(DocSettings settings) {
        return List.of(
            new DocFormat("markdown", ".md", new MarkdownWriter.Factory())
        );
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, DocWriter>> interceptors(
        DocGenerationContext context
    ) {
        // Due to the way that interceptors work, the elements at the bottom of the list will
        // be called last. Since most of these append data to their sections, that means that
        // the ones at the end will be at the top of the rendered pages. Therefore, interceptors
        // that provide more critical information should appear at the bottom of this list.
        return List.of(
            new StreamingInterceptor(),
            new ReferencesInterceptor(),
            new MediaTypeInterceptor(),
            new OperationAuthInterceptor(),
            new ApiKeyAuthInterceptor(),
            new TimestampFormatInterceptor(),
            new JsonNameInterceptor(),
            new XmlNamespaceInterceptor(),
            new XmlAttributeInterceptor(),
            new XmlNameInterceptor(),
            new XmlFlattenedInterceptor(),
            new HttpChecksumRequiredInterceptor(),
            new HttpResponseCodeInterceptor(),
            new HttpPayloadInterceptor(),
            new HttpErrorInterceptor(),
            new HttpHeaderInterceptor(),
            new HttpPrefixHeadersInterceptor(),
            new HttpQueryParamsInterceptor(),
            new HttpQueryInterceptor(),
            new HostLabelInterceptor(),
            new EndpointInterceptor(),
            new HttpLabelInterceptor(),
            new HttpInterceptor(),
            new PaginationInterceptor(),
            new RequestCompressionInterceptor(),
            new NoReplaceBindingInterceptor(),
            new NoReplaceOperationInterceptor(),
            new SparseInterceptor(),
            new UniqueItemsInterceptor(),
            new PatternInterceptor(),
            new RangeInterceptor(),
            new LengthInterceptor(),
            new ExternalDocsInterceptor(),
            new IdempotencyInterceptor(),
            new ErrorFaultInterceptor(),
            new RetryableInterceptor(),
            new DefaultValueInterceptor(),
            new SinceInterceptor(),
            new InternalInterceptor(),
            new UnstableInterceptor(),
            new DeprecatedInterceptor(),
            new RecommendedInterceptor(),
            new NullabilityInterceptor(),
            new SensitiveInterceptor()
        );
    }
}
