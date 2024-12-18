/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class TopDownSelectorTest {

    private static Model model1;
    private static Model model2;

    @BeforeAll
    public static void before() {
        model1 = Model.assembler()
                .addImport(SelectorTest.class.getResource("topdown-auth.smithy"))
                .assemble()
                .unwrap();

        model2 = Model.assembler()
                .addImport(SelectorTest.class.getResource("topdown-exclusive-traits.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void requiresAtLeastOneSelector() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(":topdown()"));
    }

    @Test
    public void doesNotAllowMoreThanTwoSelectors() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(":topdown(*, *, *)"));
    }

    @Test
    public void findsByAuthScheme() {
        Set<String> basic = SelectorTest.ids(
                model1,
                ":topdown([trait|auth|(values)='smithy.api#httpBasicAuth'],\n"
                        + "        [trait|auth]:not([trait|auth|(values)='smithy.api#httpBasicAuth']))");
        Set<String> digest = SelectorTest.ids(
                model1,
                ":topdown([trait|auth|(values)='smithy.api#httpDigestAuth'],\n"
                        + "       [trait|auth]:not([trait|auth|(values)='smithy.api#httpDigestAuth']))");

        assertThat(basic,
                containsInAnyOrder("smithy.example#RA",
                        "smithy.example#ServiceWithAuthTrait",
                        "smithy.example#OperationWithNoAuthTrait"));
        assertThat(digest,
                containsInAnyOrder("smithy.example#ServiceWithAuthTrait",
                        "smithy.example#OperationWithNoAuthTrait",
                        "smithy.example#RA",
                        "smithy.example#OperationWithAuthTrait"));
    }

    @Test
    public void findsExclusiveTraits() {
        Set<String> a = SelectorTest.ids(model2, ":topdown([trait|smithy.example#a], [trait|smithy.example#b])");
        Set<String> b = SelectorTest.ids(model2, ":topdown([trait|smithy.example#b], [trait|smithy.example#a])");

        assertThat(a, containsInAnyOrder("smithy.example#Service1", "smithy.example#R1", "smithy.example#O2"));
        assertThat(b,
                containsInAnyOrder("smithy.example#R2",
                        "smithy.example#O1",
                        "smithy.example#O3",
                        "smithy.example#O4"));
    }

    @Test
    public void topDownWithNoDisqualifiers() {
        Set<String> a = SelectorTest.ids(model2, ":topdown([trait|smithy.example#a])");

        assertThat(a,
                containsInAnyOrder("smithy.example#Service1",
                        "smithy.example#R1",
                        "smithy.example#O1",
                        "smithy.example#O2",
                        "smithy.example#R2",
                        "smithy.example#O3",
                        "smithy.example#O4"));
    }

    @Test
    public void topDownWithNoDisqualifiersWithServiceVariableFollowedByFilter() {
        Map<ShapeId, ShapeId> matches = new HashMap<>();
        Selector.parse("service $service(*) :topdown([trait|smithy.example#a]) resource")
                .consumeMatches(model2, match -> {
                    matches.put(match.getShape().getId(), match.get("service").iterator().next().getId());
                });

        assertThat(matches, hasKey(ShapeId.from("smithy.example#R1")));
        assertThat(matches.get(ShapeId.from("smithy.example#R1")), equalTo(ShapeId.from("smithy.example#Service1")));
        assertThat(matches, hasKey(ShapeId.from("smithy.example#R2")));
        assertThat(matches.get(ShapeId.from("smithy.example#R2")), equalTo(ShapeId.from("smithy.example#Service1")));
    }

    @Test
    public void doesNotOverflowOnBrokenResourceCycles() {
        Model recursiveModel = Model.assembler()
                .addImport(getClass().getResource("recursive-resources.smithy"))
                .assemble()
                .getResult()
                .get(); // we know it's invalid.

        // The result isn't really important here. We just don't want it to
        // cause a stack overflow.
        Selector.parse(":topdown(*)").select(recursiveModel);
    }
}
