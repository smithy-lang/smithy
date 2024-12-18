/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;

public class NotSelectorTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(SelectorTest.class.getResource("not-test.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void simpleStringTest() {
        Set<String> ids = SelectorTest.ids(model, ":not(string)");

        assertThat(ids, hasItem("smithy.example#MyFloat"));
        assertThat(ids, not(hasItem("smithy.example#MyString")));
    }

    @Test
    public void notStringOrFloat() {
        Set<String> ids = SelectorTest.ids(model, "[id|namespace = 'smithy.example'] :not(string) :not(float)");

        assertThat(ids, not(hasItem("smithy.example#MyString")));
        assertThat(ids, not(hasItem("smithy.example#MyFloat")));
        assertThat(ids, hasItem("smithy.example#StringList"));
    }

    @Test
    public void allowsSingleSelector() {
        SelectorSyntaxException e = Assertions.assertThrows(
                SelectorSyntaxException.class,
                () -> Selector.parse(":not(string, float)"));

        assertThat(e.getMessage(), containsString("The :not function requires a single selector argument"));
    }

    @Test
    public void filtersOutResultsFromLeftToRight() {
        // Grab list shapes that don't target strings.
        Set<String> ids = SelectorTest.ids(model, "list :not(> member > string)");

        assertThat(ids, hasItem("smithy.example#FloatList"));
        assertThat(ids, not(hasItem("smithy.example#StringList")));
        assertThat(ids, not(hasItem("smithy.example#MyFloat")));
    }

    @Test
    public void filtersOutEvenMoreComplexResultsFromLeftToRight() {
        // match structure members that don't have a length trait and they
        // target string shapes that don't have a length trait.
        Set<String> ids = SelectorTest.ids(
                model,
                "structure > member"
                        + ":not([trait|length])"
                        + ":test(> string :not([trait|length]))");

        assertThat(ids, hasItem("smithy.example#StructA$noLengthOnEither"));
        assertThat(ids, not(hasItem("smithy.example#StructA")));
        assertThat(ids, not(hasItem("smithy.example#StructA$lengthOnTarget")));
        assertThat(ids, not(hasItem("smithy.example#StructA$lengthOnMember")));
    }

    @Test
    public void filtersServiceTraitRelationships() {
        // Match services that do not have a protocol definition trait.
        Set<String> ids = SelectorTest.ids(model, "service :not(-[trait]-> [trait|protocolDefinition])");

        assertThat(ids, hasItem("smithy.example#HasNoProtocolTraits"));
        assertThat(ids, not(hasItem("smithy.example#HasProtocolTraits")));
        assertThat(ids, not(hasItem("smithy.example#StructA"))); // randomly make sure non-services weren't matched
    }
}
