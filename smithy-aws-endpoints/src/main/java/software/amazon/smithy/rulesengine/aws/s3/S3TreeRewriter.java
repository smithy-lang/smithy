/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.s3;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Rewrites S3 endpoint rules to create a dramatically smaller and more efficient BDD.
 *
 * <p>This is a BDD pre-processing transform that makes the decision tree larger but enables dramatically reduced
 * BDD artifact size. It solves the "SSA Trap" problem where semantically identical operations appear as syntactically
 * different expressions.
 *
 * <p>This class composes three separate transforms, each selectable via {@link Transform}:
 * <ol>
 *   <li>{@link Transform#AZ_CANONICALIZATION} - Canonicalizes AZ extraction:
 *       {@code substring(Bucket, N, M)} → {@code split(Bucket, "--")[1]}</li>
 *   <li>{@link Transform#REGION_UNIFICATION} - Unifies region references:
 *       {@code Region}/{@code bucketArn#region} → {@code _signing_region}/{@code _effective_region}</li>
 *   <li>{@link Transform#S3EXPRESS_ENDPOINTS} - Canonicalizes S3Express endpoints:
 *       FIPS/DualStack URL variants → ITE-computed segments</li>
 * </ol>
 *
 * <p>By default all transforms are applied. Use {@link #transform(EndpointRuleSet, Transform, Transform...)}
 * to select a subset.
 *
 * <p>This transform isn't stable. While it reduces BDD node count and enables much better sharing and reduces the
 * number of unique results, it comes at the expense of runtime performance in many cases due to adding more complex
 * expressions than before. It's possible we may entirely rewrite this to do different transforms, abandon it entirely,
 * or move to a different kind of optimization altogether. In short, don't rely on this class.
 */
@SmithyInternalApi
@SmithyUnstableApi
public final class S3TreeRewriter {
    private static final Logger LOGGER = Logger.getLogger(S3TreeRewriter.class.getName());

    /**
     * Individual transforms that can be enabled or disabled.
     */
    public enum Transform {
        /** Canonicalizes AZ extraction from substring to split. */
        AZ_CANONICALIZATION,

        /** Unifies region references into computed variables. */
        REGION_UNIFICATION,

        /** Canonicalizes S3Express endpoint URL and auth patterns. */
        S3EXPRESS_ENDPOINTS
    }

    private S3TreeRewriter() {}

    /**
     * Transforms the given endpoint rule set by applying all S3 canonicalization transforms.
     *
     * @param ruleSet Rules to transform.
     * @return the transformed rule set.
     */
    public static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        return transform(ruleSet, EnumSet.allOf(Transform.class));
    }

    /**
     * Transforms the given endpoint rule set by applying only the specified transforms.
     *
     * @param ruleSet Rules to transform.
     * @param first the first transform to apply.
     * @param rest additional transforms to apply.
     * @return the transformed rule set.
     */
    public static EndpointRuleSet transform(EndpointRuleSet ruleSet, Transform first, Transform... rest) {
        return transform(ruleSet, EnumSet.of(first, rest));
    }

    private static EndpointRuleSet transform(EndpointRuleSet ruleSet, Set<Transform> enabled) {
        EndpointRuleSet result = ruleSet;
        int azCount = 0;
        int regionCount = 0;
        int s3eCount = 0;
        int s3eTotal = 0;

        if (enabled.contains(Transform.AZ_CANONICALIZATION)) {
            S3AzCanonicalizerTransform azTransform = S3AzCanonicalizerTransform.create();
            result = azTransform.endpointRuleSet(result);
            azCount = azTransform.getRewriteCount();
        }

        if (enabled.contains(Transform.REGION_UNIFICATION)) {
            S3RegionUnifierTransform regionTransform = S3RegionUnifierTransform.create();
            result = regionTransform.endpointRuleSet(result);
            regionCount = regionTransform.getRewriteCount();
        }

        if (enabled.contains(Transform.S3EXPRESS_ENDPOINTS)) {
            S3ExpressEndpointTransform s3ExpressTransform = S3ExpressEndpointTransform.create();
            result = s3ExpressTransform.endpointRuleSet(result);
            s3eCount = s3ExpressTransform.getRewriteCount();
            s3eTotal = s3ExpressTransform.getTotalCount();
        }

        int finalAz = azCount;
        int finalRegion = regionCount;
        int finalS3e = s3eCount;
        int finalS3eTotal = s3eTotal;
        LOGGER.info(() -> String.format(
                "S3 tree rewriter (enabled: %s): %d AZ, %d region, %d/%d S3Express rewrites",
                enabled,
                finalAz,
                finalRegion,
                finalS3e,
                finalS3eTotal));

        return result;
    }
}
