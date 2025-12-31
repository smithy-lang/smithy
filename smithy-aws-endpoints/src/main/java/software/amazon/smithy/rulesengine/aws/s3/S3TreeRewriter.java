/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.s3;

import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Rewrites S3 endpoint rules to create a dramatically smaller and more efficient BDD.
 *
 * <p>This is a BDD pre-processing transform that makes the decision tree larger but enables dramatically better
 * BDD compilation. It solves the "SSA Trap" problem where semantically identical operations appear as syntactically
 * different expressions.
 *
 * <p>This class composes three separate transforms:
 * <ol>
 *   <li>{@link S3AzCanonicalizerTransform} - Canonicalizes AZ extraction:
 *       {@code substring(Bucket, N, M)} → {@code split(Bucket, "--")[1]}</li>
 *   <li>{@link S3RegionUnifierTransform} - Unifies region references:
 *       {@code Region}/{@code bucketArn#region} → {@code _signing_region}/{@code _effective_region}</li>
 *   <li>{@link S3ExpressEndpointTransform} - Canonicalizes S3Express endpoints:
 *       FIPS/DualStack URL variants → ITE-computed segments</li>
 * </ol>
 *
 * <p>Each transform is independent and can be applied separately if needed.
 */
@SmithyInternalApi
public final class S3TreeRewriter {
    private static final Logger LOGGER = Logger.getLogger(S3TreeRewriter.class.getName());

    private S3TreeRewriter() {}

    /**
     * Transforms the given endpoint rule set by applying all S3 canonicalization transforms.
     *
     * @param ruleSet Rules to transform.
     * @return the transformed rule set.
     */
    public static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        // Pass 1: Canonicalize AZ extraction
        S3AzCanonicalizerTransform azTransform = S3AzCanonicalizerTransform.create();
        EndpointRuleSet afterAz = azTransform.endpointRuleSet(ruleSet);

        // Pass 2: Unify region references
        S3RegionUnifierTransform regionTransform = S3RegionUnifierTransform.create();
        EndpointRuleSet afterRegion = regionTransform.endpointRuleSet(afterAz);

        // Pass 3: Canonicalize S3Express endpoints
        S3ExpressEndpointTransform s3ExpressTransform = S3ExpressEndpointTransform.create();
        EndpointRuleSet result = s3ExpressTransform.endpointRuleSet(afterRegion);

        LOGGER.info(() -> String.format(
                "S3 tree rewriter: %d AZ, %d region, %d/%d S3Express rewrites",
                azTransform.getRewriteCount(),
                regionTransform.getRewriteCount(),
                s3ExpressTransform.getRewriteCount(),
                s3ExpressTransform.getTotalCount()));

        return result;
    }
}
