/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.rulesengine.language.functions;

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
