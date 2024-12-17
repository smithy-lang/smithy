/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FunctionalUtilsTest {
    @Test
    public void negatesPredicate() {
        assertTrue(FunctionalUtils.not(test -> false).test(""));
    }
}
