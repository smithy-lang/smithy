/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AwsArnTest {
    @Test
    void arnsEndingInColon_1() {
        // If the resource is empty, the resource will be a [""]
        String arn = "arn:aws:s3:us-east-2:012345678:";
        assertEquals(AwsArn.parse(arn), Optional.empty());
    }

    // This test is a description of current behavior more than it is a test of what the behavior _should_ be.
    @Test
    void arnsEndingInColon_2() {
        // if the resource is non-empty and ends in colon, the resource will be `["outpost"]`
        String arn = "arn:aws:s3:us-east-2:012345678:outpost:";
        AwsArn parsed = AwsArn.parse(arn).get();
        assertEquals(parsed.getResource().get(0), "outpost");
        assertEquals(parsed.getResource().get(1), "");
        assertEquals(parsed.getResource().size(), 2);
    }
}
