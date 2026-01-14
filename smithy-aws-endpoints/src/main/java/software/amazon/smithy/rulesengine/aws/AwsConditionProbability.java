/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws;

import java.util.function.ToDoubleFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Uses prior knowledge of typical AWS endpoint resolution patterns to determine how probable a condition is of
 * returning true. This is used to aid in {@link software.amazon.smithy.rulesengine.logic.bdd.CostOptimization}.
 *
 * <p>Assumptions:
 * <ul>
 *   <li>Region is almost always set (0.95)</li>
 *   <li>Most isSet checks succeed (0.7)</li>
 *   <li>Boolean flags like UseFIPS, UseDualStack are usually false (0.1)</li>
 *   <li>ARN parsing rarely applies (0.2)</li>
 * </ul>
 */
public final class AwsConditionProbability implements ToDoubleFunction<Condition> {
    @Override
    public double applyAsDouble(Condition condition) {
        String s = condition.toString();

        // Region is almost always provided
        if (s.contains("isSet(Region)")) {
            return 0.96;
        }

        // Endpoint override is rare
        if (s.contains("isSet(Endpoint)")) {
            return 0.2;
        }

        // S3 Express is rare (includes ITE variables from S3TreeRewriter)
        if (s.contains("S3Express") || s.contains("--x-s3")
                || s.contains("--xa-s3")
                || s.contains("s3e_fips")
                || s.contains("s3e_ds")
                || s.contains("s3e_auth")) {
            return 0.001;
        }

        // Most isSet checks on optional params succeed moderately
        if (s.startsWith("isSet")) {
            return 0.5;
        }

        // Boolean feature flags are usually disabled
        if (s.contains("booleanEquals(UseFIPS, true)")
                || s.contains("booleanEquals(UseDualStack, true)")
                || s.contains("booleanEquals(Accelerate, true)")
                || s.contains("booleanEquals(UseGlobalEndpoint, true)")
                || s.contains("booleanEquals(ForcePathStyle, true)")) {
            return 0.05;
        }

        // ARN-based buckets are uncommon
        if (s.contains("parseArn") || s.contains("arn:")) {
            return 0.15;
        }

        // Default: 50/50
        return 0.5;
    }
}
