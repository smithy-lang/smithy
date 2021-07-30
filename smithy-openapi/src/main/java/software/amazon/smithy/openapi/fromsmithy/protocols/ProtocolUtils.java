/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
