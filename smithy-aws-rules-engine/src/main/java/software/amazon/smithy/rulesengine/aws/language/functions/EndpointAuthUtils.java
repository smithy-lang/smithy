/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.List;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.utils.MapUtils;

public final class EndpointAuthUtils {
    private static final String SIGV_4 = "sigv4";
    private static final String SIG_V4A = "sigv4a";
    private static final String SIGNING_NAME = "signingName";
    private static final String SIGNING_REGION = "signingRegion";
    private static final String SIGNING_REGION_SET = "signingRegionSet";

    private EndpointAuthUtils() {}

    public static Endpoint.Builder sigv4(Endpoint.Builder builder, Literal signingRegion, Literal signingService) {
        return builder.addAuthScheme(SIGV_4, MapUtils.of(SIGNING_REGION, signingRegion, SIGNING_NAME, signingService));
    }

    public static Endpoint.Builder sigv4a(
            Endpoint.Builder builder,
            List<Literal> signingRegionSet,
            Literal signingService
    ) {
        return builder.addAuthScheme(SIG_V4A, MapUtils.of(SIGNING_REGION_SET, Literal.tupleLiteral(signingRegionSet),
                SIGNING_NAME, signingService));
    }
}
