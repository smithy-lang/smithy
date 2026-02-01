/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.s3;

import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Split;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Substring;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.TreeMapper;

/**
 * Canonicalizes S3Express availability zone extraction.
 *
 * <p>Rewrites position-dependent substring operations to position-independent split operations:
 * <pre>{@code
 * substring(Bucket, N, M) → split(Bucket, "--")[1]
 * }</pre>
 *
 * <p>This enables BDD sharing across endpoints that extract the AZ from different
 * bucket name positions.
 */
final class S3AzCanonicalizerTransform extends TreeMapper {

    private static final Identifier ID_BUCKET = Identifier.of("Bucket");
    private static final Identifier ID_AZ_ID = Identifier.of("s3expressAvailabilityZoneId");

    private int rewriteCount = 0;

    private S3AzCanonicalizerTransform() {}

    /**
     * Creates a new transform instance.
     *
     * @return a new transform.
     */
    static S3AzCanonicalizerTransform create() {
        return new S3AzCanonicalizerTransform();
    }

    /**
     * Returns the number of AZ extractions that were canonicalized.
     *
     * @return rewrite count.
     */
    int getRewriteCount() {
        return rewriteCount;
    }

    @Override
    public Condition condition(Rule rule, Condition cond) {
        if (isAzIdSubstringBinding(cond)) {
            rewriteCount++;
            return createCanonicalAzCondition(cond);
        }
        return super.condition(rule, cond);
    }

    // Matches: s3expressAvailabilityZoneId = substring(Bucket, N, M)
    private static boolean isAzIdSubstringBinding(Condition cond) {
        if (!ID_AZ_ID.equals(cond.getResult().orElse(null))) {
            return false;
        }

        LibraryFunction fn = cond.getFunction();
        if (!(fn instanceof Substring) || fn.getArguments().isEmpty()) {
            return false;
        }

        Expression target = fn.getArguments().get(0);
        return target instanceof Reference && ID_BUCKET.equals(((Reference) target).getName());
    }

    // Creates: s3expressAvailabilityZoneId = split(Bucket, "--", 0)[-2]
    private static Condition createCanonicalAzCondition(Condition original) {
        Split split = Split.ofExpressions(
                Expression.getReference(ID_BUCKET),
                Expression.of("--"),
                Expression.of(0));
        GetAttr azExpr = GetAttr.ofExpressions(split, "[-2]");
        return original.toBuilder().fn(azExpr).build();
    }
}
