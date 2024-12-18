/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;

public class FunctionsTest {
    @Test
    public void awsPartitionOfExpression() {
        AwsPartition function = AwsPartition.ofExpressions(Expression.of("us-east-1"));
        assertThat(function, instanceOf(AwsPartition.class));
    }

    @Test
    public void isVirtualHostableS3BucketOfExpression() {
        IsVirtualHostableS3Bucket function = IsVirtualHostableS3Bucket.ofExpressions(
                Expression.of("foobar"),
                true);
        assertThat(function, instanceOf(IsVirtualHostableS3Bucket.class));

        IsVirtualHostableS3Bucket function2 = IsVirtualHostableS3Bucket.ofExpressions(
                Expression.of("foobar"),
                Expression.of(true));
        assertThat(function2, instanceOf(IsVirtualHostableS3Bucket.class));
    }

    @Test
    public void parseArnOfExpression() {
        ParseArn function = ParseArn.ofExpressions(Expression.of("arn:aws:s3:::bucket_name"));
        assertThat(function, instanceOf(ParseArn.class));
    }
}
