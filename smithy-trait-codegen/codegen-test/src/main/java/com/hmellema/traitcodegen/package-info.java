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
package com.hmellema.traitcodegen;

import software.amazon.smithy.traitcodegen.processing.annotations.GenerateTraits;
