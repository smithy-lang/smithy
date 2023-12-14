/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Generates traits for tests.
 */
@GenerateTraits(
        packageName = "com.example.traits",
        header = {
                "Header line One",
                "Header line Two"
        },
        excludeTags = {
                "exclude"
        })
package software.amazon.smithy.traitcodegen.test;

import software.amazon.smithy.traitcodegen.processing.annotations.GenerateTraits;
