/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.utils.ListUtils;

public class BddTraitTest {
    @Test
    void testBddTraitSerialization() {
        // Create a BddTrait with full context
        Parameters params = Parameters.builder().build();
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Rule endpoint = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE);
        results.add(endpoint);

        Bdd bdd = createSimpleBdd();

        BddTrait original = BddTrait.builder()
                .parameters(params)
                .conditions(ListUtils.of(cond))
                .results(results)
                .bdd(bdd)
                .build();

        // Serialize to Node
        Node node = original.toNode();
        assertTrue(node.isObjectNode());
        assertTrue(node.expectObjectNode().containsMember("parameters"));
        assertTrue(node.expectObjectNode().containsMember("conditions"));
        assertTrue(node.expectObjectNode().containsMember("results"));

        // Serialized should only have 1 result (the endpoint, not NoMatch)
        int serializedResultCount = node.expectObjectNode()
                .expectArrayMember("results")
                .getElements()
                .size();
        assertEquals(1, serializedResultCount);

        // Deserialize from Node
        BddTrait restored = BddTrait.fromNode(node);

        assertEquals(original.getParameters(), restored.getParameters());
        assertEquals(original.getConditions().size(), restored.getConditions().size());
        assertEquals(original.getResults().size(), restored.getResults().size());
        assertEquals(original.getBdd().getRootRef(), restored.getBdd().getRootRef());
        assertEquals(original.getBdd().getConditionCount(), restored.getBdd().getConditionCount());
        assertEquals(original.getBdd().getResultCount(), restored.getBdd().getResultCount());

        // Verify NoMatchRule was restored at index 0
        assertInstanceOf(NoMatchRule.class, restored.getResults().get(0));
    }

    private Bdd createSimpleBdd() {
        return new Bdd(2, 1, 2, 2, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, Bdd.RESULT_OFFSET + 1, -1); // node 1: if cond true, return result 1, else FALSE
        });
    }

    @Test
    void testEmptyBddTrait() {
        Parameters params = Parameters.builder().build();

        Bdd bdd = new Bdd(-1, 0, 1, 1, consumer -> {
            consumer.accept(-1, 1, -1); // terminal node only
        });

        BddTrait trait = BddTrait.builder()
                .parameters(params)
                .conditions(ListUtils.of())
                .results(ListUtils.of(NoMatchRule.INSTANCE))
                .bdd(bdd)
                .build();

        assertEquals(0, trait.getConditions().size());
        assertEquals(1, trait.getResults().size());
        assertEquals(-1, trait.getBdd().getRootRef()); // FALSE terminal
    }
}
