/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.Set;
import software.amazon.smithy.utils.SetUtils;

/**
 * Protocol utilities for OpenAPI protocol support.
 */
final class ProtocolUtils {
    // If a request returns / accepts a body then it should allow these un-modeled headers.
    static final Set<String> CONTENT_HEADERS = SetUtils.of("Content-Length", "Content-Type");

    private ProtocolUtils() {}
}
