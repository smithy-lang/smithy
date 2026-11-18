/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import java.util.List;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A {@link ValidationEventDecorator} that tracks the suppressions it applies and can emit
 * {@link Severity#WARNING} events for suppressions that matched no validation events.
 *
 * <p>Suppressions that never match any validation events typically indicate that the model changed
 * such that the suppression is no longer needed, or that the suppression was misspelled. Such
 * suppressions are silently ignored today, which allows them to accumulate in models forever.
 *
 * <p>The events created by {@link #createNoOpSuppressionWarnings()} are intentionally never
 * decorated by the suppression and severity override pipeline. A no-op suppression by definition
 * matches no events, so allowing suppressions to silence the warning about themselves would let a
 * stale suppression permanently hide the very warning that reports it.
 */
@SmithyUnstableApi
public interface SuppressionUsage extends ValidationEventDecorator {

    /**
     * Creates a {@link Severity#WARNING} validation event for each suppression that was applied to
     * zero validation events while decorating.
     *
     * <p>This method is intended to be called after all validation events of a validation run have
     * been decorated. Suppressions that did match at least one event never generate a warning.
     *
     * @return Returns a warning event for each suppression that matched no validation events.
     */
    List<ValidationEvent> createNoOpSuppressionWarnings();
}
