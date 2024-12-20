/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ShapeSubheadingSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * This adds badges and notices to errors that are retryable.
 */
@SmithyInternalApi
public final class RetryableInterceptor implements CodeInterceptor.Prepender<ShapeSubheadingSection, DocWriter> {
    @Override
    public Class<ShapeSubheadingSection> sectionType() {
        return ShapeSubheadingSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeSubheadingSection section) {
        return section.shape().hasTrait(RetryableTrait.class);
    }

    @Override
    public void prepend(DocWriter writer, ShapeSubheadingSection section) {
        writer.writeBadge(NoticeType.IMPORTANT, "RETRYABLE").write("\n");
        if (section.shape().expectTrait(RetryableTrait.class).getThrottling()) {
            writer.openAdmonition(NoticeType.NOTE);
            writer.write("""
                    This is a throttling error. Request retries in response to this error should use exponential
                    backoff with jitter. Clients should do this automatically.
                    """);
            writer.closeAdmonition();
        }
    }
}
