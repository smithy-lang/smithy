/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;

import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

public class ShapeTypeCategoryEnumSelectorTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(SelectorTest.class.getResource("shape-type-test.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void simpleTypeMatchesSimpleShapes() {
        Set<String> ids = exampleIds("simpleType");

        assertThat(ids,
                containsInAnyOrder("smithy.example#String",
                        "smithy.example#Integer",
                        "smithy.example#Enum",
                        "smithy.example#IntEnum"));
        assertThat("smithy.example#List", not(in(ids)));
        assertThat("smithy.example#Service", not(in(ids)));
    }

    @Test
    public void aggregateTypeMatchesAggregateShapes() {
        Set<String> ids = exampleIds("aggregateType");

        assertThat(ids,
                containsInAnyOrder(
                        "smithy.example#List",
                        "smithy.example#Map",
                        "smithy.example#Structure",
                        "smithy.example#Union",
                        "smithy.example#OperationInput",
                        "smithy.example#OperationOutput"));
        assertThat("smithy.example#String", not(in(ids)));
        assertThat("smithy.example#Service", not(in(ids)));
    }

    @Test
    public void serviceTypeMatchesServiceShapes() {
        Set<String> ids = exampleIds("serviceType");

        assertThat(ids,
                containsInAnyOrder(
                        "smithy.example#Service",
                        "smithy.example#Operation",
                        "smithy.example#Resource"));
    }

    @Test
    public void dataTypeMatchesAggregateAndSimpleShapes() {
        Set<String> ids = exampleIds("dataType");

        assertThat(ids,
                containsInAnyOrder(
                        "smithy.example#String",
                        "smithy.example#Integer",
                        "smithy.example#Enum",
                        "smithy.example#IntEnum",
                        "smithy.example#List",
                        "smithy.example#Map",
                        "smithy.example#Structure",
                        "smithy.example#OperationInput",
                        "smithy.example#OperationOutput",
                        "smithy.example#Union"));

        // Service shapes are excluded.
        assertThat("smithy.example#Service", not(in(ids)));
        assertThat("smithy.example#Operation", not(in(ids)));
        assertThat("smithy.example#Resource", not(in(ids)));

        // Member shapes are excluded
        assertThat("smithy.example#List$member", not(in(ids)));
    }

    private static Set<String> exampleIds(String typeSelector) {
        return SelectorTest.ids(model, typeSelector + " [id|namespace = smithy.example]");
    }
}
