/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;

import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

public class ShapeTypeSelectorTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(SelectorTest.class.getResource("shape-type-test.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void stringSelectsEnum() {
        Set<String> ids = SelectorTest.ids(model, "string");
        assertThat("smithy.example#String", in(ids));
        assertThat("smithy.example#Enum", in(ids));
    }

    @Test
    public void integerSelectsIntEnum() {
        Set<String> ids = SelectorTest.ids(model, "integer");
        assertThat("smithy.example#Integer", in(ids));
        assertThat("smithy.example#IntEnum", in(ids));
    }

    @Test
    public void hasContainsOptimization() {
        Set<String> ids = SelectorTest.ids(model, ":in(enum) [id|namespace = smithy.example]");

        assertThat("smithy.example#Enum", in(ids));
    }
}
