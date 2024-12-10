/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.error;

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Represents an error encountered when parsing a rule-set expression.
 */
@SmithyUnstableApi
public final class InnerParseError extends RuntimeException {
    public InnerParseError(String message) {
        super(message);
    }
}
