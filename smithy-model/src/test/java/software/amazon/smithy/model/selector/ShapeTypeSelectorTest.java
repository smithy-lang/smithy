/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.in;

import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

public class ShapeTypeSelectorTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model =  Model.assembler()
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
