/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CloudWatchNamespaceValidatorTest {
    @ParameterizedTest
    @CsvSource({
            "Lambda, true",
            "S3, true",
            "DynamoDB2, true",
            "A, true",
            "lambda, false",
            "1Lambda, false",
            "Lambda-Foo, false",
            "Lambda_Foo, false",
            "Lambda Foo, false",
            "Lambda/Foo, false",
            "'', false"
    })
    public void detectsPascalCase(String namespace, boolean expected) {
        assertEquals(expected, CloudWatchNamespaceValidator.isServicePascalCase(namespace));
    }
}
