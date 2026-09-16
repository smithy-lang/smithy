/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Thrown when SMF binary data is malformed or invalid.
 */
@SmithyUnstableApi
public final class SmfFormatException extends RuntimeException {
    public SmfFormatException(String message) {
        super(message);
    }
}
