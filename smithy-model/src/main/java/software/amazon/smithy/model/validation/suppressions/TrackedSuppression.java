/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A {@link Suppression} that tracks whether it suppressed at least one validation event.
 *
 * <p>Validation events are decorated concurrently, so the match state is tracked atomically.
 */
final class TrackedSuppression implements Suppression {

    private final Suppression suppression;
    private final AtomicBoolean matched = new AtomicBoolean();

    TrackedSuppression(Suppression suppression) {
        this.suppression = suppression;
    }

    @Override
    public boolean test(ValidationEvent event) {
        if (suppression.test(event)) {
            matched.set(true);
            return true;
        }

        return false;
    }

    @Override
    public Optional<String> getReason() {
        return suppression.getReason();
    }

    boolean hasMatched() {
        return matched.get();
    }

    Suppression getSuppression() {
        return suppression;
    }
}
